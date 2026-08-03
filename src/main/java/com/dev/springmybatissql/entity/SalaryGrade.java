package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资等级实体
 *
 * 对应表：salary_grade
 * 经典 salgrade 表迁移：grade/losal/hisal -> grade/low_salary/high_salary
 */
@Data
@TableName("salary_grade")
public class SalaryGrade {

    @TableId(type = IdType.INPUT)
    private Integer grade;

    /** 该等级最低薪资 */
    private BigDecimal lowSalary;

    /** 该等级最高薪资 */
    private BigDecimal highSalary;
}