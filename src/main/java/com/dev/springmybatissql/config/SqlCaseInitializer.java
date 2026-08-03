package com.dev.springmybatissql.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dev.springmybatissql.entity.SqlCase;
import com.dev.springmybatissql.mapper.SqlCaseMapper;
import com.dev.springmybatissql.mapper.SqlExecutorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 实验中心题目初始化器
 *
 * 作用：应用启动后，自动将经典35题迁移 + 现代业务SQL扩展写入 sql_case 表。
 * 关键设计：真实执行标准 SQL，把查询结果序列化为 expected_result(JSON) 存入数据库，
 * 这样用户在实验中心提交自己的 SQL 时，系统才能自动比对结果是否正确。
 *
 * 幂等设计：sql_case 表已有数据则跳过，不重复插入。
 */
@Slf4j
@Component
public class SqlCaseInitializer implements ApplicationRunner {

    @Autowired
    private SqlCaseMapper sqlCaseMapper;

    @Autowired
    private SqlExecutorMapper sqlExecutorMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long count = sqlCaseMapper.selectCount(new QueryWrapper<>());
        if (count != null && count > 0) {
            log.info("SQL 实验中心：sql_case 已有 {} 道题，跳过初始化", count);
            return;
        }

        List<SqlCase> cases = buildCases();
        for (SqlCase sqlCase : cases) {
            // 真实执行标准 SQL，生成 expected_result
            try {
                List<Map<String, Object>> rows = sqlExecutorMapper.executeSelect(sqlCase.getStandardSql());
                sqlCase.setExpectedResult(buildExpectedJson(rows));
                sqlCaseMapper.insert(sqlCase);
            } catch (Exception e) {
                log.error("初始化题目失败 id={} title={} 原因={}", sqlCase.getId(), sqlCase.getTitle(), e.getMessage());
            }
        }
        log.info("SQL 实验中心：成功初始化 {} 道题目", cases.size());
    }

    /**
     * 将查询结果转换为 {"columns":[...],"rows":[[...]]} JSON
     */
    private String buildExpectedJson(List<Map<String, Object>> rows) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }
        result.put("columns", columns);

        List<List<Object>> dataRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> dataRow = new ArrayList<>();
            for (String col : columns) {
                dataRow.add(row.get(col));
            }
            dataRows.add(dataRow);
        }
        result.put("rows", dataRows);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 内置题目定义（经典35题迁移 + 现代业务SQL扩展）
     */
    private List<SqlCase> buildCases() {
        List<SqlCase> list = new ArrayList<>();

        // ==================== 经典35题迁移（对应 AnalysisController 接口） ====================
        list.add(caseOf(1L, "每个部门薪资最高的员工（经典第1题）", "【数据模型】employee、department\n【SQL分析】第一步：按部门分组用MAX求最高薪资；第二步：员工表JOIN该结果匹配部门+薪资\n【核心知识】group by、max、join\n【面试表达】先聚合得到每部门最高薪资，再关联员工表取出对应员工；注意同一薪资多人会返回多行",
                "入门", "group by/join", "SELECT e.name AS employee_name, d.dept_name, e.salary FROM employee e JOIN department d ON e.dept_id = d.id JOIN (SELECT dept_id, MAX(salary) AS max_salary FROM employee GROUP BY dept_id) m ON e.dept_id = m.dept_id AND e.salary = m.max_salary ORDER BY d.id"));

        list.add(caseOf(2L, "薪资高于部门平均薪资的员工（经典第2题）", "【数据模型】employee、department\n【SQL分析】第一步：按部门分组求平均薪资；第二步：员工表JOIN该结果，WHERE过滤 salary大于平均\n【核心知识】join、group by、子查询\n【面试表达】关联子查询是经典做法，先算部门均值再比较",
                "入门", "join/子查询", "SELECT e.name AS employee_name, d.dept_name, e.salary, ROUND(a.avg_salary, 2) AS dept_avg_salary FROM employee e JOIN department d ON e.dept_id = d.id JOIN (SELECT dept_id, AVG(salary) AS avg_salary FROM employee GROUP BY dept_id) a ON e.dept_id = a.dept_id WHERE e.salary > a.avg_salary ORDER BY d.id, e.salary DESC"));

        list.add(caseOf(3L, "部门平均薪资等级统计（经典第3/13题）", "【数据模型】employee、department、salary_grade\n【SQL分析】第一步：员工薪资与薪资等级表做非等值连接（BETWEEN）；第二步：按部门+等级分组统计人数与平均薪资\n【核心知识】非等值连接、group by\n【面试表达】薪资区间匹配必须用 BETWEEN 范围连接，不能用等值连接",
                "进阶", "非等值连接", "SELECT d.dept_name, sg.grade, COUNT(e.id) AS employee_count, ROUND(AVG(e.salary), 2) AS avg_salary FROM salary_grade sg LEFT JOIN employee e ON e.salary BETWEEN sg.low_salary AND sg.high_salary LEFT JOIN department d ON e.dept_id = d.id GROUP BY d.dept_name, sg.grade ORDER BY d.dept_name, sg.grade"));

        list.add(caseOf(5L, "不用MAX函数求最高薪资（经典第5题）", "【数据模型】employee\n【SQL分析】方案一：ORDER BY salary DESC LIMIT 1；方案二：自连接或子查询取最大的 salary\n【核心知识】order by、limit、替代方案\n【面试表达】面试常问两种解法：排序取第一条、或者用 NOT EXISTS 找没有比自己更高的员工",
                "进阶", "order by/limit", "SELECT salary FROM employee ORDER BY salary DESC LIMIT 1"));

        list.add(caseOf(8L, "平均薪资最高的部门（经典第6/7题）", "【数据模型】employee、department\n【SQL分析】第一步：JOIN部门按部门分组求平均薪资；第二步：ORDER BY平均薪资DESC LIMIT 1\n【核心知识】group by、order by、limit\n【面试表达】分组聚合后排序取第一条即可；若要并列第一需用窗口函数",
                "入门", "group by/limit", "SELECT d.id AS dept_id, d.dept_name, ROUND(AVG(e.salary), 2) AS avg_salary FROM employee e JOIN department d ON e.dept_id = d.id GROUP BY d.id, d.dept_name ORDER BY AVG(e.salary) DESC LIMIT 1"));

        list.add(caseOf(10L, "薪资最高前5名 / 第6~10名（经典第10/11题）", "【数据模型】employee\n【SQL分析】ORDER BY salary DESC + LIMIT offset,size；前5: LIMIT 0,5；第6~10: LIMIT 5,5\n【核心知识】order by、limit offset\n【面试表达】全局TopN用LIMIT即可；分组TopN必须用窗口函数",
                "入门", "limit/offset", "SELECT name AS employee_name, salary, position FROM employee ORDER BY salary DESC LIMIT 0, 5"));

        list.add(caseOf(15L, "入职早于直接上级的员工（经典第15题）", "【数据模型】employee（自连接）\n【SQL分析】第一步：employee自连接 员工.leader_id=领导.id；第二步：WHERE 员工入职日期 < 领导入职日期\n【核心知识】自连接\n【面试表达】员工表存领导ID即可自连接查出领导信息，注意区分别名",
                "进阶", "自连接", "SELECT e.id AS employee_id, e.name AS employee_name, e.hire_date AS employee_hire_date, d.dept_name, l.name AS leader_name, l.hire_date AS leader_hire_date FROM employee e JOIN employee l ON e.leader_id = l.id JOIN department d ON e.dept_id = d.id WHERE e.hire_date < l.hire_date"));

        list.add(caseOf(16L, "所有部门及员工信息（含无员工部门，经典第16题）", "【数据模型】department、employee\n【SQL分析】department 作为驱动表 LEFT JOIN employee，保留没有员工的部门\n【核心知识】left join、驱动表方向\n【面试表达】需要保留主表全部记录时，一定要让主表在 LEFT JOIN 左侧",
                "入门", "left join", "SELECT d.id AS dept_id, d.dept_name, e.id AS employee_id, e.name AS employee_name, e.salary FROM department d LEFT JOIN employee e ON e.dept_id = d.id ORDER BY d.id, e.id"));

        list.add(caseOf(17L, "员工数不少于阈值的部门（经典第17题）", "【数据模型】department、employee\n【SQL分析】按部门分组后 HAVING COUNT 过滤；HAVING用于分组后条件\n【核心知识】having、count\n【面试表达】WHERE 过滤行，HAVING 过滤分组，这是关键区别",
                "入门", "having", "SELECT d.id AS dept_id, d.dept_name, COUNT(e.id) AS employee_count FROM department d JOIN employee e ON e.dept_id = d.id GROUP BY d.id, d.dept_name HAVING COUNT(e.id) >= 5 ORDER BY employee_count DESC"));

        list.add(caseOf(21L, "指定编码部门员工名单（经典第21题）", "【数据模型】employee、department\n【SQL分析】通过 dept_code='SALES' 定位部门，再JOIN员工；业务上不直接暴露部门ID\n【核心知识】join、业务编码设计\n【面试表达】用业务编码而非物理主键做跨系统关联是常见设计",
                "入门", "join", "SELECT e.name AS employee_name, e.salary, d.dept_name FROM employee e JOIN department d ON e.dept_id = d.id WHERE d.dept_code = 'SALES' ORDER BY e.salary DESC"));

        list.add(caseOf(22L, "高于公司平均薪资的员工及部门/领导/等级（经典第22题）", "【数据模型】employee、department、salary_grade\n【SQL分析】三步：①子查询求公司平均薪资 ②三表JOIN ③薪资等级非等值连接\n【核心知识】子查询、多表join、非等值连接\n【面试表达】综合题考察多表关联的熟练度，注意 JOIN 顺序不影响结果只影响性能",
                "困难", "多表join/子查询", "SELECT e.name AS employee_name, d.dept_name, l.name AS leader_name, sg.grade AS salary_grade, e.salary FROM employee e JOIN department d ON e.dept_id = d.id LEFT JOIN employee l ON e.leader_id = l.id JOIN salary_grade sg ON e.salary BETWEEN sg.low_salary AND sg.high_salary WHERE e.salary > (SELECT AVG(salary) FROM employee) ORDER BY e.salary DESC"));

        list.add(caseOf(25L, "薪资高于某部门所有员工的人（经典第25题）", "【数据模型】employee、department\n【SQL分析】salary > ALL(子查询某部门全部薪资)\n【核心知识】all关键字、子查询\n【面试表达】ALL 等价于大于子查询最大值；也可用 MAX 子查询替换",
                "困难", "all/子查询", "SELECT e.name AS employee_name, e.salary, d.dept_name FROM employee e JOIN department d ON e.dept_id = d.id WHERE d.id != 2 AND e.salary > ALL (SELECT salary FROM employee WHERE dept_id = 2 AND salary IS NOT NULL) ORDER BY e.salary"));

        list.add(caseOf(26L, "每个部门员工数/平均薪资/平均服务年限（经典第26题）", "【数据模型】department、employee\n【SQL分析】LEFT JOIN + GROUP BY；用 TIMESTAMPDIFF 计算服务年限\n【核心知识】group by、日期函数\n【面试表达】服务年限计算注意用 YEAR 还是按天/365，业务口径要一致",
                "进阶", "日期函数/group by", "SELECT d.id AS dept_id, d.dept_name, COUNT(e.id) AS employee_count, ROUND(AVG(e.salary), 2) AS avg_salary, ROUND(AVG(TIMESTAMPDIFF(YEAR, e.hire_date, CURDATE())), 1) AS avg_service_years FROM department d LEFT JOIN employee e ON e.dept_id = d.id GROUP BY d.id, d.dept_name ORDER BY d.id"));

        list.add(caseOf(31L, "员工年工资排行（经典第31题）", "【数据模型】employee\n【SQL分析】salary*13 模拟含13薪的年薪，ORDER BY 排序\n【核心知识】表达式、order by\n【面试表达】排序字段可以是计算列，注意别省略 ORDER BY 断言稳定性",
                "入门", "表达式排序", "SELECT name AS employee_name, salary AS month_salary, salary * 13 AS annual_salary FROM employee ORDER BY annual_salary"));

        // ==================== 学生选课综合（经典第35题） ====================
        list.add(caseOf(35L, "没选过某老师课程的学生（经典35题-1）", "【数据模型】student、course、student_course\n【SQL分析】NOT EXISTS 子查询：找出不存在选课记录的学生\n【核心知识】not exists、多对多\n【面试表达】NOT EXISTS 比 NOT IN 更安全（后者遇到NULL会出问题）",
                "进阶", "not exists", "SELECT s.name AS student_name FROM student s WHERE NOT EXISTS (SELECT 1 FROM student_course sc JOIN course c ON sc.course_id = c.id WHERE sc.student_id = s.id AND c.teacher = '黎明') ORDER BY s.id"));

        list.add(caseOf(36L, "2门以上不及格学生姓名及平均（经典35题-2）", "【数据模型】student、student_course\n【SQL分析】HAVING SUM(score<60)>=2 过滤不及格门数，AVG求平均成绩\n【核心知识】having、条件聚合\n【面试表达】SUM(score<60) 利用布尔表达式统计满足条件的行数",
                "困难", "having/条件聚合", "SELECT s.name AS student_name, ROUND(AVG(sc.score), 2) AS avg_score, SUM(CASE WHEN sc.score < 60 THEN 1 ELSE 0 END) AS fail_count FROM student s JOIN student_course sc ON s.id = sc.student_id GROUP BY s.id, s.name HAVING SUM(CASE WHEN sc.score < 60 THEN 1 ELSE 0 END) >= 2 ORDER BY avg_score"));

        list.add(caseOf(37L, "既学课程1又学课程2的学生（经典35题-3）", "【数据模型】student、student_course\n【SQL分析】同表两次JOIN，分别关联课程1和课程2\n【核心知识】多表join、多对多\n【面试表达】同一张关联表需用两次JOIN时，必须使用不同别名",
                "进阶", "多次join", "SELECT s.name AS student_name FROM student s JOIN student_course sc1 ON s.id = sc1.student_id AND sc1.course_id = 1 JOIN student_course sc2 ON s.id = sc2.student_id AND sc2.course_id = 2 GROUP BY s.id, s.name ORDER BY s.id"));

        // ==================== 阶段6 现代业务SQL扩展 ====================
        list.add(caseOf(40L, "连续登录N天的用户（现代业务-连续问题）", "【数据模型】login_log\n【SQL分析】窗口函数差集法：①ROW_NUMBER按用户分区日期排序得序号 ②日期-序号得到分组标识 ③按用户+分组聚合，HAVING>=N\n【核心知识】row_number、日期差值、连续问题\n【面试表达】连续问题通用解法：日期减去行号，连续日期差值相同，经典面试题",
                "困难", "窗口函数/连续问题", "SELECT user_id, MIN(login_date) AS start_date, MAX(login_date) AS end_date, COUNT(*) AS continuous_days FROM (SELECT user_id, login_date, DATE_SUB(login_date, INTERVAL ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) DAY) AS grp FROM login_log) t GROUP BY user_id, grp HAVING COUNT(*) >= 3 ORDER BY user_id"));

        list.add(caseOf(41L, "每个分类销量Top3商品（现代业务-分组TopN）", "【数据模型】category、product、order_item\n【SQL分析】①聚合求出各商品销量 ②ROW_NUMBER按分类分区、销量排序 ③外层过滤rn<=3\n【核心知识】row_number、分组TopN\n【面试表达】为什么LIMIT不行？LIMIT只能解决全局TopN，分组TopN必须用窗口函数",
                "困难", "窗口函数/分组TopN", "SELECT category_name, product_name, total_quantity, rn FROM (SELECT c.name AS category_name, p.name AS product_name, SUM(oi.quantity) AS total_quantity, ROW_NUMBER() OVER (PARTITION BY c.id ORDER BY SUM(oi.quantity) DESC) AS rn FROM product p JOIN category c ON p.category_id = c.id LEFT JOIN order_item oi ON p.id = oi.product_id GROUP BY c.id, c.name, p.id, p.name) t WHERE rn <= 3 ORDER BY category_name, rn"));

        list.add(caseOf(43L, "每月销售额Top3商品（现代业务-月度排行）", "【数据模型】orders、order_item、product\n【SQL分析】DATE_FORMAT取月份 + ROW_NUMBER按月分区排序\n【核心知识】日期格式化、窗口函数\n【面试表达】月度/周度排行都是按时间维度分区做窗口排序",
                "困难", "窗口函数/时间维度", "SELECT sale_month, product_name, sale_amount, rn FROM (SELECT DATE_FORMAT(o.create_time, '%Y-%m') AS sale_month, p.name AS product_name, SUM(oi.quantity * oi.price) AS sale_amount, ROW_NUMBER() OVER (PARTITION BY DATE_FORMAT(o.create_time, '%Y-%m') ORDER BY SUM(oi.quantity * oi.price) DESC) AS rn FROM order_item oi JOIN `orders` o ON oi.order_id = o.id JOIN product p ON oi.product_id = p.id GROUP BY sale_month, p.id, p.name) t WHERE rn <= 3 ORDER BY sale_month DESC, rn"));

        list.add(caseOf(45L, "订单状态分布统计（现代业务-行列转换）", "【数据模型】orders\n【SQL分析】CASE WHEN 条件聚合：把多个状态值转成多列\n【核心知识】case when、条件聚合\n【面试表达】行列转换的核心技巧是用 CASE WHEN + SUM/COUNT 把行转成列",
                "进阶", "case when/行列转换", "SELECT COUNT(CASE WHEN status = 0 THEN 1 END) AS pending_payment, COUNT(CASE WHEN status = 1 THEN 1 END) AS paid, COUNT(CASE WHEN status = 2 THEN 1 END) AS shipped, COUNT(CASE WHEN status = 3 THEN 1 END) AS completed, COUNT(CASE WHEN status = 4 THEN 1 END) AS cancelled, COUNT(*) AS total FROM `orders`"));

        list.add(caseOf(46L, "用户消费金额排行（现代业务-rank并列排名）", "【数据模型】user、orders\n【SQL分析】①按用户聚合消费总额 ②rank()窗口函数按金额排序，相同金额并列\n【核心知识】rank、窗口函数\n【面试表达】rank并列则跳号、dense_rank不跳号、row_number不并列，三者的区别是高频考点",
                "进阶", "rank/窗口函数", "SELECT u.username, COALESCE(c.total_amount, 0) AS total_amount, rank() OVER (ORDER BY COALESCE(c.total_amount, 0) DESC) AS rank_no FROM `user` u LEFT JOIN (SELECT user_id, SUM(total_amount) AS total_amount FROM `orders` WHERE status IN (1, 2, 3) GROUP BY user_id) c ON u.id = c.user_id ORDER BY rank_no"));

        return list;
    }

    /**
     * 构造题目对象
     */
    private SqlCase caseOf(Long id, String title, String description, String difficulty,
                           String knowledgePoint, String standardSql) {
        SqlCase sqlCase = new SqlCase();
        sqlCase.setId(id);
        sqlCase.setTitle(title);
        sqlCase.setDescription(description);
        sqlCase.setDifficulty(difficulty);
        sqlCase.setKnowledgePoint(knowledgePoint);
        sqlCase.setStandardSql(standardSql);
        return sqlCase;
    }
}
