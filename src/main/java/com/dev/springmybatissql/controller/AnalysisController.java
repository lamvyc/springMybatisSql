package com.dev.springmybatissql.controller;

import com.dev.springmybatissql.common.Result;
import com.dev.springmybatissql.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析 Controller（经典35题迁移 + 现代业务SQL扩展）
 *
 * 每道题对应一个 REST 接口，方便通过浏览器 / Postman 直接调用查看结果。
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    /** 题1：每个部门薪资最高的员工 */
    @GetMapping("/top-salary-by-dept")
    public Result<List<Map<String, Object>>> topSalaryByDept() {
        return Result.ok(analysisService.topSalaryByDept());
    }

    /** 题2：薪资高于部门平均薪资的员工 */
    @GetMapping("/above-dept-avg")
    public Result<List<Map<String, Object>>> aboveDeptAvgSalary() {
        return Result.ok(analysisService.aboveDeptAvgSalary());
    }

    /** 题3：部门薪资等级分布 */
    @GetMapping("/dept-salary-grade-stats")
    public Result<List<Map<String, Object>>> deptSalaryGradeStats() {
        return Result.ok(analysisService.deptSalaryGradeStats());
    }

    /** 题5：不用 MAX 求最高薪资 */
    @GetMapping("/max-salary-no-group-func")
    public Result<BigDecimal> maxSalaryNoGroupFunc() {
        return Result.ok(analysisService.maxSalaryNoGroupFunc());
    }

    /** 题6/7：平均薪资最高的部门 */
    @GetMapping("/top-avg-salary-dept")
    public Result<List<Map<String, Object>>> topAvgSalaryDept() {
        return Result.ok(analysisService.topAvgSalaryDept());
    }

    /** 题10/11：薪资 Top N（默认前5名；page=2 即第6~10名） */
    @GetMapping("/salary-top-n")
    public Result<List<Map<String, Object>>> salaryTopN(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.ok(analysisService.salaryTopN(page, size));
    }

    /** 题12：最后入职 N 名员工 */
    @GetMapping("/latest-hire")
    public Result<List<Map<String, Object>>> latestHire(
            @RequestParam(defaultValue = "5") int size) {
        return Result.ok(analysisService.latestHire(size));
    }

    /** 题14：所有员工及领导姓名 */
    @GetMapping("/employee-with-leader")
    public Result<List<Map<String, Object>>> employeeWithLeader() {
        return Result.ok(analysisService.employeeWithLeader());
    }

    /** 题15：入职早于直接上级的员工 */
    @GetMapping("/earlier-than-leader")
    public Result<List<Map<String, Object>>> earlierThanLeader() {
        return Result.ok(analysisService.earlierThanLeader());
    }

    /** 题16：所有部门及员工信息（含无员工部门） */
    @GetMapping("/all-dept-with-employee")
    public Result<List<Map<String, Object>>> allDeptWithEmployee() {
        return Result.ok(analysisService.allDeptWithEmployee());
    }

    /** 题17：员工数达到阈值的部门 */
    @GetMapping("/dept-with-min-employee")
    public Result<List<Map<String, Object>>> deptWithMinEmployee(
            @RequestParam(defaultValue = "5") int minCount) {
        return Result.ok(analysisService.deptWithMinEmployee(minCount));
    }

    /** 题19：指定职位员工与部门人数 */
    @GetMapping("/position-with-dept")
    public Result<List<Map<String, Object>>> positionWithDeptAndCount(
            @RequestParam(defaultValue = "高级工程师") String position) {
        return Result.ok(analysisService.positionWithDeptAndCount(position));
    }

    /** 题21：指定编码部门员工（如 SALES） */
    @GetMapping("/employee-in-dept")
    public Result<List<Map<String, Object>>> employeeInDept(
            @RequestParam(defaultValue = "SALES") String deptCode) {
        return Result.ok(analysisService.employeeInDept(deptCode));
    }

    /** 题22：高于公司平均薪水的员工 */
    @GetMapping("/above-company-avg")
    public Result<List<Map<String, Object>>> aboveCompanyAvg() {
        return Result.ok(analysisService.aboveCompanyAvg());
    }

    /** 题25：高于指定部门所有员工薪资的员工 */
    @GetMapping("/higher-than-all-of-dept")
    public Result<List<Map<String, Object>>> higherThanAllOfDept(
            @RequestParam(defaultValue = "2") Long deptId) {
        return Result.ok(analysisService.higherThanAllOfDept(deptId));
    }

    /** 题26：每个部门员工数、平均薪资、平均服务年限 */
    @GetMapping("/dept-stats")
    public Result<List<Map<String, Object>>> deptStats() {
        return Result.ok(analysisService.deptStats());
    }

    /** 题31：年工资排行 */
    @GetMapping("/annual-salary")
    public Result<List<Map<String, Object>>> annualSalary() {
        return Result.ok(analysisService.annualSalary());
    }

    /** 阶段6：连续登录 N 天用户 */
    @GetMapping("/continuous-login")
    public Result<List<Map<String, Object>>> continuousLogin(
            @RequestParam(defaultValue = "3") int days) {
        return Result.ok(analysisService.continuousLogin(days));
    }

    /** 阶段6：每个分类销量 Top N 商品 */
    @GetMapping("/category-sales-top-n")
    public Result<List<Map<String, Object>>> categorySalesTopN(
            @RequestParam(defaultValue = "3") int topN) {
        return Result.ok(analysisService.categorySalesTopN(topN));
    }

    /** 阶段6：每月销售额 Top N 商品 */
    @GetMapping("/monthly-sales-top-n")
    public Result<List<Map<String, Object>>> monthlySalesTopN(
            @RequestParam(defaultValue = "3") int topN) {
        return Result.ok(analysisService.monthlySalesTopN(topN));
    }

    /** 阶段6：各薪资等级员工数 */
    @GetMapping("/salary-grade-count")
    public Result<List<Map<String, Object>>> salaryGradeCount() {
        return Result.ok(analysisService.salaryGradeCount());
    }

    /** 阶段6：订单状态分布 */
    @GetMapping("/order-status-stats")
    public Result<List<Map<String, Object>>> orderStatusStats() {
        return Result.ok(analysisService.orderStatusStats());
    }

    /** 阶段6：用户消费排行 */
    @GetMapping("/user-consume-rank")
    public Result<List<Map<String, Object>>> userConsumeRank() {
        return Result.ok(analysisService.userConsumeRank());
    }
}
