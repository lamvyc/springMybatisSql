package com.dev.springmybatissql.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 索引实战实验台 · 单条语句执行结果 VO
 *
 * kind=query 时返回 columns + rows（SELECT/EXPLAIN/SHOW/ANALYZE 的结果集）；
 * kind=ddl   时返回 message + affectedRows（建索引/删索引等）。
 * 前端根据 kind 决定画“结果表格”还是“成功提示”。
 */
@Data
public class SqlRunResultVO {

    /** 是否执行成功 */
    private Boolean ok;

    /** 失败原因（失败时才有） */
    private String errorMessage;

    /** query=返回结果集的语句；ddl=索引变更语句 */
    private String kind;

    /** 实际执行的 SQL */
    private String sql;

    /** 执行耗时（毫秒） */
    private Long elapsedMs;

    /** 结果列名（kind=query） */
    private List<String> columns;

    /** 结果行（kind=query） */
    private List<Map<String, Object>> rows;

    /** 影响行数（kind=ddl，MySQL DDL 一般为 0） */
    private Integer affectedRows;
}
