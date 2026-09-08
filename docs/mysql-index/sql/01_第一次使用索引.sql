-- =====================================================================
-- 01_第一次使用索引.sql
-- MySQL 索引实战 · 第一阶段：第一次使用索引
--
-- 【本阶段你要亲手做的 6 件事】
--   1. 先取一个真实存在的手机号（每人造数不同）
--   2. 无索引查一次：SELECT  ——  看耗时
--   3. 无索引 EXPLAIN    ——  看 type=ALL、rows≈30万
--   4. CREATE INDEX 建索引
--   5. 同样 SQL 再查一次 ——  看耗时变化
--   6. 再 EXPLAIN       ——  看 type=ref、rows=1
--
-- 【怎么运行】
--   mysql -h127.0.0.1 -uroot -proot1234 sql_learning_platform < 01_第一次使用索引.sql
--   也可以把下面语句一段段复制到 Navicat / DataGrip 里执行（推荐，看得更清楚）
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
-- ① 取一个真实存在的手机号（数据库里每人的号码是随机造的，先取一个）
-- =====================================================================
SELECT id, username, phone
FROM mall_user
ORDER BY id
LIMIT 5;

-- 把上面结果里的任意一个 phone 值记下来，例如教程用的是：13713529579
-- 下文用 <你的号码> 占位，请替换成你查到的那一个。

-- =====================================================================
-- ② 第 1 次查询（此时 mall_user 上【没有】phone 索引）
-- =====================================================================

-- 业务场景：客服/运营在后台“按手机号精确查用户”
SELECT * FROM mall_user WHERE phone = '<你的号码>';

-- 查看执行计划：看懂 MySQL 到底怎么找这行数据
EXPLAIN SELECT * FROM mall_user WHERE phone = '<你的号码>';
-- 你会看到：
--   type = ALL           全表扫描（一行一行扫 30 万行）
--   key  = NULL          没有可用索引
--   rows = 299124        预估扫描 29.9 万行
--   Extra= Using where   先全表扫，再用 where 过滤
-- 耗时参考：约 60~80 ms（前几次执行，页没缓存会更慢）

-- =====================================================================
-- ③ 创建索引（本阶段最关键的一步）
-- =====================================================================
CREATE INDEX idx_user_phone ON mall_user (phone);

-- 这句 SQL 逐词解释：
--   CREATE INDEX           创建索引（关键字）
--   idx_user_phone         索引名（自己起，规范：idx_表名_字段名）
--   ON mall_user(phone)    给哪张表的哪个字段建
-- 为什么建在 phone 上？因为业务高频查询是“WHERE phone = ?”，
-- 而 phone 在 30 万用户里几乎每行都不同（区分度极高），索引效率最高。

-- 创建完成后可以确认一下索引真的建出来了：
SHOW INDEX FROM mall_user;

-- =====================================================================
-- ④ 第 2 次查询：同样的 SQL 原样再执行一次
-- =====================================================================
SELECT * FROM mall_user WHERE phone = '<你的号码>';

EXPLAIN SELECT * FROM mall_user WHERE phone = '<你的号码>';
-- 对比变化：
--   type = ref           通过索引精确定位（ref = 按索引等值查找）
--   key  = idx_user_phone 真正用上了刚建的索引
--   key_len = 83         用到的索引前缀字节数（phone 是 VARCHAR(20) utf8mb4：20*4+3）
--   ref  = const         等值条件来自常量
--   rows = 1             预估只扫 1 行（原来 29.9 万行）
--   Extra= NULL          不再需要额外过滤
-- 耗时参考：约 15~25 ms（其中大部分是“连接 MySQL”的固定开销，服务器实际 1ms 内）

-- =====================================================================
-- ⑤ 前后对比（把结果填进这张表，你就真正看懂了）
-- =====================================================================
-- ┌──────────┬───────────────┬──────────────────┬───────────────┬──────────────────────┐
-- │ 对比项     │ 建索引前(ALL)   │ 建索引后(ref)      │ 结论           │                      │
-- ├──────────┼───────────────┼──────────────────┼───────────────┼──────────────────────┤
-- │ type     │ ALL           │ ref              │ 全表扫 → 索引定位  │                      │
-- │ key      │ NULL          │ idx_user_phone   │ 从“没索引”到“用索引” │                      │
-- │ rows     │ 299124        │ 1                │ 扫描量 30万 → 1   │                      │
-- │ Extra    │ Using where   │ NULL             │ 过滤工作交给了索引  │                      │
-- │ 耗时      │ ~70ms         │ ~20ms(含连接开销)   │ 服务器端差一个数量级 │                      │
-- └──────────┴───────────────┴──────────────────┴───────────────┴──────────────────────┘

-- 一句话记忆锚点：EXPLAIN 里 type 从 ALL 变成 ref、rows 从 29 万变成 1，
-- 就是“索引生效”最直接的证据。
