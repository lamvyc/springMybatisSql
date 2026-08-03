package com.dev.springmybatissql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dev.springmybatissql.entity.SqlCase;

/**
 * SQL 实验题目 Mapper
 *
 * 说明：这里使用了 MyBatis Plus 的 BaseMapper，直接获得
 * selectById / selectList / insert 等通用 CRUD 能力，
 * 无需编写 XML。这是 MyBatis Plus 解决"简单 CRUD 效率"的典型场景。
 */
public interface SqlCaseMapper extends BaseMapper<SqlCase> {
}