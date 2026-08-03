package com.dev.springmybatissql.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析服务接口（经典35题迁移 + 现代业务SQL扩展）
 */
public interface AnalysisService {

    /** 题1：每个部门薪资最高的员工 */
    List<Map<String, Object>> topSalaryByDept();

    /** 题2：薪资高于部门平均薪资的员工 */
    List<Map<String, Object>> aboveDeptAvgSalary();

    /** 题3：部门薪资等级分布 */
    List<Map<String, Object>> deptSalaryGradeStats();

    /** 题5：不用 MAX 求最高薪资 */
    BigDecimal maxSalaryNoGroupFunc();

    /** 题6/7：平均薪资最高的部门 */
    List<Map<String, Object>> topAvgSalaryDept();

    /** 题10/11：薪资 Top N */
    List<Map<String, Object>> salaryTopN(int page, int size);

    /** 题12：最后入职 N 名员工 */
    List<Map<String, Object>> latestHire(int size);

    /** 题14：员工及领导姓名 */
    List<Map<String, Object>> employeeWithLeader();

    /** 题15：入职早于领导 */
    List<Map<String, Object>> earlierThanLeader();

    /** 题16：所有部门及员工 */
    List<Map<String, Object>> allDeptWithEmployee();

    /** 题17：员工数达到阈值的部门 */
    List<Map<String, Object>> deptWithMinEmployee(int minCount);

    /** 题19：指定职位员工与部门 */
    List<Map<String, Object>> positionWithDeptAndCount(String position);

    /** 题21：指定编码部门员工 */
    List<Map<String, Object>> employeeInDept(String deptCode);

    /** 题22：高于公司平均薪水的员工 */
    List<Map<String, Object>> aboveCompanyAvg();

    /** 题25：高于指定部门所有员工 */
    List<Map<String, Object>> higherThanAllOfDept(Long deptId);

    /** 题26：部门统计 */
    List<Map<String, Object>> deptStats();

    /** 题31：年工资排行 */
    List<Map<String, Object>> annualSalary();

    /** 阶段6：连续登录 N 天 */
    List<Map<String, Object>> continuousLogin(int days);

    /** 阶段6：分类销量 Top N */
    List<Map<String, Object>> categorySalesTopN(int topN);

    /** 阶段6：每月销售额 Top N */
    List<Map<String, Object>> monthlySalesTopN(int topN);

    /** 阶段6：薪资等级员工数 */
    List<Map<String, Object>> salaryGradeCount();

    /** 阶段6：订单状态分布 */
    List<Map<String, Object>> orderStatusStats();

    /** 阶段6：用户消费排行 */
    List<Map<String, Object>> userConsumeRank();
}