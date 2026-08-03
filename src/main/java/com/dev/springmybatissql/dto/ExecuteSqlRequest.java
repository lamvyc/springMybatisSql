package com.dev.springmybatissql.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 执行 SQL 请求 DTO
 */
@Data
public class ExecuteSqlRequest {

    /** 要执行的 SQL */
    @NotBlank(message = "SQL 不能为空")
    private String sql;

    /** 关联的题目ID（用于结果验证，可为空） */
    private Long caseId;
}