package com.dev.springmybatissql.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dev.springmybatissql.entity.SqlCase;
import com.dev.springmybatissql.mapper.SqlCaseMapper;
import com.dev.springmybatissql.mapper.SqlExecutorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 实验中心题目初始化器
 *
 * 作用：应用启动后，自动将经典35题迁移 + 现代业务SQL扩展写入 sql_case 表。
 * 关键设计：真实执行标准 SQL，把查询结果序列化为 expected_result(JSON) 存入数据库，
 * 这样用户在实验中心提交自己的 SQL 时，系统才能自动比对结果是否正确。
 *
 * 题目定义集中在 {@link SqlCaseEnum} 枚举中，此处只负责遍历枚举 → 执行SQL → 入库。
 *
 * 幂等设计：sql_case 表已有数据则跳过，不重复插入。
 */
@Slf4j
@Component
public class SqlCaseInitializer implements ApplicationRunner {

    @Autowired
    private SqlCaseMapper sqlCaseMapper;

    @Autowired
    private SqlExecutorMapper sqlExecutorMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long count = sqlCaseMapper.selectCount(new QueryWrapper<>());
        if (count != null && count > 0) {
            log.info("SQL 实验中心：sql_case 已有 {} 道题，跳过初始化", count);
            return;
        }

        List<SqlCase> cases = buildCases();
        for (SqlCase sqlCase : cases) {
            try {
                List<Map<String, Object>> rows = sqlExecutorMapper.executeSelect(sqlCase.getStandardSql());
                sqlCase.setExpectedResult(buildExpectedJson(rows));
                sqlCaseMapper.insert(sqlCase);
            } catch (Exception e) {
                log.error("初始化题目失败 id={} title={} 原因={}", sqlCase.getId(), sqlCase.getTitle(), e.getMessage());
            }
        }
        log.info("SQL 实验中心：成功初始化 {} 道题目", cases.size());
    }

    /**
     * 将查询结果转换为 {"columns":[...],"rows":[[...]]} JSON
     */
    private String buildExpectedJson(List<Map<String, Object>> rows) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }
        result.put("columns", columns);

        List<List<Object>> dataRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> dataRow = new ArrayList<>();
            for (String col : columns) {
                dataRow.add(row.get(col));
            }
            dataRows.add(dataRow);
        }
        result.put("rows", dataRows);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 遍历 {@link SqlCaseEnum} 枚举，将每道题的定义转换为 {@link SqlCase} 实体
     */
    private List<SqlCase> buildCases() {
        List<SqlCase> list = new ArrayList<>();
        for (SqlCaseEnum e : SqlCaseEnum.values()) {
            SqlCase sqlCase = new SqlCase();
            sqlCase.setId(e.getId());
            sqlCase.setTitle(e.getTitle());
            sqlCase.setDescription(e.getDescription());
            sqlCase.setDifficulty(e.getDifficulty());
            sqlCase.setKnowledgePoint(e.getKnowledgePoint());
            sqlCase.setStandardSql(e.getStandardSql());
            list.add(sqlCase);
        }
        return list;
    }
}