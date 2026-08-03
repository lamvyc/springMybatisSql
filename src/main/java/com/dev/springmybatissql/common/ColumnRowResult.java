package com.dev.springmybatissql.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 任意 SQL 查询结果
 * 用于 SQL 实验中心：执行任意 SELECT 语句后返回的通用结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnRowResult {

    /** 列名列表 */
    private List<String> columns;

    /** 行数据：每行是一个 Map（列名 -> 值） */
    private List<Map<String, Object>> rows;
}