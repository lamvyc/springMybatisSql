package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生实体
 *
 * 对应表：student（经典第35题 S 表迁移）
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生姓名 */
    private String name;

    /** 年龄 */
    private Integer age;
}