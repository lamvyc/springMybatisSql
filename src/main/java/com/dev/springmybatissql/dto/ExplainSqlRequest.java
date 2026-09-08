package com.dev.springmybatissql.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 索引实战演示 · 通用 EXPLAIN 请求体（IndexDemo 模块专用）
 */
@Data
public class ExplainSqlRequest {

    /** 要分析的 SELECT 语句（示例：select * from mall_order where user_id = 100） */
    @NotBlank(message = "sql 不能为空")
    private String sql;
}
