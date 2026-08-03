package com.dev.springmybatissql.controller;

import com.dev.springmybatissql.common.Result;
import com.dev.springmybatissql.entity.Employee;
import com.dev.springmybatissql.service.EmployeeService;
import com.dev.springmybatissql.vo.EmployeeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工 Controller（阶段4：MyBatis 完整调用链演示）
 *
 * URL 示例：
 * - 条件查询：GET /api/employees?name=张&minSalary=4000
 * - 部门领导联查：GET /api/employees/with-dept-leader
 * - 分页查询：GET /api/employees/page?page=1&size=5
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 条件查询员工（动态 SQL）
     */
    @GetMapping
    public Result<List<Employee>> list(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) Long deptId,
                                       @RequestParam(required = false) BigDecimal minSalary,
                                       @RequestParam(required = false) BigDecimal maxSalary) {
        return Result.ok(employeeService.listByCondition(name, deptId, minSalary, maxSalary));
    }

    /**
     * 员工 + 部门 + 领导（ResultMap 多表联查演示）
     */
    @GetMapping("/with-dept-leader")
    public Result<List<EmployeeVO>> listWithDeptAndLeader() {
        return Result.ok(employeeService.listWithDeptAndLeader());
    }

    /**
     * 分页查询员工（limit/offset 分页演示）
     */
    @GetMapping("/page")
    public Result<List<Employee>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        return Result.ok(employeeService.listByPage(page, size));
    }

    /**
     * 员工总数
     */
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(employeeService.count());
    }
}
