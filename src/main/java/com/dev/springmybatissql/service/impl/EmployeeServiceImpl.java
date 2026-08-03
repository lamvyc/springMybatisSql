package com.dev.springmybatissql.service.impl;

import com.dev.springmybatissql.entity.Employee;
import com.dev.springmybatissql.mapper.EmployeeMapper;
import com.dev.springmybatissql.service.EmployeeService;
import com.dev.springmybatissql.vo.EmployeeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工服务实现
 *
 * 学习重点（完整调用链）：
 * Controller -> Service -> EmployeeMapper(接口) -> EmployeeMapper.xml(SQL) -> MySQL
 *
 * Service 层职责：
 * 1. 业务逻辑编排（校验、计算、组装）
 * 2. 调用 Mapper 访问数据库
 * 3. 事务管理（本示例为查询，无事务；写操作可加 @Transactional）
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public List<Employee> listByCondition(String name, Long deptId, BigDecimal minSalary, BigDecimal maxSalary) {
        // 业务校验：薪资区间下限不能大于上限
        if (minSalary != null && maxSalary != null && minSalary.compareTo(maxSalary) > 0) {
            throw new IllegalArgumentException("最低薪资不能大于最高薪资");
        }
        return employeeMapper.selectByCondition(name, deptId, minSalary, maxSalary);
    }

    @Override
    public List<EmployeeVO> listWithDeptAndLeader() {
        return employeeMapper.selectEmployeeWithDeptAndLeader();
    }

    @Override
    public List<Employee> listByPage(int page, int size) {
        // page 从 1 开始，转换为 offset = (page - 1) * size
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }
        int offset = (page - 1) * size;
        return employeeMapper.selectByPage(offset, size);
    }

    @Override
    public long count() {
        return employeeMapper.countAll();
    }
}