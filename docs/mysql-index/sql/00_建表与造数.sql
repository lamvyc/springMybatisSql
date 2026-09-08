 -- =====================================================================
-- 00_建表与造数.sql
-- MySQL 索引实战 · 准备阶段：创建业务表 + 生成百万级测试数据
--
-- 【怎么用】
--   1. 先启动你的应用一次（保证数据库 sql_learning_platform 已存在）
--      或手动执行：CREATE DATABASE IF NOT EXISTS sql_learning_platform
--   2. mysql -h127.0.0.1 -uroot -proot1234 sql_learning_platform < 00_建表与造数.sql
--   3. 造数规模在下方三个 @变量 里改，默认：用户30万 + 商品20万 + 订单100万
--
-- 【为什么要单独建一套 mall_* 表】
--   本工程原有的 user/orders/product 表只有几十行、专供 SQL 实验台 35 题用。
--   索引学习必须有“数据量大到明显影响性能”的表，所以新建独立业务表，
--   前缀 mall_ 与原有表完全隔离，互不影响。
-- =====================================================================

SET NAMES utf8mb4;

-- ① 造数规模配置（想快一点就把数字改小；想更接近生产就改大）
--    注意：本文件会 DROP 并重建 mall_* 三张表，等于“一键重置”到无二级索引的初始状态。
SET @USER_N    = 300000;    -- 用户行数
SET @PRODUCT_N = 200000;    -- 商品行数（分类 1..200，平均每类 1000 个）
SET @ORDER_N   = 1000000;   -- 订单行数

-- 递归 CTE 一次生成 100 万行，需要调大 MySQL 的递归深度上限（默认只有 1000）
SET SESSION cte_max_recursion_depth = 1000000;

-- =====================================================================
-- ② 建表：刻意【只保留主键索引 + 业务唯一索引】，先不建任何二级索引，
--    这样第 1、2 阶段才能亲眼看到“没有索引时 MySQL 怎么全表扫”。
-- =====================================================================
DROP TABLE IF EXISTS mall_order;
DROP TABLE IF EXISTS mall_product;
DROP TABLE IF EXISTS mall_user;

-- 用户表 --------------------------------------------------------------
CREATE TABLE mall_user (
    id            BIGINT       NOT NULL COMMENT '用户ID（主键）',
    username      VARCHAR(32)  NOT NULL COMMENT '用户名（唯一：登录名不能重复）',
    phone         VARCHAR(20)           COMMENT '手机号（存的是字符串！为第5阶段“隐式类型转换”埋伏笔）',
    email         VARCHAR(64)           COMMENT '邮箱',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    register_time DATETIME     NOT NULL COMMENT '注册时间',
    PRIMARY KEY (id),                       -- 主键索引：InnoDB 聚簇索引
    UNIQUE KEY uk_username (username)       -- 业务唯一索引（username 天生该唯一）
) ENGINE = InnoDB COMMENT '索引实战-用户表';

-- 商品表 --------------------------------------------------------------
CREATE TABLE mall_product (
    id           BIGINT        NOT NULL COMMENT '商品ID（主键）',
    product_name VARCHAR(64)   NOT NULL COMMENT '商品名称',
    category_id  INT           NOT NULL COMMENT '商品分类ID（后台“按分类翻商品”就用它过滤）',
    price        DECIMAL(10,2) NOT NULL COMMENT '售价',
    stock        INT           NOT NULL COMMENT '库存',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    create_time  DATETIME      NOT NULL COMMENT '上架时间',
    PRIMARY KEY (id)                        -- 只留主键索引
) ENGINE = InnoDB COMMENT '索引实战-商品表';

-- 订单表 --------------------------------------------------------------
CREATE TABLE mall_order (
    id          BIGINT        NOT NULL COMMENT '订单ID（主键）',
    order_no    VARCHAR(32)   NOT NULL COMMENT '订单号（唯一：对外展示的业务单号）',
    user_id     BIGINT        NOT NULL COMMENT '下单用户ID（“我的订单”查询条件）',
    status      TINYINT       NOT NULL COMMENT '状态：0待支付 1已支付 2已发货 3已完成 4已取消',
    pay_amount  DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    create_time DATETIME      NOT NULL COMMENT '下单时间',
    PRIMARY KEY (id),                       -- 主键索引
    UNIQUE KEY uk_order_no (order_no)       -- 业务唯一索引（订单号）
) ENGINE = InnoDB COMMENT '索引实战-订单表';

-- =====================================================================
-- ③ 造数
--    思路：WITH RECURSIVE 一次性生成 1..N 的序号列，再为每一行拼出
--    业务字段。user_id/status/create_time 都用加权随机，模拟真实分布：
--    - 订单向“少数活跃用户”倾斜（前 10% 用户拿走约 80% 订单）
--    - status=1(已支付) 占比最高
-- =====================================================================

-- 3.1 mall_user：30 万用户
INSERT INTO mall_user (id, username, phone, email, status, register_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @USER_N
)
SELECT n,
       CONCAT('user_', n),                                          -- 用户名唯一
       CONCAT('1', LPAD(FLOOR(RAND() * 10000000000), 10, '0')),     -- 11 位手机号（存为字符串）
       CONCAT('user', n, '@example.com'),
       IF(RAND() < 0.98, 1, 0),                                     -- 98% 正常用户
       TIMESTAMPADD(SECOND,
                    FLOOR(RAND() * 900 * 86400),                    -- 近 900 天内随机
                    '2022-01-01 00:00:00')
FROM seq;

-- 3.2 mall_product：20 万商品，分类 1..200
INSERT INTO mall_product (id, product_name, category_id, price, stock, status, create_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @PRODUCT_N
)
SELECT n,
       CONCAT('商品_', n),
       1 + FLOOR(RAND() * 200),            -- 200 个分类
       ROUND(5 + RAND() * 4995, 2),        -- 5 ~ 5000 元
       FLOOR(RAND() * 1000),               -- 0~999 件库存
       IF(RAND() < 0.85, 1, 0),            -- 85% 上架
       TIMESTAMPADD(SECOND,
                    FLOOR(RAND() * 1095 * 86400),
                    '2022-01-01 00:00:00')
FROM seq;

-- 3.3 mall_order：100 万订单
INSERT INTO mall_order (id, order_no, user_id, status, pay_amount, create_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @ORDER_N
)
SELECT n,
       CONCAT('ORD', LPAD(n, 10, '0')),                            -- 订单号唯一
       1 + IF(RAND() < 0.8,                                        -- 80% 落在前 10% 活跃用户
              FLOOR(RAND() * (@USER_N * 0.1)),
              FLOOR(RAND() * @USER_N)),
       CASE FLOOR(RAND() * 10)                                      -- 状态分布（FLOOR(RAND()*10)=0..9）
            WHEN 9 THEN 0   -- 待支付 10%
            WHEN 8 THEN 2   -- 已发货 10%
            WHEN 6 THEN 4   -- 已取消 20%（6、7 两个分支）
            WHEN 7 THEN 4
            WHEN 5 THEN 3   -- 已完成 10%
            ELSE 1          -- 已支付 50%（0、1、2、3、4 五个分支）
       END,
       ROUND(9.9 + RAND() * 19990.1, 2),                           -- 10 ~ 20000 元
       TIMESTAMPADD(SECOND,
                    FLOOR(RAND() * 730 * 86400),
                    '2023-01-01 00:00:00')                         -- 2023-01-01 ~ 2024-12-31
FROM seq;

-- =====================================================================
-- ④ 更新统计信息 + 验证
--    大批量插入后必须 ANALYZE TABLE，否则优化器手里的索引基数(cardinality)
--    还是旧值，EXPLAIN 预估的 rows 会失真（这是真实项目里常见的坑）。
-- =====================================================================
ANALYZE TABLE mall_user, mall_product, mall_order;

SELECT 'mall_user'    AS 表名, COUNT(*) AS 行数 FROM mall_user
UNION ALL
SELECT 'mall_product',          COUNT(*) FROM mall_product
UNION ALL
SELECT 'mall_order',            COUNT(*) FROM mall_order;

SHOW INDEX FROM mall_user;      -- 应看到 PRIMARY、uk_username
SHOW INDEX FROM mall_product;   -- 应看到 PRIMARY
SHOW INDEX FROM mall_order;     -- 应看到 PRIMARY、uk_order_no
