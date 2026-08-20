package com.dev.springmybatissql.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dev.springmybatissql.entity.SqlCase;
import com.dev.springmybatissql.mapper.SqlCaseMapper;
import com.dev.springmybatissql.mapper.SqlExecutorMapper;
import com.dev.springmybatissql.service.SqlCaseService;
import com.dev.springmybatissql.vo.ExecuteSqlVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL 实验中心 服务实现
 *
 * 核心链路：Controller -> Service -> Mapper(动态SQL) -> MySQL
 *
 * 职责：
 * 1. 管理题目（列表、详情）
 * 2. 执行用户 SQL：安全校验 -> 沙箱保护 -> 执行 -> 结果比对
 */
@Service
public class SqlCaseServiceImpl implements SqlCaseService {

    /** 只允许 SELECT 开头的查询语句（DOTALL：支持格式化后含换行的 SQL） */
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 危险关键字黑名单（沙箱保护） */
    private static final String[] DANGEROUS_KEYWORDS = {
            "insert ", "update ", "delete ", "drop ", "alter ",
            "truncate ", "replace ", "create ", "grant ", "revoke "
    };

    @Autowired
    private SqlCaseMapper sqlCaseMapper;

    @Autowired
    private SqlExecutorMapper sqlExecutorMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /** 是否开启沙箱保护（application.yml 配置） */
    @Value("${sql-learning.sandbox-enabled:true}")
    private boolean sandboxEnabled;

    @Override
    public List<SqlCase> listCases() {
        // 按 id 升序返回题目列表
        return sqlCaseMapper.selectList(new QueryWrapper<SqlCase>().orderByAsc("id"));
    }

    @Override
    public SqlCase getCaseById(Long id) {
        return sqlCaseMapper.selectById(id);
    }

    @Override
    public ExecuteSqlVO executeSql(String sql, Long caseId) {
        ExecuteSqlVO vo = new ExecuteSqlVO();
        long start = System.currentTimeMillis();

        try {
            // ===== 第一步：安全校验 =====
            String safeSql = validateAndClean(sql);

            // ===== 第二步：执行查询 =====
            List<Map<String, Object>> rows = sqlExecutorMapper.executeSelect(safeSql);

            long cost = System.currentTimeMillis() - start;
            vo.setSuccess(true);
            vo.setCostTime(cost);
            vo.setAffectedRows(rows.size());

            // ===== 第三步：解析列名与行数据 =====
            List<String> columns = new ArrayList<>();
            if (!rows.isEmpty()) {
                columns.addAll(rows.get(0).keySet());
            }
            vo.setColumns(columns);
            vo.setRows(rows);

            // ===== 第四步：与标准答案比对（可选） =====
            if (caseId != null) {
                verifyResult(vo, caseId);
            }

        } catch (IllegalArgumentException e) {
            vo.setSuccess(false);
            vo.setErrorMessage(e.getMessage());
            vo.setCostTime(System.currentTimeMillis() - start);
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setErrorMessage("SQL 执行失败：" + e.getMessage());
            vo.setCostTime(System.currentTimeMillis() - start);
        }
        return vo;
    }

    /**
     * 安全校验：只允许 SELECT 查询，沙箱开启时禁止危险关键字
     */
    private String validateAndClean(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String trimmed = sql.trim();

        // 去掉末尾分号，避免多语句拼接走私
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        // 只允许 SELECT
        if (!SELECT_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("实验中心仅允许执行 SELECT 查询语句");
        }

        // 沙箱保护：禁止危险关键字（防止 "select * from user; drop table user" 类注入）
        if (sandboxEnabled) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            for (String keyword : DANGEROUS_KEYWORDS) {
                if (lower.contains(keyword)) {
                    throw new IllegalArgumentException("沙箱保护：禁止执行包含 '" + keyword.trim() + "' 的语句");
                }
            }
        }
        return trimmed;
    }

    /**
     * 与标准答案比对：
     * 标准答案保存在 sql_case.expected_result（JSON：{"columns":[...],"rows":[[...]]}）
     * 比对内容：列名集合、行数、每行数据
     */
    private void verifyResult(ExecuteSqlVO vo, Long caseId) throws Exception {
        SqlCase sqlCase = sqlCaseMapper.selectById(caseId);
        if (sqlCase == null || sqlCase.getExpectedResult() == null || sqlCase.getExpectedResult().isBlank()) {
            vo.setVerified(null);
            vo.setVerifyMessage("该题目未配置标准结果，跳过验证");
            return;
        }

        JsonNode expected = objectMapper.readTree(sqlCase.getExpectedResult());

        // 1. 比对列名（不考虑顺序）
        Set<String> expectedCols = new TreeSet<>();
        for (JsonNode col : expected.get("columns")) {
            expectedCols.add(col.asText());
        }
        Set<String> actualCols = new TreeSet<>();
        if (vo.getColumns() != null) {
            actualCols.addAll(vo.getColumns());
        }

        if (!actualCols.equals(expectedCols)) {
            vo.setVerified(false);
            // 缺失列与多余列分开提示，并指引用户参见题干【期望输出列】
            Set<String> missingCols = new TreeSet<>(expectedCols);
            missingCols.removeAll(actualCols);
            Set<String> extraCols = new TreeSet<>(actualCols);
            extraCols.removeAll(expectedCols);
            StringBuilder msg = new StringBuilder("列名不一致，请按题干【期望输出列】编写 SELECT。");
            msg.append("期望列：").append(expectedCols).append("，实际列：").append(actualCols);
            if (!missingCols.isEmpty()) {
                msg.append("；缺少列：").append(missingCols);
            }
            if (!extraCols.isEmpty()) {
                msg.append("；多余列：").append(extraCols);
            }
            vo.setVerifyMessage(msg.toString());
            return;
        }

        // 2. 比对行数
        JsonNode expectedRows = expected.get("rows");
        int expectedCount = expectedRows.size();
        int actualCount = vo.getRows() == null ? 0 : vo.getRows().size();

        if (expectedCount != actualCount) {
            vo.setVerified(false);
            vo.setVerifyMessage("行数不一致。期望 " + expectedCount + " 行，实际 " + actualCount + " 行");
            return;
        }

        // 3. 比对数据内容（按标准结果顺序）
        for (int i = 0; i < expectedCount; i++) {
            JsonNode expectedRow = expectedRows.get(i);
            Map<String, Object> actualRow = vo.getRows().get(i);

            List<String> expectedColList = new ArrayList<>();
            expected.get("columns").forEach(c -> expectedColList.add(c.asText()));

            for (int j = 0; j < expectedColList.size(); j++) {
                String col = expectedColList.get(j);
                String expVal = normalize(expectedRow.get(j).asText());
                String actVal = normalize(String.valueOf(actualRow.get(col)));
                if (!expVal.equals(actVal)) {
                    vo.setVerified(false);
                    vo.setVerifyMessage("第 " + (i + 1) + " 行、列 '" + col + "' 数据不一致。期望值：" + expVal + "，实际值：" + actVal);
                    return;
                }
            }
        }

        vo.setVerified(true);
        vo.setVerifyMessage("结果与标准答案完全一致");
    }

    /**
     * 值归一化：用于 SQL 结果与标准答案比对
     * 处理技巧：
     * 1. 去掉两端空白
     * 2. 数值统一用 BigDecimal.stripTrailingZeros 去掉尾随0（8000.00 -> 8000）
     *    因为 expected_result 存到 MySQL JSON 列后数值会被规范化（8000.00 可能变 8000）
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        // 尝试数值归一化
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(v);
            return bd.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            // 非数值直接返回原值
            return v;
        }
    }
}
