package com.dev.springmybatissql.mapper;

import com.dev.springmybatissql.entity.Employee;
import com.dev.springmybatissql.vo.EmployeeVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工 Mapper（原生 MyBatis 方式）
 *
 * 说明：本接口的 SQL 全部定义在 resources/mapper/EmployeeMapper.xml 中。
 * 这是"原生 MyBatis"的典型用法：接口定义方法，XML 编写 SQL。
 *
 * 学习重点：
 * 1. 参数映射：#{} 预编译占位（防止SQL注入）
 * 2. 动态 SQL：<if>、<where> 组合条件查询
 * 3. ResultMap：多表联查时自定义字段映射
 */
public interface EmployeeMapper {

    /**
     * 条件查询员工列表（动态 SQL 演示）
     * 支持按 姓名模糊、部门ID、薪资下限、薪资上限 组合过滤
     */
    List<Employee> selectByCondition(@Param("name") String name,
                                     @Param("deptId") Long deptId,
                                     @Param("minSalary") BigDecimal minSalary,
                                     @Param("maxSalary") BigDecimal maxSalary);

    /**
     * 查询员工及其部门名称、领导姓名（ResultMap 演示）
     */
    List<EmployeeVO> selectEmployeeWithDeptAndLeader();

    /**
     * 按薪资降序分页查询员工（limit/offset 分页演示）
     */
    List<Employee> selectByPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 统计员工总数
     */
    long countAll();
}
