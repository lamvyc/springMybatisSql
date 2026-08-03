package com.dev.springmybatissql.mapper;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析 Mapper（经典35题迁移 + 现代业务SQL扩展）
 *
 * 全部使用原生 MyBatis（XML 定义 SQL），充分练习：
 * 多表 JOIN、GROUP BY、HAVING、子查询、窗口函数、CASE WHEN
 */
public interface AnalysisMapper {

    // ==================== 经典35题迁移（核心示例） ====================

    /**
     * 题1：每个部门薪资最高的员工（去重+保留一个）
     * 经典：取得每个部门最高薪水的人员名称
     */
    List<Map<String, Object>> topSalaryByDept();

    /**
     * 题2：薪资高于所在部门平均薪资的员工
     * 经典：哪些人的薪水在部门的平均薪水之上
     */
    List<Map<String, Object>> aboveDeptAvgSalary();

    /**
     * 题3：每个部门薪资等级的分布（员工数、平均薪资）
     * 经典：取得每个部门平均薪水的等级 / 每个薪水等级有多少员工
     */
    List<Map<String, Object>> deptSalaryGradeStats();

    /**
     * 题5：不用 MAX 函数求最高薪资（两种方案见 XML）
     * 经典：不准用组函数（Max），取得最高薪水
     */
    BigDecimal maxSalaryNoGroupFunc();

    /**
     * 题6/7：平均薪资最高的部门（子查询 + 排序取一）
     * 经典：取得平均薪水最高的部门的部门编号/名称
     */
    List<Map<String, Object>> topAvgSalaryDept();

    /**
     * 题10/11：薪资最高的前 5 名 / 第 6~10 名（Top N）
     * 经典：取得薪水最高的前五名员工 / 第六到第十名
     */
    List<Map<String, Object>> salaryTopN(@Param("offset") int offset, @Param("size") int size);

    /**
     * 题12：最后入职的 N 名员工
     * 经典：取得最后入职的5名员工
     */
    List<Map<String, Object>> latestHire(@Param("size") int size);

    /**
     * 题14：所有员工及领导姓名（自连接）
     * 经典：列出所有员工及领导的姓名
     */
    List<Map<String, Object>> employeeWithLeader();

    /**
     * 题15：入职早于直接上级的员工
     * 经典：列出受雇日期早于其直接上级的所有员工
     */
    List<Map<String, Object>> earlierThanLeader();

    /**
     * 题16：所有部门及员工信息（含无员工部门，驱动表 department）
     * 经典：列出部门名称和这些部门的员工信息，同时列出没有员工的部门
     */
    List<Map<String, Object>> allDeptWithEmployee();

    /**
     * 题17：员工数 >= 5 的部门
     * 经典：列出至少有5个员工的所有部门
     */
    List<Map<String, Object>> deptWithMinEmployee(@Param("minCount") int minCount);

    /**
     * 题19：指定职位的员工姓名、部门名称、部门人数
     * 经典：列出所有 CLERK 的姓名及其部门名称、部门人数
     */
    List<Map<String, Object>> positionWithDeptAndCount(@Param("position") String position);

    /**
     * 题21：销售部员工名单（不直接知道部门编号）
     * 经典：列出在部门 SALES 工作的员工
     */
    List<Map<String, Object>> employeeInDept(@Param("deptCode") String deptCode);

    /**
     * 题22/23：薪资高于公司平均的员工 + 所在部门/领导/薪资等级
     * 经典：薪金高于公司平均薪金的所有员工
     */
    List<Map<String, Object>> aboveCompanyAvg();

    /**
     * 题25：薪资高于指定部门全体员工薪资的员工（ALL 关键字）
     * 经典：薪金高于部门30中所有员工薪金的员工
     */
    List<Map<String, Object>> higherThanAllOfDept(@Param("deptId") Long deptId);

    /**
     * 题26：每个部门员工数、平均薪资、平均服务年限
     * 经典：每个部门工作的员工数量、平均工资和平均服务期限
     */
    List<Map<String, Object>> deptStats();

    /**
     * 题31：员工年工资（按月薪13薪模拟），按年薪排序
     * 经典：列出所有员工的年工资，按年薪从低到高排序
     */
    List<Map<String, Object>> annualSalary();

    // ==================== 阶段6：现代业务SQL扩展 ====================

    /**
     * 连续登录 N 天用户（窗口函数差集法）
     * 思路：对每个用户按日期排序生成序号，将 日期-序号 分组，
     *       同一分组说明日期连续
     */
    List<Map<String, Object>> continuousLogin(@Param("days") int days);

    /**
     * 每个分类销量 Top N 商品（row_number 分组TopN）
     */
    List<Map<String, Object>> categorySalesTopN(@Param("topN") int topN);

    /**
     * 每月销售额 Top N 商品
     */
    List<Map<String, Object>> monthlySalesTopN(@Param("topN") int topN);

    /**
     * 各薪资等级员工数（CASE WHEN / JOIN 两种思路）
     */
    List<Map<String, Object>> salaryGradeCount();

    /**
     * 订单状态分布统计（CASE WHEN 行列转换）
     */
    List<Map<String, Object>> orderStatusStats();

    /**
     * 用户消费总金额排行（rank 并列排名）
     */
    List<Map<String, Object>> userConsumeRank();
}
