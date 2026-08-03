package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 选课实体
 *
 * 对应表：student_course（经典第35题 SC 表迁移）
 * 联合主键：student_id + course_id
 */
@Data
@TableName("student_course")
public class StudentCourse {

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 成绩 */
    private BigDecimal score;
}