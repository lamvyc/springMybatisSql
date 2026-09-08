-- =====================================================================
-- 04_回表与覆盖索引.sql
-- MySQL 索引实战 · 第四阶段：回表（Table Lookup）与覆盖索引（Covering Index）
--
-- 业务场景：商品列表接口
--     SELECT ... FROM mall_product
--     WHERE category_id = ? AND status = 1
--     ORDER BY price DESC
--     LIMIT 20;
-- 本阶段回答：
--   1. 二级索引里到底存了什么？为什么“查不全”就要回表？
--   2. 怎么让查询“只扫索引、不碰表”（覆盖索引）？
--   3. 为什么 SELECT * 会带来更多数据访问？
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

-- ① 认知前提：InnoDB 二级索引里存了什么？
--    二级索引 = “字段值的排序目录”，每一页存的是：
--        (索引字段..., 主键 id)
--    它【不】存表的其他列！所以只靠二级索引查不到 product_name、create_time
--    等“索引外”的列，必须拿着主键 id 再回主键索引（聚簇索引）里取整行 —— 这就是回表。

-- =====================================================================
-- 第 1 步：无索引时，商品列表 = 全表扫 + 自己排序
-- =====================================================================
-- 先确认本教程用的分类有多少商品（分类37：上架 847 个）
SELECT COUNT(*) AS cnt
FROM mall_product
WHERE category_id = 37 AND status = 1;

-- 商品列表 SQL（先写 SELECT *：只图省事，后面会付出代价）
EXPLAIN SELECT *
FROM mall_product
WHERE category_id = 37 AND status = 1
ORDER BY price DESC
LIMIT 20;
-- type = ALL, rows ≈ 199455, Extra = Using where; Using filesort

-- =====================================================================
-- 第 2 步：建联合索引 (category_id, status, price)
-- =====================================================================
CREATE INDEX idx_cat_status_price
ON mall_product (category_id, status, price);

EXPLAIN SELECT *
FROM mall_product
WHERE category_id = 37 AND status = 1
ORDER BY price DESC
LIMIT 20;
-- type = ref, rows = 847, Extra = Backward index scan
-- ✅ 三个条件全部命中索引：等值(category_id,status) + price 有序 → 无需 filesort
-- ⚠️ 但 Extra 里没有 Using index —— 说明 MySQL 只“用了索引定位”，
--    要返回的 product_name、create_time 不在索引里，每行都要回表去取。
--    注意：847 行命中，EXPLAIN 里是看不出“回表”字样的，回表是一种动作不是标记。

-- 就算只想要 4 个字段，只要 stock 不在索引里，照样回表：
EXPLAIN SELECT id, category_id, price, stock
FROM mall_product
WHERE category_id = 37 AND status = 1
ORDER BY price DESC
LIMIT 20;
-- 和上面一模一样（Extra = Backward index scan，没有 Using index）

-- =====================================================================
-- 第 3 步：把要返回的 stock 也“塞进”索引 → 覆盖索引
-- =====================================================================
DROP INDEX idx_cat_status_price ON mall_product;
CREATE INDEX idx_cat_status_price_stock
ON mall_product (category_id, status, price, stock);

-- 3.1 只 SELECT 索引里有的列：不再回表！
EXPLAIN SELECT id, category_id, price, stock
FROM mall_product
WHERE category_id = 37 AND status = 1
ORDER BY price DESC
LIMIT 20;
-- Extra = Backward index scan; Using index   ← “Using index” 出现 = 覆盖索引
-- 意味着：从索引里就能拿到全部需要的列，一次表都不用回。
-- id 为什么够？二级索引叶子自带主键，所以主键永远“免费覆盖”。

-- 3.2 同一索引，只要 SELECT *，product_name/create_time 索引里没有 → 仍要回表：
EXPLAIN SELECT *
FROM mall_product
WHERE category_id = 37 AND status = 1
ORDER BY price DESC
LIMIT 20;
-- Extra = Backward index scan（没有 Using index）→ 每行回表取完整行

-- 3.3 进阶彩蛋：统计接口也可以靠“覆盖索引”直接扫索引完成
EXPLAIN SELECT category_id, COUNT(*) AS c, ROUND(AVG(price), 2) AS avg_p, SUM(stock) AS s
FROM mall_product
GROUP BY category_id;
-- type = index（全索引扫描，不碰表）、Extra = Using index
-- 因为 category_id/price/stock/主键全在索引里，统计只扫索引就够了

-- =====================================================================
-- ④ 结论：回表 vs 覆盖索引 & 为什么别随手 SELECT *
-- =====================================================================
-- 回表一次 = 一次主键索引的随机 IO（磁盘上翻页）；
-- 命中 N 行 = 最多 N 次回表。行数小时感觉不到，行数大、宽表时差距是数量级。
-- SELECT * 会强制 MySQL 把“索引里没有的列”都回表取一遍；
-- 生产经验：列表接口按需列出要展示的列，既是流量优化，也是给“覆盖索引”留机会。
-- 覆盖索引也非免费：索引列越多，插入/更新维护成本越高，只覆盖“高频查询”即可。

-- 一句话记忆锚点：
--   二级索引里只有“索引字段 + 主键”，要的列不在索引里就得回表；
--   EXPLAIN 出现 Using index = 覆盖索引 = 不用回表。
