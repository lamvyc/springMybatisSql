-- =============================================================
-- sql-learning-platform 数据库设计
-- 说明：经典 emp/dept 教学模型被迁移到现代互联网业务场景
--   emp     -> employee  员工表（含 leader_id 领导关系、salary 薪资）
--   dept    -> department 部门表（含 dept_code 部门编码）
--   salgrade-> salary_grade 薪资等级表
--   S/C/SC  -> student/course/student_course 学生选课表
-- =============================================================

CREATE DATABASE IF NOT EXISTS sql_learning_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE sql_learning_platform;

-- -------------------------------------------------------------
-- 1. 用户中心：user（注意：user 是 MySQL 关键字，需反引号包裹）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20)  COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    INDEX idx_user_status (status),
    INDEX idx_user_create_time (create_time)
) COMMENT '用户表';

-- -------------------------------------------------------------
-- 2. 组织员工中心：department 部门表
--    经典 dept 迁移：deptno->id, dname->dept_name, loc->parent_id
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(20) COMMENT '部门编码：如 TECH/MKT/SALES（用于字符匹配类题目）',
    parent_id BIGINT      NOT NULL DEFAULT 0 COMMENT '上级部门ID，0表示顶级'
) COMMENT '部门表';

-- -------------------------------------------------------------
-- 3. 组织员工中心：employee 员工表
--    经典 emp 迁移：empno->id, ename->name, mgr->leader_id,
--                   sal->salary, job->position, hiredate->hire_date
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employee (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工ID',
    name      VARCHAR(50) NOT NULL COMMENT '员工姓名',
    dept_id   BIGINT      NOT NULL COMMENT '所属部门ID',
    leader_id BIGINT      DEFAULT NULL COMMENT '直属领导ID，指向 employee.id',
    salary    DECIMAL(10, 2) COMMENT '月薪',
    position  VARCHAR(50) COMMENT '职位：总裁/总监/高级工程师/市场专员/初级工程师等',
    hire_date DATE        COMMENT '入职日期',
    INDEX idx_emp_dept (dept_id),
    INDEX idx_emp_leader (leader_id),
    INDEX idx_emp_salary (salary)
) COMMENT '员工表';

-- -------------------------------------------------------------
-- 4. 组织员工中心：salary_grade 薪资等级表
--    经典 salgrade 迁移：grade/losal/hisal 语义保留为薪资区间
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS salary_grade (
    grade      INT PRIMARY KEY COMMENT '等级',
    low_salary DECIMAL(10, 2) NOT NULL COMMENT '该等级最低薪资',
    high_salary DECIMAL(10, 2) NOT NULL COMMENT '该等级最高薪资'
) COMMENT '薪资等级表';

-- -------------------------------------------------------------
-- 5. 商品中心：category 商品分类表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
) COMMENT '商品分类表';

-- -------------------------------------------------------------
-- 6. 商品中心：product 商品表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    category_id BIGINT COMMENT '分类ID',
    price       DECIMAL(10, 2) COMMENT '单价',
    stock       INT          NOT NULL DEFAULT 0 COMMENT '库存',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_category (category_id),
    INDEX idx_product_status (status)
) COMMENT '商品表';

-- -------------------------------------------------------------
-- 7. 订单中心：orders 订单表（order 是关键字，故用 orders 复数）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT COMMENT '下单用户ID',
    order_no     VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    total_amount DECIMAL(12, 2) COMMENT '订单总金额',
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已发货 3已完成 4已取消',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_create_time (create_time),
    INDEX idx_orders_status (status)
) COMMENT '订单表';

-- -------------------------------------------------------------
-- 8. 订单中心：order_item 订单明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT COMMENT '订单ID',
    product_id BIGINT COMMENT '商品ID',
    quantity   INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    price      DECIMAL(10, 2) COMMENT '成交单价',
    INDEX idx_item_order (order_id),
    INDEX idx_item_product (product_id)
) COMMENT '订单明细表';

-- -------------------------------------------------------------
-- 9. 权限中心：role 角色表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) COMMENT '角色表';

-- -------------------------------------------------------------
-- 10. 权限中心：user_role 用户角色关联表（多对多）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) COMMENT '用户角色关联表';

-- -------------------------------------------------------------
-- 11. 行为日志中心：operation_log 用户行为日志表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT COMMENT '操作人ID',
    operation   VARCHAR(255) COMMENT '操作内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_user (user_id),
    INDEX idx_log_create_time (create_time)
) COMMENT '行为日志表';

-- -------------------------------------------------------------
-- 12. 用户登录日志表：支撑"连续登录"分析题
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT COMMENT '用户ID',
    login_date DATE COMMENT '登录日期',
    UNIQUE KEY uk_user_date (user_id, login_date)
) COMMENT '登录日志表';

-- -------------------------------------------------------------
-- 13. 数据统计中心（学生选课模块）：student 学生表
--     经典第35题 S 表迁移
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age  INT
) COMMENT '学生表';

-- -------------------------------------------------------------
-- 14. 数据统计中心：course 课程表（经典第35题 C 表迁移）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS course (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    teacher VARCHAR(50) COMMENT '授课老师'
) COMMENT '课程表';

-- -------------------------------------------------------------
-- 15. 数据统计中心：student_course 选课表（经典第35题 SC 表迁移）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_course (
    student_id BIGINT NOT NULL,
    course_id  BIGINT NOT NULL,
    score      DECIMAL(5, 2) COMMENT '成绩',
    PRIMARY KEY (student_id, course_id)
) COMMENT '选课表';

-- -------------------------------------------------------------
-- 16. SQL实验中心：sql_case 题目表
--     standard_sql 保存标准答案，expected_result 保存正确结果(JSON)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sql_case (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100) NOT NULL COMMENT '题目标题',
    description     TEXT COMMENT '题目描述（业务场景）',
    difficulty      VARCHAR(20)  COMMENT '难度：入门/进阶/困难',
    knowledge_point VARCHAR(100) COMMENT '核心知识点：join/group by/window function...',
    standard_sql    TEXT COMMENT '标准SQL答案',
    expected_result JSON COMMENT '正确结果，格式：{"columns":[...],"rows":[[...]]}'
) COMMENT 'SQL实验题目表';
