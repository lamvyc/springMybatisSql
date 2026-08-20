package com.dev.springmybatissql.config;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 实验中心题目枚举
 * 每道题定义为枚举常量，统一管理 id、title、description、difficulty、knowledgePoint、standardSql
 *
 * 所有表结构统一定义在 {@link #TABLE_SCHEMAS}，题目只需声明涉及的 tableKey 即可自动拼接。
 */
@Getter
public enum SqlCaseEnum {

    // ==================== 经典35题迁移 ====================
    CASE_01(1L, "每个部门薪资最高的员工（经典第1题）",
            List.of("employee", "department"),
            "第一步：按部门分组用MAX求最高薪资；第二步：员工表JOIN该结果匹配部门+薪资",
            "group by、max、join",
            "先聚合得到每部门最高薪资，再关联员工表取出对应员工；注意同一薪资多人会返回多行",
            "入门", "group by/join",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "salary（月薪，来源 employee.salary）"),
            "SELECT e.name AS employee_name, d.dept_name, e.salary FROM employee e JOIN department d ON e.dept_id = d.id JOIN (SELECT dept_id, MAX(salary) AS max_salary FROM employee GROUP BY dept_id) m ON e.dept_id = m.dept_id AND e.salary = m.max_salary ORDER BY d.id"),

    CASE_02(2L, "薪资高于部门平均薪资的员工（经典第2题）",
            List.of("employee", "department"),
            "第一步：按部门分组求平均薪资；第二步：员工表JOIN该结果，WHERE过滤 salary大于平均",
            "join、group by、子查询",
            "关联子查询是经典做法，先算部门均值再比较",
            "入门", "join/子查询",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "salary（月薪，来源 employee.salary）",
                    "dept_avg_salary（部门平均薪资，ROUND(AVG(employee.salary), 2)）"),
            "SELECT e.name AS employee_name, d.dept_name, e.salary, ROUND(a.avg_salary, 2) AS dept_avg_salary FROM employee e JOIN department d ON e.dept_id = d.id JOIN (SELECT dept_id, AVG(salary) AS avg_salary FROM employee GROUP BY dept_id) a ON e.dept_id = a.dept_id WHERE e.salary > a.avg_salary ORDER BY d.id, e.salary DESC"),

    CASE_03(3L, "部门平均薪资等级统计（经典第3/13题）",
            List.of("employee", "department", "salary_grade"),
            "第一步：员工薪资与薪资等级表做非等值连接（BETWEEN）；第二步：按部门+等级分组统计人数与平均薪资",
            "非等值连接、group by",
            "薪资区间匹配必须用 BETWEEN 范围连接，不能用等值连接",
            "进阶", "非等值连接",
            List.of("dept_name（部门名称，来源 department.dept_name）",
                    "grade（薪资等级，来源 salary_grade.grade）",
                    "employee_count（该部门该等级的员工数，COUNT(employee.id)）",
                    "avg_salary（该等级平均薪资，ROUND(AVG(employee.salary), 2)）"),
            "SELECT d.dept_name, sg.grade, COUNT(e.id) AS employee_count, ROUND(AVG(e.salary), 2) AS avg_salary FROM salary_grade sg LEFT JOIN employee e ON e.salary BETWEEN sg.low_salary AND sg.high_salary LEFT JOIN department d ON e.dept_id = d.id GROUP BY d.dept_name, sg.grade ORDER BY d.dept_name, sg.grade"),

    CASE_05(5L, "不用MAX函数求最高薪资（经典第5题）",
            List.of("employee"),
            "方案一：ORDER BY salary DESC LIMIT 1；方案二：自连接或子查询取最大的 salary",
            "order by、limit、替代方案",
            "面试常考两种解法：排序取第一条、或者用 NOT EXISTS 找没有比自己更高的员工",
            "进阶", "order by/limit",
            List.of("salary（最高薪资，来源 employee.salary）"),
            "SELECT salary FROM employee ORDER BY salary DESC LIMIT 1"),

    CASE_08(8L, "平均薪资最高的部门（经典第6/7题）",
            List.of("employee", "department"),
            "第一步：JOIN部门按部门分组求平均薪资；第二步：ORDER BY平均薪资DESC LIMIT 1",
            "group by、order by、limit",
            "分组聚合后排序取第一条即可；若要并列第一需用窗口函数",
            "入门", "group by/limit",
            List.of("dept_id（部门ID，来源 department.id）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "avg_salary（部门平均薪资，ROUND(AVG(employee.salary), 2)）"),
            "SELECT d.id AS dept_id, d.dept_name, ROUND(AVG(e.salary), 2) AS avg_salary FROM employee e JOIN department d ON e.dept_id = d.id GROUP BY d.id, d.dept_name ORDER BY AVG(e.salary) DESC LIMIT 1"),

    CASE_10(10L, "薪资最高前5名 / 第6~10名（经典第10/11题）",
            List.of("employee"),
            "ORDER BY salary DESC + LIMIT offset,size；前5: LIMIT 0,5；第6~10: LIMIT 5,5",
            "order by、limit offset",
            "全局TopN用LIMIT即可；分组TopN必须用窗口函数",
            "入门", "limit/offset",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "salary（月薪，来源 employee.salary）",
                    "position（职位，来源 employee.position）"),
            "SELECT name AS employee_name, salary, position FROM employee ORDER BY salary DESC LIMIT 0, 5"),

    CASE_15(15L, "入职早于直接上级的员工（经典第15题）",
            List.of("employee", "department"),
            "第一步：employee自连接 员工.leader_id=领导.id；第二步：WHERE 员工入职日期 < 领导入职日期",
            "自连接",
            "员工表存领导ID即可自连接查出领导信息，注意区分别名",
            "进阶", "自连接",
            List.of("employee_id（员工ID，来源 employee.id）",
                    "employee_name（员工姓名，来源 employee.name）",
                    "employee_hire_date（员工入职日期，来源 employee.hire_date）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "leader_name（领导姓名，来源 employee.name 自连接（leader_id 指向））",
                    "leader_hire_date（领导入职日期，来源 employee.hire_date 自连接）"),
            "SELECT e.id AS employee_id, e.name AS employee_name, e.hire_date AS employee_hire_date, d.dept_name, l.name AS leader_name, l.hire_date AS leader_hire_date FROM employee e JOIN employee l ON e.leader_id = l.id JOIN department d ON e.dept_id = d.id WHERE e.hire_date < l.hire_date"),

    CASE_16(16L, "所有部门及员工信息（含无员工部门，经典第16题）",
            List.of("department", "employee"),
            "department 作为驱动表 LEFT JOIN employee，保留没有员工的部门",
            "left join、驱动表方向",
            "需要保留主表全部记录时，一定要让主表在 LEFT JOIN 左侧",
            "入门", "left join",
            List.of("dept_id（部门ID，来源 department.id）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "employee_id（员工ID，来源 employee.id，无员工部门为 NULL）",
                    "employee_name（员工姓名，来源 employee.name）",
                    "salary（月薪，来源 employee.salary）"),
            "SELECT d.id AS dept_id, d.dept_name, e.id AS employee_id, e.name AS employee_name, e.salary FROM department d LEFT JOIN employee e ON e.dept_id = d.id ORDER BY d.id, e.id"),

    CASE_17(17L, "员工数不少于阈值的部门（经典第17题）",
            List.of("department", "employee"),
            "按部门分组后 HAVING COUNT 过滤；HAVING用于分组后条件",
            "having、count",
            "WHERE 过滤行，HAVING 过滤分组，这是关键区别",
            "入门", "having",
            List.of("dept_id（部门ID，来源 department.id）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "employee_count（员工数，COUNT(employee.id)）"),
            "SELECT d.id AS dept_id, d.dept_name, COUNT(e.id) AS employee_count FROM department d JOIN employee e ON e.dept_id = d.id GROUP BY d.id, d.dept_name HAVING COUNT(e.id) >= 5 ORDER BY employee_count DESC"),

    CASE_21(21L, "指定编码部门员工名单（经典第21题）",
            List.of("employee", "department"),
            "通过 dept_code='SALES' 定位部门，再JOIN员工；业务上不直接暴露部门ID",
            "join、业务编码设计",
            "用业务编码而非物理主键做跨系统关联是常见设计",
            "入门", "join",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "salary（月薪，来源 employee.salary）",
                    "dept_name（部门名称，来源 department.dept_name）"),
            "SELECT e.name AS employee_name, e.salary, d.dept_name FROM employee e JOIN department d ON e.dept_id = d.id WHERE d.dept_code = 'SALES' ORDER BY e.salary DESC"),

    CASE_22(22L, "高于公司平均薪资的员工及部门/领导/等级（经典第22题）",
            List.of("employee", "department", "salary_grade"),
            "三步：①子查询求公司平均薪资 ②三表JOIN ③薪资等级非等值连接",
            "子查询、多表join、非等值连接",
            "综合题考察多表关联的熟练度，注意 JOIN 顺序不影响结果只影响性能",
            "困难", "多表join/子查询",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "leader_name（领导姓名，来源 employee.name 自连接，无领导为 NULL）",
                    "salary_grade（薪资等级，来源 salary_grade.grade）",
                    "salary（月薪，来源 employee.salary）"),
            "SELECT e.name AS employee_name, d.dept_name, l.name AS leader_name, sg.grade AS salary_grade, e.salary FROM employee e JOIN department d ON e.dept_id = d.id LEFT JOIN employee l ON e.leader_id = l.id JOIN salary_grade sg ON e.salary BETWEEN sg.low_salary AND sg.high_salary WHERE e.salary > (SELECT AVG(salary) FROM employee) ORDER BY e.salary DESC"),

    CASE_25(25L, "薪资高于某部门所有员工的人（经典第25题）",
            List.of("employee", "department"),
            "salary > ALL(子查询某部门全部薪资)",
            "all关键字、子查询",
            "ALL 等价于大于子查询最大值；也可用 MAX 子查询替换",
            "困难", "all/子查询",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "salary（月薪，来源 employee.salary）",
                    "dept_name（部门名称，来源 department.dept_name）"),
            "SELECT e.name AS employee_name, e.salary, d.dept_name FROM employee e JOIN department d ON e.dept_id = d.id WHERE d.id != 2 AND e.salary > ALL (SELECT salary FROM employee WHERE dept_id = 2 AND salary IS NOT NULL) ORDER BY e.salary"),

    CASE_26(26L, "每个部门员工数/平均薪资/平均服务年限（经典第26题）",
            List.of("department", "employee"),
            "LEFT JOIN + GROUP BY；用 TIMESTAMPDIFF 计算服务年限",
            "group by、日期函数",
            "服务年限计算注意用 YEAR 还是按天/365，业务口径要一致",
            "进阶", "日期函数/group by",
            List.of("dept_id（部门ID，来源 department.id）",
                    "dept_name（部门名称，来源 department.dept_name）",
                    "employee_count（员工数，COUNT(employee.id)，无员工部门为 0）",
                    "avg_salary（部门平均薪资，ROUND(AVG(employee.salary), 2)）",
                    "avg_service_years（平均服务年限，ROUND(AVG(TIMESTAMPDIFF(YEAR, employee.hire_date, CURDATE())), 1)）"),
            "SELECT d.id AS dept_id, d.dept_name, COUNT(e.id) AS employee_count, ROUND(AVG(e.salary), 2) AS avg_salary, ROUND(AVG(TIMESTAMPDIFF(YEAR, e.hire_date, CURDATE())), 1) AS avg_service_years FROM department d LEFT JOIN employee e ON e.dept_id = d.id GROUP BY d.id, d.dept_name ORDER BY d.id"),

    CASE_31(31L, "员工年工资排行（经典第31题）",
            List.of("employee"),
            "salary*13 模拟含13薪的年薪，ORDER BY 排序",
            "表达式、order by",
            "排序字段可以是计算列，注意别省略 ORDER BY 断言稳定性",
            "入门", "表达式排序",
            List.of("employee_name（员工姓名，来源 employee.name）",
                    "month_salary（月薪，来源 employee.salary）",
                    "annual_salary（年薪，employee.salary * 13）"),
            "SELECT name AS employee_name, salary AS month_salary, salary * 13 AS annual_salary FROM employee ORDER BY annual_salary"),

    // ==================== 学生选课综合（经典第35题） ====================
    CASE_35(35L, "没选过某老师课程的学生（经典35题-1）",
            List.of("student", "course", "student_course"),
            "NOT EXISTS 子查询：找出不存在选课记录的学生",
            "not exists、多对多",
            "NOT EXISTS 比 NOT IN 更安全（后者遇到NULL会出问题）",
            "进阶", "not exists",
            List.of("student_name（学生姓名，来源 student.name）"),
            "SELECT s.name AS student_name FROM student s WHERE NOT EXISTS (SELECT 1 FROM student_course sc JOIN course c ON sc.course_id = c.id WHERE sc.student_id = s.id AND c.teacher = '黎明') ORDER BY s.id"),

    CASE_36(36L, "2门以上不及格学生姓名及平均（经典35题-2）",
            List.of("student", "student_course"),
            "HAVING SUM(score<60)>=2 过滤不及格门数，AVG求平均成绩",
            "having、条件聚合",
            "SUM(score<60) 利用布尔表达式统计满足条件的行数",
            "困难", "having/条件聚合",
            List.of("student_name（学生姓名，来源 student.name）",
                    "avg_score（平均成绩，ROUND(AVG(student_course.score), 2)）",
                    "fail_count（不及格门数，SUM(CASE WHEN score < 60 THEN 1 ELSE 0 END)）"),
            "SELECT s.name AS student_name, ROUND(AVG(sc.score), 2) AS avg_score, SUM(CASE WHEN sc.score < 60 THEN 1 ELSE 0 END) AS fail_count FROM student s JOIN student_course sc ON s.id = sc.student_id GROUP BY s.id, s.name HAVING SUM(CASE WHEN sc.score < 60 THEN 1 ELSE 0 END) >= 2 ORDER BY avg_score"),

    CASE_37(37L, "既学课程1又学课程2的学生（经典35题-3）",
            List.of("student", "student_course"),
            "同表两次JOIN，分别关联课程1和课程2",
            "多表join、多对多",
            "同一张关联表需用两次JOIN时，必须使用不同别名",
            "进阶", "多次join",
            List.of("student_name（学生姓名，来源 student.name）"),
            "SELECT s.name AS student_name FROM student s JOIN student_course sc1 ON s.id = sc1.student_id AND sc1.course_id = 1 JOIN student_course sc2 ON s.id = sc2.student_id AND sc2.course_id = 2 GROUP BY s.id, s.name ORDER BY s.id"),

    // ==================== 现代业务SQL扩展 ====================
    CASE_40(40L, "连续登录N天的用户（现代业务-连续问题）",
            List.of("login_log"),
            "窗口函数差集法：①ROW_NUMBER按用户分区日期排序得序号 ②日期-序号得到分组标识 ③按用户+分组聚合，HAVING>=N",
            "row_number、日期差值、连续问题",
            "连续问题通用解法：日期减去行号，连续日期差值相同，经典面试题",
            "困难", "窗口函数/连续问题",
            List.of("user_id（用户ID，来源 login_log.user_id）",
                    "start_date（连续登录开始日期，MIN(login_log.login_date)）",
                    "end_date（连续登录结束日期，MAX(login_log.login_date)）",
                    "continuous_days（连续登录天数，COUNT(*)）"),
            "SELECT user_id, MIN(login_date) AS start_date, MAX(login_date) AS end_date, COUNT(*) AS continuous_days FROM (SELECT user_id, login_date, DATE_SUB(login_date, INTERVAL ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) DAY) AS grp FROM login_log) t GROUP BY user_id, grp HAVING COUNT(*) >= 3 ORDER BY user_id"),

    CASE_41(41L, "每个分类销量Top3商品（现代业务-分组TopN）",
            List.of("category", "product", "order_item"),
            "①聚合求出各商品销量 ②ROW_NUMBER按分类分区、销量排序 ③外层过滤rn<=3",
            "row_number、分组TopN",
            "为什么LIMIT不行？LIMIT只能解决全局TopN，分组TopN必须用窗口函数",
            "困难", "窗口函数/分组TopN",
            List.of("category_name（分类名，来源 category.name）",
                    "product_name（商品名，来源 product.name）",
                    "total_quantity（总销量，SUM(order_item.quantity)）",
                    "rn（分类内销量排名，ROW_NUMBER()，1~3）"),
            "SELECT category_name, product_name, total_quantity, rn FROM (SELECT c.name AS category_name, p.name AS product_name, SUM(oi.quantity) AS total_quantity, ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY SUM(oi.quantity) DESC) AS rn FROM product p JOIN category c ON p.category_id = c.id LEFT JOIN order_item oi ON p.id = oi.product_id GROUP BY c.id, c.name, p.id, p.name) t WHERE rn <= 3 ORDER BY category_name, rn"),

    CASE_43(43L, "每月销售额Top3商品（现代业务-月度排行）",
            List.of("orders", "order_item", "product"),
            "DATE_FORMAT取月份 + ROW_NUMBER按月分区排序",
            "日期格式化、窗口函数",
            "月度/周度排行都是按时间维度分区做窗口排序",
            "困难", "窗口函数/时间维度",
            List.of("sale_month（销售月份，DATE_FORMAT(orders.create_time, '%Y-%m')）",
                    "product_name（商品名，来源 product.name）",
                    "sale_amount（销售额，SUM(order_item.quantity * order_item.price)）",
                    "rn（月度销售额排名，ROW_NUMBER()，1~3）"),
            "SELECT sale_month, product_name, sale_amount, rn FROM (SELECT DATE_FORMAT(o.create_time, '%Y-%m') AS sale_month, p.name AS product_name, SUM(oi.quantity * oi.price) AS sale_amount, ROW_NUMBER() OVER (PARTITION BY DATE_FORMAT(o.create_time, '%Y-%m') ORDER BY SUM(oi.quantity * oi.price) DESC) AS rn FROM order_item oi JOIN `orders` o ON oi.order_id = o.id JOIN product p ON oi.product_id = p.id GROUP BY sale_month, p.id, p.name) t WHERE rn <= 3 ORDER BY sale_month DESC, rn"),

    CASE_45(45L, "订单状态分布统计（现代业务-行列转换）",
            List.of("orders"),
            "CASE WHEN 条件聚合：把多个状态值转成多列",
            "case when、条件聚合",
            "行列转换的核心技巧是用 CASE WHEN + SUM/COUNT 把行转成列",
            "进阶", "case when/行列转换",
            List.of("pending_payment（待支付订单数，COUNT(CASE WHEN status = 0)）",
                    "paid（已支付订单数，COUNT(CASE WHEN status = 1)）",
                    "shipped（已发货订单数，COUNT(CASE WHEN status = 2)）",
                    "completed（已完成订单数，COUNT(CASE WHEN status = 3)）",
                    "cancelled（已取消订单数，COUNT(CASE WHEN status = 4)）",
                    "total（订单总数，COUNT(*)）"),
            "SELECT COUNT(CASE WHEN status = 0 THEN 1 END) AS pending_payment, COUNT(CASE WHEN status = 1 THEN 1 END) AS paid, COUNT(CASE WHEN status = 2 THEN 1 END) AS shipped, COUNT(CASE WHEN status = 3 THEN 1 END) AS completed, COUNT(CASE WHEN status = 4 THEN 1 END) AS cancelled, COUNT(*) AS total FROM `orders`"),

    CASE_46(46L, "用户消费金额排行（现代业务-rank并列排名）",
            List.of("user", "orders"),
            "①按用户聚合消费总额 ②rank()窗口函数按金额排序，相同金额并列",
            "rank、窗口函数",
            "rank并列则跳号、dense_rank不跳号、row_number不并列，三者的区别是高频考点",
            "进阶", "rank/窗口函数",
            List.of("username（用户名，来源 user.username）",
                    "total_amount（消费总额，SUM(orders.total_amount)，无消费为 0）",
                    "rank_no（消费金额排名，rank()，相同金额并列）"),
            "SELECT u.username, COALESCE(c.total_amount, 0) AS total_amount, rank() OVER (ORDER BY COALESCE(c.total_amount, 0) DESC) AS rank_no FROM `user` u LEFT JOIN (SELECT user_id, SUM(total_amount) AS total_amount FROM `orders` WHERE status IN (1, 2, 3) GROUP BY user_id) c ON u.id = c.user_id ORDER BY rank_no");

    // ================================================================
    // 全量表结构（统一定义一次，题目按 tableKey 引用）
    // ================================================================
    private static final Map<String, String> TABLE_SCHEMAS = new LinkedHashMap<>();
    static {
        TABLE_SCHEMAS.put("employee",
                "┌─ employee（员工表）──────────────────────────────┐\n" +
                "│ id (PK)    │ BIGINT      │ 员工ID                │\n" +
                "│ name       │ VARCHAR(50) │ 员工姓名               │\n" +
                "│ dept_id    │ BIGINT      │ 所属部门ID             │\n" +
                "│ leader_id  │ BIGINT      │ 直属领导ID（FK→本表）   │\n" +
                "│ salary     │ DECIMAL     │ 月薪                   │\n" +
                "│ position   │ VARCHAR(50) │ 职位                   │\n" +
                "│ hire_date  │ DATE        │ 入职日期               │\n" +
                "└──────────────────────────────────────────────────┘");

        TABLE_SCHEMAS.put("department",
                "┌─ department（部门表）────────────────┐\n" +
                "│ id (PK)    │ BIGINT      │ 部门ID    │\n" +
                "│ dept_name  │ VARCHAR(50) │ 部门名称   │\n" +
                "│ dept_code  │ VARCHAR(20) │ 部门编码   │\n" +
                "│ parent_id  │ BIGINT      │ 上级部门ID │\n" +
                "└──────────────────────────────────────┘");

        TABLE_SCHEMAS.put("salary_grade",
                "┌─ salary_grade（薪资等级表）──────────────┐\n" +
                "│ grade (PK)   │ INT     │ 等级             │\n" +
                "│ low_salary   │ DECIMAL │ 该等级最低薪资    │\n" +
                "│ high_salary  │ DECIMAL │ 该等级最高薪资    │\n" +
                "└──────────────────────────────────────────┘");

        TABLE_SCHEMAS.put("student",
                "┌─ student（学生表）───────┐\n" +
                "│ id (PK) │ BIGINT │ 学生ID│\n" +
                "│ name    │ VARCHAR│ 姓名  │\n" +
                "│ age     │ INT    │ 年龄  │\n" +
                "└─────────────────────────┘");

        TABLE_SCHEMAS.put("course",
                "┌─ course（课程表）────────┐\n" +
                "│ id (PK) │ BIGINT │ 课程ID│\n" +
                "│ name    │ VARCHAR│ 课程名│\n" +
                "│ teacher │ VARCHAR│ 授课老师│\n" +
                "└─────────────────────────┘");

        TABLE_SCHEMAS.put("student_course",
                "┌─ student_course（选课表）──┐\n" +
                "│ student_id│ BIGINT │ 学生ID │\n" +
                "│ course_id │ BIGINT │ 课程ID │\n" +
                "│ score     │ DECIMAL│ 成绩   │\n" +
                "│ PK: (student_id, course_id)│\n" +
                "└───────────────────────────┘");

        TABLE_SCHEMAS.put("login_log",
                "┌─ login_log（登录日志表）────┐\n" +
                "│ id (PK)   │ BIGINT │ 日志ID │\n" +
                "│ user_id   │ BIGINT │ 用户ID │\n" +
                "│ login_date│ DATE   │ 登录日期│\n" +
                "└────────────────────────────┘");

        TABLE_SCHEMAS.put("user",
                "┌─ user（用户表）────────────────┐\n" +
                "│ id (PK)    │ BIGINT │ 用户ID   │\n" +
                "│ username   │ VARCHAR│ 用户名   │\n" +
                "│ email      │ VARCHAR│ 邮箱     │\n" +
                "│ phone      │ VARCHAR│ 手机号   │\n" +
                "│ status     │ TINYINT│ 状态     │\n" +
                "│ create_time│ DATETIME│注册时间  │\n" +
                "└───────────────────────────────┘");

        TABLE_SCHEMAS.put("orders",
                "┌─ orders（订单表）───────────────────┐\n" +
                "│ id (PK)      │ BIGINT  │ 订单ID     │\n" +
                "│ user_id      │ BIGINT  │ 下单用户ID  │\n" +
                "│ order_no     │ VARCHAR │ 订单号      │\n" +
                "│ total_amount │ DECIMAL │ 订单总金额  │\n" +
                "│ status       │ TINYINT │ 状态        │\n" +
                "│ create_time  │ DATETIME│ 下单时间    │\n" +
                "└─────────────────────────────────────┘");

        TABLE_SCHEMAS.put("category",
                "┌─ category（商品分类表）──┐\n" +
                "│ id (PK) │ BIGINT │ 分类ID│\n" +
                "│ name    │ VARCHAR│ 分类名│\n" +
                "└─────────────────────────┘");

        TABLE_SCHEMAS.put("product",
                "┌─ product（商品表）──────────────┐\n" +
                "│ id (PK)     │ BIGINT │ 商品ID   │\n" +
                "│ name        │ VARCHAR│ 商品名   │\n" +
                "│ category_id │ BIGINT │ 分类ID   │\n" +
                "│ price       │ DECIMAL│ 单价     │\n" +
                "│ stock       │ INT    │ 库存     │\n" +
                "│ status      │ TINYINT│ 状态     │\n" +
                "└────────────────────────────────┘");

        TABLE_SCHEMAS.put("order_item",
                "┌─ order_item（订单明细表）┐\n" +
                "│ id (PK)   │ BIGINT │ 明细ID│\n" +
                "│ order_id  │ BIGINT │ 订单ID│\n" +
                "│ product_id│ BIGINT │ 商品ID│\n" +
                "│ quantity  │ INT    │ 数量  │\n" +
                "│ price     │ DECIMAL│ 成交单价│\n" +
                "└──────────────────────────┘");
    }

    // ================================================================
    // 枚举字段（raw data，description 通过 getDescription() 懒计算）
    // ================================================================
    private final Long id;
    private final String title;
    private final String difficulty;
    private final String knowledgePoint;
    private final String standardSql;

    /**
     * 期望输出列（含含义/来源说明）：
     * 格式为 "列名（含义，来源字段或计算式）"，
     * 题干中逐行展示，做题人可明确知道每一列对应哪个字段。
     * 注意：结果比对时要求 SELECT 输出的列名与括号外的列名完全一致（可用 AS 重命名）。
     */
    private final List<String> expectedColumns;

    /** description 的原始组成部分，getDescription() 时拼接表结构图 */
    private final List<String> tableKeys;
    private final String analysis;
    private final String knowledge;
    private final String interview;

    SqlCaseEnum(Long id, String title,
                List<String> tableKeys, String analysis, String knowledge, String interview,
                String difficulty, String knowledgePoint,
                List<String> expectedColumns, String standardSql) {
        this.id = id;
        this.title = title;
        this.tableKeys = tableKeys;
        this.analysis = analysis;
        this.knowledge = knowledge;
        this.interview = interview;
        this.difficulty = difficulty;
        this.knowledgePoint = knowledgePoint;
        this.expectedColumns = expectedColumns;
        this.standardSql = standardSql;
    }

    /**
     * 懒计算 description：拼接涉及的表结构图 + 分析/知识/面试表达 + 期望输出列（含来源说明）
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("【涉及表结构】\n");
        for (String key : tableKeys) {
            String schema = TABLE_SCHEMAS.get(key);
            if (schema != null) {
                sb.append(schema).append("\n");
            }
        }
        sb.append("\n【SQL分析】").append(analysis).append("\n");
        sb.append("【核心知识】").append(knowledge).append("\n");
        sb.append("【面试表达】").append(interview).append("\n");
        sb.append("【期望输出列】（括号内为含义/来源，输出列名需与括号外名称完全一致，可用 AS 重命名）\n");
        for (String col : expectedColumns) {
            sb.append("  ● ").append(col).append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取纯列名（去掉括号说明），用于程序化比对等场景。
     */
    public List<String> getExpectedColumnNames() {
        return expectedColumns.stream()
                .map(s -> s.substring(0, s.indexOf('（')).trim())
                .toList();
    }
}