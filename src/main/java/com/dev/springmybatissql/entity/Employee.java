package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工实体
 *
 * 对应表：employee
 * 经典 emp 表迁移：empno->id, ename->name, mgr->leader_id,
 *                  sal->salary, job->position, hiredate->hire_date
 */
@Data
@TableName("employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工姓名 */
    private String name;

    /** 所属部门ID */
    private Long deptId;

    /** 直属领导ID，指向 employee.id（NULL 表示顶级领导） */
    private Long leaderId;

    /** 月薪 */
    private BigDecimal salary;

    /** 职位：CTO/技术总监/高级工程师/销售专员等 */
    private String position;

    /** 入职日期 */
    private LocalDate hireDate;
}