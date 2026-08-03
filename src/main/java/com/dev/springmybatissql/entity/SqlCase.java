package com.dev.springmybatissql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SQL 实验题目实体
 *
 * 对应表：sql_case
 * standardSql 保存标准答案；expectedResult 保存正确结果(JSON)
 */
@Data
@TableName("sql_case")
public class SqlCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 题目描述（业务场景） */
    private String description;

    /** 难度：入门/进阶/困难 */
    private String difficulty;

    /** 核心知识点：join/group by/window function... */
    private String knowledgePoint;

    /** 标准 SQL 答案 */
    private String standardSql;

    /** 正确结果：{"columns":[...],"rows":[[...]]} */
    private String expectedResult;
}