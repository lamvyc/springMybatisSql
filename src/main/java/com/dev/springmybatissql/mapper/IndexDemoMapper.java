package com.dev.springmybatissql.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 索引实战演示 Mapper（IndexDemo 模块专用）
 *
 * 与 SqlExecutorMapper 一样使用 ${sql} 动态执行（SQL 语句本身无法用 #{} 占位），
 * 因此 Service 层必须做白名单校验：
 * - executeQuery  只允许 SELECT/EXPLAIN/SHOW/ANALYZE（返回结果集）
 * - executeUpdate 只允许对 mall_* 表做 CREATE/DROP/ALTER INDEX（不返回结果集）
 *
 * selectMallIndexes / selectMallTableStats 是服务端写死的元数据查询（无注入面），
 * 供前端“数据概览”展示当前索引与体积。
 */
public interface IndexDemoMapper {

    /**
     * 执行任意查询（SELECT / EXPLAIN / SHOW / ANALYZE）
     */
    @Select("${sql}")
    List<Map<String, Object>> executeQuery(@Param("sql") String sql);

    /**
     * 执行任意 DDL（只允许经过白名单校验的索引操作）
     */
    @Update("${sql}")
    int executeUpdate(@Param("sql") String sql);

    /**
     * mall_* 表当前的索引与索引内列顺序（来自 information_schema）
     */
    @Select("SELECT i.table_name AS tbl, i.index_name AS idx, i.column_name AS col, i.seq_in_index AS seq " +
            "FROM information_schema.statistics i " +
            "WHERE i.table_schema = DATABASE() AND i.table_name LIKE 'mall\\_%' " +
            "ORDER BY i.table_name, i.index_name, i.seq_in_index")
    List<Map<String, Object>> selectMallIndexes();

    /**
     * mall_* 表的数据/索引占用空间（来自 information_schema）
     */
    @Select("SELECT t.table_name AS tbl, " +
            "ROUND(t.data_length  / 1024 / 1024, 2) AS data_mb, " +
            "ROUND(t.index_length / 1024 / 1024, 2) AS idx_mb " +
            "FROM information_schema.tables t " +
            "WHERE t.table_schema = DATABASE() AND t.table_name LIKE 'mall\\_%'")
    List<Map<String, Object>> selectMallTableStats();
}
