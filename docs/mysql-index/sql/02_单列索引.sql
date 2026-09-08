-- =====================================================================
-- 02_单列索引.sql
-- MySQL 索引实战 · 第二阶段：单列索引
--
-- 【本阶段 4 个核心问题】
--   1. 什么字段适合建索引？（用“区分度”回答）
--   2. CREATE INDEX 怎么写？（+ 主键索引 / 普通二级索引 / 唯一索引 区别）
--   3. 索引为什么能提高查询速度？
--   4. 索引为什么不是越多越好？
--
-- 前置：已执行 00_建表与造数.sql；可先执行 01_第一次使用索引.sql 体验过一次建索引
-- =====================================================================

-- ---------- 0. 幂等重置：清掉本教程所有二级索引（不删数据） ----------
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
-- ① 什么字段适合建索引：先看“区分度”（cardinality）
-- =====================================================================
-- 先创建两个对比索引：一个建在“区分度极高”的 phone 上，
-- 一个建在“区分度极低”的 status 上（status 只有 5 种取值）。
CREATE INDEX idx_user_phone ON mall_user (phone);
CREATE INDEX idx_order_status ON mall_order (status);

-- 对比 1：高区分度 —— 手机号查用户，30 万里几乎不重复
EXPLAIN SELECT * FROM mall_user WHERE phone = '<你的号码，如 13713529579>';
-- rows ≈ 1：索引把一个“30 万选 1”的问题变成了“定位 1 行”

-- 对比 2：低区分度 —— status=1（已支付）占整表 50%（约 50 万行）
EXPLAIN SELECT * FROM mall_order WHERE status = 1;
-- 结果依然是 ref，但 rows ≈ 498125：走索引也要拿 49.8 万行，
-- 如果还要 SELECT *，每一行都要回表（随机IO）——并不比全表扫划算。

-- 对比 3：低区分度但比例小些 —— status=0（待支付）占 10%（约 10 万行）
EXPLAIN SELECT * FROM mall_order WHERE status = 0;
-- rows ≈ 203490（统计值），依然要拿 20 万行

-- 【结论】字段值种类越少（如 status/sex/flag），单列索引越没用；
-- 字段值越接近一一对应（如 phone/order_no/id），索引越高效。
-- 这也是面试常问：为什么不对“性别”建索引？——区分度太低。

-- =====================================================================
-- ② CREATE INDEX 的三种常见形态（本工程都能看到）
-- =====================================================================
-- 形态 A：主键索引（建表时自动生成，InnoDB 的数据就按主键组织）
EXPLAIN SELECT * FROM mall_user WHERE id = 137;
-- type = const：按主键定位，理论最快（“常数命中”）
EXPLAIN SELECT * FROM mall_user WHERE id BETWEEN 100 AND 200;
-- type = range：主键范围扫描，一次定位到起点，向后顺序取

-- 形态 B：业务唯一索引（建表时 UNIQUE KEY 自动生成）
EXPLAIN SELECT * FROM mall_user WHERE username = 'user_123';
-- type = const，key = uk_username：唯一索引等值命中 = 最多 1 行

-- 形态 C：普通二级索引（我们刚建的 idx_user_phone 就是）
EXPLAIN SELECT * FROM mall_user WHERE phone = '<你的号码>';
-- type = ref：二级索引等值命中，可能多行（手机号不唯一时）

-- 三种索引的关系（一句话）：
--   主键索引 = 表的“目录即正文”（聚簇）；二级索引 = 按别的字段另排的目录，
--   目录里只存“索引字段 + 主键”，找到主键后还要回表翻正文。

-- =====================================================================
-- ③ 索引为什么快？（20 秒直觉版）
-- =====================================================================
-- 全表扫：从第一行到最后一行，30 万次“翻开看”
-- 用索引：B+树自顶向下 3~4 层二分定位，落到一页（约 1.6 万行/页 的量级），
--         然后顺着页内有序链表取出目标行
-- 代价是：建索引 = 把字段复制一份，按排序规则组织成一棵树（占空间、写时要维护）

-- =====================================================================
-- ④ 为什么索引不是越多越好？（用数字说话）
-- =====================================================================
-- 4.1 先看磁盘占用（100 万行的订单表）
-- 说明：下面的 SQL 是查看表数据/索引各占多大，不是建索引
SELECT
    table_name,
    ROUND(data_length  / 1024 / 1024, 1) AS 数据_MB,
    ROUND(index_length / 1024 / 1024, 1) AS 索引_MB
FROM information_schema.tables
WHERE table_schema = 'sql_learning_platform'
  AND table_name IN ('mall_order', 'mall_user', 'mall_product');

-- 本教程实测（真实机器上的结果）：
--   订单表行数据 62.6MB，只带主键+订单号唯一索引时 索引=26.6MB；
--   再加 2 个联合索引后 索引=78.7MB  —— 每加一个联合索引约多占 26MB

-- 4.2 加回去一个试试（现场做一次：建索引 → 看体积 → 删掉）
CREATE INDEX idx_user_id ON mall_order (user_id);
-- 观察：SHOW INDEX 列表变长；information_schema 的 index_length 变大
DROP INDEX idx_user_id ON mall_order;

-- 4.3 索引的隐性成本（为什么不是越多越好）：
--   · 空间：上面实测，每个索引都要为整张表的该字段另存一份
--   · 写入变慢：每次 INSERT/UPDATE/DELETE，都要同步维护【每一个】索引的
--     B+ 树（这叫“写放大”）；写多读少的表，索引越多越吃亏
--   · 维护成本：删索引/加索引都要小心锁表窗口（大表 DDL 另说）
-- 经验值：单表二级索引 5 个以内常见；先满足真实查询，再谈覆盖/优化

-- 一句话记忆锚点：
--   给“高区分度 + 高频 where 条件”的字段建单列索引；
--   索引是“空间换时间”，建一个多一份维护成本，宁缺毋滥。
