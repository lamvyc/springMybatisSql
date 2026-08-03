package com.dev.springmybatissql.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工视图对象（含部门名称、领导姓名）
 *
 * 用于多表联查：employee LEFT JOIN department LEFT JOIN employee(leader)
 */
@Data
public class EmployeeVO {

    /** 员工ID */
    private Long id;

    /** 员工姓名 */
    private String name;

    /** 所属部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 领导ID */
    private Long leaderId;

    /** 领导姓名 */
    private String leaderName;

    /** 月薪 */
    private BigDecimal salary;

    /** 职位 */
    private String position;

    /** 入职日期 */
    private LocalDate hireDate;
}