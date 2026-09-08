-- =====================================================================
-- 05_索引失效.sql
-- MySQL 索引实战 · 第五阶段：亲手制造并修复“索引失效”
--
-- 五个真实场景，每个都按同一套路练习：
--   错误 SQL → EXPLAIN → 分析为什么不走索引 → 修改 SQL / 索引 → 再 EXPLAIN → 对比
--   ① 对索引列使用函数
--   ② 隐式类型转换
--   ③ LIKE '%xxx' 前导通配
--   ④ 联合索引不满足最左前缀
--   ⑤ OR 条件
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
-- 场景 ① 对索引列使用函数
-- =====================================================================
-- 先在 create_time 上建好索引（生产里这种索引很常见）
CREATE INDEX idx_ct ON mall_order (create_time);

-- ✗ 错误 SQL：把函数套在索引列上（DATE(create_time)）
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE DATE(create_time) = '2024-06-15'
LIMIT 20;
-- type = ALL, rows ≈ 996250, Extra = Using where
-- 原因：索引里存的是 create_time 原值，不是 DATE() 算出来的值。
--       对列做函数 = 把每一行的值先算一遍才能比较 → 索引顺序失效，只能全表扫。

-- ✓ 修改 SQL：把函数从列上挪走，改成“范围条件”（不改变业务语义）
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE create_time >= '2024-06-15 00:00:00'
  AND create_time <  '2024-06-16 00:00:00'
LIMIT 20;
-- type = range, key = idx_ct, rows ≈ 1381, Extra = Using index condition
-- 原样对比：ALL(99.6万行) → range(1381行)，索引回来了

-- 临时清理，避免影响后面场景
DROP INDEX idx_ct ON mall_order;

-- =====================================================================
-- 场景 ② 隐式类型转换
-- =====================================================================
-- mall_user.phone 存的是【字符串】（VARCHAR），先保证它有索引
CREATE INDEX idx_user_phone ON mall_user (phone);

-- ✗ 错误 SQL：拿数字跟字符串列比较（Java 里常见：phone 参数被解析成 long）
EXPLAIN SELECT * FROM mall_user WHERE phone = 13713529579;
-- possible_keys 里有 idx_user_phone，但 key = NULL、type = ALL、rows ≈ 299124
-- 原因：MySQL 遇到“字符串列 = 数字”时，会把【列本身】转成数字再比较
--       （对索引列做隐式类型转换 = 偷偷给列套了个 CAST()，同场景①）

-- ✓ 修改 SQL：传入字符串 '13713529579'
EXPLAIN SELECT * FROM mall_user WHERE phone = '13713529579';
-- type = ref, key = idx_user_phone, rows = 1
-- 应用层经验：字段是 VARCHAR 就传字符串（MyBatis 里注意参数类型映射）

-- =====================================================================
-- 场景 ③ LIKE 前导通配 '%xxx'
-- =====================================================================
-- 沿用上面的 idx_user_phone；业务场景：按手机号号段搜用户
-- ✓ 前缀匹配：'1371%' 走索引（相当于范围 [1371, 1372)）
EXPLAIN SELECT id, username, phone FROM mall_user WHERE phone LIKE '1371%';
-- type = range, key = idx_user_phone, rows = 324, Extra = Using index condition

-- ✗ 前导通配：'%1371%' 无法确定起始位置，只能全表扫
EXPLAIN SELECT id, username, phone FROM mall_user WHERE phone LIKE '%1371%';
-- type = ALL, rows ≈ 299124
-- 原因：B+树按“前缀”排序，‘%xxx’ 的开头是通配符，树不知道该从哪个分支往下走
-- 修复思路：能改成前缀就改；业务真需要“包含查询”时，考虑 ES / 反向索引 / 分词

-- =====================================================================
-- 场景 ④ 联合索引不满足最左前缀
-- =====================================================================
-- 假设线上只有“我的订单”用的联合索引 (user_id, status, create_time)
CREATE INDEX idx_user_status_ct ON mall_order (user_id, status, create_time);

-- ✗ 错误 SQL：运营后台按“状态 + 时间段”查（没带 user_id）
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 20;
-- type = ALL, rows ≈ 996250, Extra = Using where; Using filesort
-- 原因：联合索引最左列是 user_id，条件里没有它 → 整个索引用不上（最左前缀）

-- ✓ 修改方式一（改 SQL）：如果业务必须按该用户筛，就补上 user_id
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997
  AND status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 20;

-- ✓ 修改方式二（改索引）：如果“按状态+时间段”是另一种真实高频查询，
--   就为它单独建 (status, create_time)，而不是硬套旧索引
CREATE INDEX idx_order_status_ct ON mall_order (status, create_time);

EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 20;
-- type = range, key = idx_order_status_ct, rows ≈ 39158,
-- Extra = Using index condition; Backward index scan（连排序都省了）

-- =====================================================================
-- 场景 ⑤ OR 条件
-- =====================================================================
-- mall_user 上现有：uk_username(username)、idx_user_phone(phone)；email 还没有索引
-- ✗ 错误 SQL：username 有索引，但 email 没有 → OR 要让“两边都满足才走索引”，
--             有一边走不了索引，整个查询退化全表扫
EXPLAIN SELECT id, username, phone, email
FROM mall_user
WHERE username = 'user_1' OR email = 'user2@example.com';
-- type = ALL, rows ≈ 299124（possible_keys 只有 uk_username，但用不上）

-- ✓ 修改方式一（补索引）：两边都有索引 → MySQL 可以做 index_merge(union)
CREATE INDEX idx_user_email ON mall_user (email);
EXPLAIN SELECT id, username, phone, email
FROM mall_user
WHERE username = 'user_1' OR email = 'user2@example.com';
-- type = index_merge, Extra = Using union(uk_username, idx_user_email)
-- 两个索引都被用上，各查 1 行再合并

-- ✓ 修改方式二（推荐，SQL 改造）：用 UNION ALL 拆成两条各自走索引
EXPLAIN
SELECT id, username, phone, email FROM mall_user WHERE username = 'user_1'
UNION ALL
SELECT id, username, phone, email FROM mall_user WHERE email = 'user2@example.com';
-- 两段分别 type=const / ref，rows=1

-- =====================================================================
-- ⑥ 结尾：收掉演示用的临时索引，模拟“生产只留业务真实需要的索引”
-- =====================================================================
DROP INDEX idx_user_email ON mall_user;        -- 场景⑤演示用
DROP INDEX idx_order_status_ct ON mall_order;  -- 场景④演示用
-- 此刻 mall_user 保留 idx_user_phone；mall_order 保留 idx_user_status_ct（“我的订单”专用）

-- 一句话记忆锚点：
--   索引喜欢“原汁原味的列 + 可计算的前缀”；
--   函数、类型转换、%开头、断最左、OR乱拼，都会让索引白建。
