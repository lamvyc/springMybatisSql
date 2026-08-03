package com.dev.springmybatissql.service;

import com.dev.springmybatissql.entity.Employee;
import com.dev.springmybatissql.vo.EmployeeVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工服务接口
 */
public interface EmployeeService {

    /**
     * 条件查询员工（动态 SQL）
     */
    List<Employee> listByCondition(String name, Long deptId, BigDecimal minSalary, BigDecimal maxSalary);

    /**
     * 查询员工及其部门、领导信息（ResultMap 演示）
     */
    List<EmployeeVO> listWithDeptAndLeader();

    /**
     * 分页查询员工（原生 limit/offset）
     */
    List<Employee> listByPage(int page, int size);

    /**
     * 员工总数
     */
    long count();
}