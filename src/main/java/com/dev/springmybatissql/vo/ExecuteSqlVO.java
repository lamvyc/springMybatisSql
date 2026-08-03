package com.dev.springmybatissql.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SQL 执行结果 VO（输出给前端页面展示）
 */
@Data
public class ExecuteSqlVO {

    /** 是否执行成功 */
    private Boolean success;

    /** 执行耗时（毫秒） */
    private Long costTime;

    /** 错误信息（执行失败时） */
    private String errorMessage;

    /** 查询结果的列名 */
    private List<String> columns;

    /** 查询结果的行数据 */
    private List<Map<String, Object>> rows;

    /** 影响行数（非 SELECT 语句） */
    private Integer affectedRows;

    /** 验证结果：与标准答案比对是否一致 */
    private Boolean verified;

    /** 验证差异说明 */
    private String verifyMessage;
}