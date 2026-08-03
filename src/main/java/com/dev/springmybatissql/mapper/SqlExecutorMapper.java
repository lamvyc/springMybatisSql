package com.dev.springmybatissql.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 通用 SQL 执行器 Mapper
 *
 * 作用：动态执行传入的 SELECT SQL，返回通用结果。
 * 注意：这里使用 ${sql} 拼接（而非 #{sql}），因为 SQL 语句本身不能做预编译占位。
 * 危险：${} 有 SQL 注入风险，因此 Service 层必须做严格的语句白名单校验
 * （只允许 SELECT 开头的查询语句）。
 */
public interface SqlExecutorMapper {

    /**
     * 执行任意 SELECT 查询
     *
     * @param sql 查询语句（必须经过安全校验）
     * @return 行数据列表，每行是 列名->值 的 Map
     */
    @Select("${sql}")
    List<Map<String, Object>> executeSelect(@Param("sql") String sql);
}