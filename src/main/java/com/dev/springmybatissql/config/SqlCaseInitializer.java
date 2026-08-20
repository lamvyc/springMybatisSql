package com.dev.springmybatissql.config;

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
 * 幂等设计：按 id upsert（存在则更新、不存在则插入）。
 * 这样修改枚举中的题干/期望列/标准答案后，重启应用即自动生效，无需手动 TRUNCATE。
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
        List<SqlCase> cases = buildCases();
        int inserted = 0;
        int updated = 0;
        for (SqlCase sqlCase : cases) {
            try {
                List<Map<String, Object>> rows = sqlExecutorMapper.executeSelect(sqlCase.getStandardSql());
                sqlCase.setExpectedResult(buildExpectedJson(rows));

                SqlCase exist = sqlCaseMapper.selectById(sqlCase.getId());
                if (exist == null) {
                    sqlCaseMapper.insert(sqlCase);
                    inserted++;
                } else {
                    sqlCaseMapper.updateById(sqlCase);
                    updated++;
                }
            } catch (Exception e) {
                log.error("初始化题目失败 id={} title={} 原因={}", sqlCase.getId(), sqlCase.getTitle(), e.getMessage());
            }
        }
        log.info("SQL 实验中心：共 {} 道题，新增 {} 道，更新 {} 道", cases.size(), inserted, updated);
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