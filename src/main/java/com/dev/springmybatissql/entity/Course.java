package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程实体
 *
 * 对应表：course（经典第35题 C 表迁移）
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    private String name;

    /** 授课老师 */
    private String teacher;
}