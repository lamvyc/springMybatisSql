package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 部门实体
 *
 * 对应表：department
 * 经典 dept 表迁移：deptno->id, dname->dept_name, loc->parent_id
 */
@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 部门名称 */
    private String deptName;

    /** 部门编码（如 TECH/MKT/SALES，用于字符匹配类题目） */
    private String deptCode;

    /** 上级部门ID，0表示顶级 */
    private Long parentId;
}