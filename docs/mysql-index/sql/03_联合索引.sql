-- =====================================================================
-- 03_联合索引.sql
-- MySQL 索引实战 · 第三阶段：联合索引（复合索引）
--
-- 业务场景：“我的订单列表”接口
--     SELECT id, order_no, user_id, status, pay_amount, create_time
--     FROM mall_order
--     WHERE user_id = ? AND status = ?
--     ORDER BY create_time DESC
--     LIMIT 20;
-- 本阶段按下面顺序，一步一步升级索引，亲眼观察每一步的变化：
--     (user_id)  →  (user_id, status)  →  (user_id, status, create_time)
-- =====================================================================

-- ---------- 0. 幂等重置（同前：先清空本教程二级索引） ----------
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

-- ① 先认识教学主角：一个订单量够大的用户（本教程用 29997 号，他有 53 单）
SELECT user_id, status, COUNT(*) AS cnt
FROM mall_order
WHERE user_id = 29997
GROUP BY user_id, status
ORDER BY status;

-- ② 定义一个“业务查询”（下面每一小步都用同一句）
--    SELECT id, order_no, user_id, status, pay_amount, create_time
--    FROM mall_order
--    WHERE user_id = 29997 AND status = 1        -- 查他“已支付”的订单
--    ORDER BY create_time DESC
--    LIMIT 20;

-- =====================================================================
-- 第 0 步：什么都不建 —— 全表扫 100 万行
-- =====================================================================
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;
-- type = ALL, rows ≈ 996250, Extra = Using where; Using filesort
-- 100 万行里找出 15 行，却要一行行全看一遍，还要额外做一次排序
-- 实测耗时：约 190~240ms

-- =====================================================================
-- 第 1 步：建单列索引 (user_id)
-- =====================================================================
CREATE INDEX idx_user_id ON mall_order (user_id);

-- 查询只带 user_id 时：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, rows = 53, Extra = Using filesort
-- ✅ 定位到他的 53 行，快了；但 ORDER BY create_time 还是得临时排序

-- 查询带 user_id + status 时，单列索引只帮了一半：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, rows = 53(还要再按 status 过滤), filtered = 10.00, Extra = Using where; Using filesort

-- =====================================================================
-- 第 2 步：升级成联合索引 (user_id, status)
-- =====================================================================
DROP INDEX idx_user_id ON mall_order;
CREATE INDEX idx_user_status ON mall_order (user_id, status);

EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, rows = 15, ref = const,const, key_len = 9 (8+1)
-- ✅ 两个等值条件都吃进索引了：53 → 15 行
-- ⚠️ Extra 还是 Using filesort —— create_time 不在索引里，排序仍要自己做

-- =====================================================================
-- 第 3 步：再升级成 (user_id, status, create_time)
-- =====================================================================
DROP INDEX idx_user_status ON mall_order;
CREATE INDEX idx_user_status_ct ON mall_order (user_id, status, create_time);

EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, rows = 15
-- Extra = Backward index scan  ← 重点！filesort 消失了
-- create_time 现在是索引第三列，而前两列都是“等值”条件，
-- 索引内部顺序 = user_id,status 相同段内按 create_time 升序，
-- 倒序读索引 = 天然得到 ORDER BY create_time DESC，排序交给索引完成。
-- 实测耗时：约 20~30ms（服务器端 1ms 级）

-- =====================================================================
-- 第 4 步：亲手验证“最左前缀”
-- =====================================================================
-- 4.1 中间列没给等值时，第三列的“排序能力”也用不上：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997                     -- 只有第一列
ORDER BY create_time DESC
LIMIT 20;
-- type = ref, rows = 53, Extra = Using filesort
-- 因为 status 这一“中间列”没作为等值条件，索引第三列 create_time 无法保证有序

-- 4.2 跳过第一列 user_id，只按 status + 时间区间查：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 20;
-- type = ALL, rows ≈ 996250, Extra = Using where; Using filesort
-- 联合索引最左列是 user_id，没带 user_id = 索引完全用不上 → 全表扫

-- 【最左前缀原则（第三阶段核心结论）】
--   联合索引 (a, b, c) 能生效的前缀组合：
--     a ✅ | a,b ✅ | a,b,c ✅
--     b ❌ | a,c ⚠️(只用 a，c 的顺序能力用不上) | b,c ❌
--   等值条件放前面，范围/排序条件放后面，是最常用的设计顺序。

-- 一句话记忆锚点：
--   联合索引 = 排好序的三列电话号码本；只有“从左往右连续取前缀”才翻得下去。
