package com.dev.springmybatissql.service;

import com.dev.springmybatissql.entity.SqlCase;
import com.dev.springmybatissql.vo.ExecuteSqlVO;

import java.util.List;

/**
 * SQL 实验中心 服务接口
 */
public interface SqlCaseService {

    /**
     * 获取题目列表
     */
    List<SqlCase> listCases();

    /**
     * 根据ID获取题目详情（含标准答案）
     */
    SqlCase getCaseById(Long id);

    /**
     * 执行 SQL（自动安全校验），可选与标准答案比对
     */
    ExecuteSqlVO executeSql(String sql, Long caseId);
}