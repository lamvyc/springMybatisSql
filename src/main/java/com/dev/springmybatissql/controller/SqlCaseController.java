package com.dev.springmybatissql.controller;

import com.dev.springmybatissql.common.Result;
import com.dev.springmybatissql.dto.ExecuteSqlRequest;
import com.dev.springmybatissql.entity.SqlCase;
import com.dev.springmybatissql.service.SqlCaseService;
import com.dev.springmybatissql.vo.ExecuteSqlVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SQL 实验中心 Controller
 *
 * 提供：
 * 1. 题目管理：列表、详情
 * 2. SQL 执行台：提交 SQL -> 执行 -> 返回结果（含验证）
 */
@RestController
@RequestMapping("/api/sql")
public class SqlCaseController {

    @Autowired
    private SqlCaseService sqlCaseService;

    /**
     * 题目列表
     */
    @GetMapping("/cases")
    public Result<List<SqlCase>> listCases() {
        return Result.ok(sqlCaseService.listCases());
    }

    /**
     * 题目详情（含标准答案，用于对照学习）
     */
    @GetMapping("/cases/{id}")
    public Result<SqlCase> getCase(@PathVariable Long id) {
        return Result.ok(sqlCaseService.getCaseById(id));
    }

    /**
     * 执行 SQL 并（可选）验证结果
     * 请求体：{"sql": "...", "caseId": 1}
     */
    @PostMapping("/execute")
    public Result<ExecuteSqlVO> execute(@Valid @RequestBody ExecuteSqlRequest request) {
        return Result.ok(sqlCaseService.executeSql(request.getSql(), request.getCaseId()));
    }

    /**
     * 方式1：控制台执行
     * GET /api/sql/run?sql=select...  （方便直接在浏览器测试）
     */
    @GetMapping("/run")
    public Result<ExecuteSqlVO> run(@RequestParam("sql") String sql,
                                    @RequestParam(value = "caseId", required = false) Long caseId) {
        return Result.ok(sqlCaseService.executeSql(sql, caseId));
    }
}
