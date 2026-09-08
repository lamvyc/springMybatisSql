package com.dev.springmybatissql.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 索引实战实验台 · 执行一条语句的请求体
 *
 * 允许的语句范围（由 Service 白名单校验）：
 * - SELECT / EXPLAIN SELECT（只能查 mall_* 表）
 * - SHOW INDEX FROM mall_*
 * - ANALYZE TABLE mall_*
 * - CREATE INDEX / DROP INDEX / ALTER TABLE ... ADD|DROP INDEX（只能作用于 mall_* 表）
 */
@Data
public class SqlRunRequest {

    @NotBlank(message = "sql 不能为空")
    private String sql;
}
