-- =====================================================================
-- 06_慢SQL排查实战.sql
-- MySQL 索引实战 · 第六阶段：模拟线上事故“订单查询接口突然变慢”
--
-- 事故回放（企业级流程）：
--   周一 10:05 运营反馈：“后台导出 6 月已支付订单，页面一直转圈”
--   你按下面的 7 步排查：
--     ① 复现并拿到慢 SQL
--     ② EXPLAIN 看执行计划
--     ③ 定位根因（索引与 SQL 形态不匹配）
--     ④ 设计修复（新建/调整索引，或改 SQL）
--     ⑤ 再 EXPLAIN 验证
--     ⑥ 对比优化效果（耗时 / Rows_examined）
--     ⑦ 回归：确认“我的订单”等原有接口没被影响
-- =====================================================================

-- ---------- 0. 幂等重置（同前） ----------
DROP PROCEDURE IF EXISTS __idx_reset;
DELIMITER $$
CREATE PROCEDURE __idx_reset()
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLWARNING, SQLEXCEPTION BEGIN END;
    DROP INDEX idx_user_phone   ON mall_user;
    DROP INDEX idx_user_email   ON mall_user;
    DROP INDEX idx_cat_status_price       ON mall_product;
    DROP INDEX idx_cat_status_price_stock ON mall_product;
    DROP INDEX idx_user_id       ON mall_order;
    DROP INDEX idx_user_status   ON mall_order;
    DROP INDEX idx_user_status_ct ON mall_order;
    DROP INDEX idx_order_status  ON mall_order;
    DROP INDEX idx_ct            ON mall_order;
    DROP INDEX idx_order_status_ct ON mall_order;
END$$
DELIMITER ;
CALL __idx_reset();
DROP PROCEDURE __idx_reset;

-- =====================================================================
-- ① 事故现场还原
-- =====================================================================
-- 背景：订单表 100 万行。开发时为“我的订单”接口建过联合索引：
CREATE INDEX idx_user_status_ct ON mall_order (user_id, status, create_time);
-- （这正是“上一阶段刚学会的”典型索引：等值 user_id+status + 排序 create_time）

-- 运营报的慢接口 SQL 长这样（按状态 + 时间范围，没有 user_id）：
SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;

-- ② 先 EXPLAIN，别急着猜：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;
-- 观察三行关键信息：
--   type  = ALL           ← 全表扫描
--   rows  ≈ 996250        ← 扫了 100 万行
--   Extra = Using where; Using filesort   ← 扫完还要整体排序
-- 本机实测：接口耗时 ~190~270ms，慢日志里 Rows_examined = 1000050

-- 附：如何让 MySQL 帮你把慢 SQL 记下来（生产排查标准动作）
--   SET GLOBAL slow_query_log = ON;                 -- 打开慢查询日志
--   SET GLOBAL long_query_time  = 0.5;              -- 超过 0.5 秒的记下来（开发环境可设 0.1）
--   SHOW VARIABLES LIKE 'slow_query_log_file';      -- 看日志文件路径
--   出事后第一件事：mysql -uroot -p -e "SELECT * FROM mysql.slow_log" 或看文件
--   （用完记得 SET GLOBAL slow_query_log = OFF，别一直开着）

-- =====================================================================
-- ③ 定位根因
-- =====================================================================
-- 慢 SQL 的形态是 (status, create_time)，而线上索引是 (user_id, status, create_time)。
-- 按“最左前缀”，没带 user_id → 这个联合索引【整个用不上】。
-- 结论：不是数据量的问题，是“SQL 形态 vs 索引列序”不匹配（最典型的线上索引问题）。
-- 顺带验证：老接口“我的订单”依旧秒回（user_id 在最左列，索引正常）：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, key = idx_user_status_ct —— 老接口没坏，是“新查询形态缺索引”

-- =====================================================================
-- ④ 设计修复：为 (status, create_time) 这个真实高频形态单独建索引
-- =====================================================================
CREATE INDEX idx_order_status_ct ON mall_order (status, create_time);

-- ⑤ 再 EXPLAIN（同一句慢 SQL）：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;
-- type = range, key = idx_order_status_ct, rows ≈ 39158,
-- Extra = Using index condition; Backward index scan（排序也让索引干了）

-- =====================================================================
-- ⑥ 验证优化效果（真实机器数据）
-- =====================================================================
-- 修复前 EXPLAIN： type=ALL rows≈996250 extra=Using where; Using filesort
--               客户端 5 次耗时：273/240/236/240/242 ms
--               慢日志：Query_time 0.233  Rows_examined 1000050
-- 修复后 EXPLAIN： type=range rows≈39158 extra=Using index condition; Backward index scan
--               客户端 5 次耗时：27/23/20/17/17 ms（含连接开销，服务器端约 7ms）
--               慢日志：Query_time 0.007  Rows_examined 50
-- 结论：扫描行数 100万 → 50 行、服务器耗时 233ms → 7ms（约 30 倍）

-- 再把上面同一句慢 SQL 手动跑几次，感受耗时变化（多跑几次看稳定值）：
SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;

-- =====================================================================
-- ⑦ 回归：确认线上其它接口没被影响
-- =====================================================================
SHOW INDEX FROM mall_order;   -- 应看到：PRIMARY、uk_order_no、
                              --       idx_user_status_ct（我的订单）、idx_order_status_ct（本次新增）
-- “我的订单”仍走自己的索引：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;

-- 大功告成。总结这套排查心法（面试也能这么答）：
--   拿到慢SQL → EXPLAIN → 盯 type/key/rows/Extra 四列 → 判断是“没索引”
--   还是“索引没对上SQL形态” → 用最左前缀反推正确列序 → 建/改索引 → 再EXPLAIN
--   → 对比 rows / 耗时 / 慢日志 Rows_examined → 回归其它查询。
