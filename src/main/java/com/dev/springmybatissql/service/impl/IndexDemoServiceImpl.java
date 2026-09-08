package com.dev.springmybatissql.service.impl;

import com.dev.springmybatissql.mapper.IndexDemoMapper;
import com.dev.springmybatissql.service.IndexDemoService;
import com.dev.springmybatissql.vo.IndexDemoVO;
import com.dev.springmybatissql.vo.SqlRunResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 索引实战演示服务实现（IndexDemo 模块专用）
 *
 * 每个方法都做同一件事：
 *   拼出“真实业务代码里那条 SQL” → 原样执行（带耗时）
 *   → 再对它执行 EXPLAIN（带耗时） → 汇总成 IndexDemoVO
 *
 * 学习视角：不用理解这里每一行 Java，重点是返回结果里的
 * sql / elapsedMs / explainRows(type、key、rows、Extra) 三个字段。
 */
@Slf4j
@Service
public class IndexDemoServiceImpl implements IndexDemoService {

    /** 订单列表要返回的列（生产里绝不写 SELECT *，见阶段4） */
    private static final String ORDER_COLS = "id, order_no, user_id, status, pay_amount, create_time";

    /** 用户查询要返回的列 */
    private static final String USER_COLS = "id, username, phone, email, status, register_time";

    /** 只允许 SELECT 开头 */
    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*select\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 只允许针对索引教学表查询 */
    private static final Pattern MALL_TABLE_PATTERN =
            Pattern.compile("(?is)\\b(from|join)\\s+`?(mall_user|mall_product|mall_order)`?\\b");

    /** 危险关键字黑名单 */
    private static final String[] DANGEROUS_KEYWORDS = {
            "insert ", "update ", "delete ", "drop ", "alter ",
            "truncate ", "replace ", "create ", "grant ", "revoke ", "into "
    };

    /** 手机号：只允许 5~11 位数字（杜绝注入） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{5,11}$");

    /** 时间参数：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}:\\d{2})?$");

    // ==================== 实验台单语句执行的白名单 ====================
    /** 返回结果集的语句：SELECT */
    private static final Pattern Q_SELECT = Pattern.compile("^\\s*select\\b.*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** 返回结果集的语句：EXPLAIN SELECT（允许 FORMAT=JSON/TREE） */
    private static final Pattern Q_EXPLAIN = Pattern.compile("^\\s*explain\\s+(format\\s*=\\s*(json|tree)\\s+)?select\\b.*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** 返回结果集的语句：SHOW INDEX FROM mall_* */
    private static final Pattern Q_SHOW_INDEX = Pattern.compile(
            "^\\s*show\\s+index\\s+from\\s+`?(mall_user|mall_product|mall_order)`?\\s*$", Pattern.CASE_INSENSITIVE);
    /** 返回结果集的语句：ANALYZE TABLE mall_*（可逗号多个） */
    private static final Pattern Q_ANALYZE = Pattern.compile(
            "^\\s*analyze\\s+table\\s+(mall_user|mall_product|mall_order)(\\s*,\\s*(mall_user|mall_product|mall_order))*\\s*$",
            Pattern.CASE_INSENSITIVE);
    /** DDL：CREATE [UNIQUE] INDEX ... ON mall_* (...) */
    private static final Pattern D_CREATE_INDEX = Pattern.compile(
            "^\\s*create\\s+(unique\\s+)?index\\s+`?[A-Za-z0-9_]+`?\\s+on\\s+(mall_user|mall_product|mall_order)\\s*\\([A-Za-z0-9_`, ()]+\\)\\s*$",
            Pattern.CASE_INSENSITIVE);
    /** DDL：DROP INDEX ... ON mall_* */
    private static final Pattern D_DROP_INDEX = Pattern.compile(
            "^\\s*drop\\s+index\\s+`?[A-Za-z0-9_]+`?\\s+on\\s+(mall_user|mall_product|mall_order)\\s*$",
            Pattern.CASE_INSENSITIVE);
    /** DDL：ALTER TABLE mall_* ADD [UNIQUE] INDEX / DROP INDEX */
    private static final Pattern D_ALTER_INDEX = Pattern.compile(
            "^\\s*alter\\s+table\\s+(mall_user|mall_product|mall_order)\\s+("
                    + "add\\s+(unique\\s+)?(index|key)\\s+`?[A-Za-z0-9_]+`?\\s*\\([A-Za-z0-9_`, ()]+\\)"
                    + "|drop\\s+(index|key)\\s+`?[A-Za-z0-9_]+`?)"
                    + "\\s*$",
            Pattern.CASE_INSENSITIVE);

    @Autowired
    private IndexDemoMapper indexDemoMapper;

    // ==================== 我的订单列表（阶段3） ====================

    @Override
    public IndexDemoVO myOrders(Long userId, Integer status, int pageNo, int pageSize) {
        // 1. 参数校验（userId 必填；status 可选 = 只看该用户全部订单）
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId 必须是正整数");
        }
        if (status != null && (status < 0 || status > 4)) {
            throw new IllegalArgumentException("status 必须在 0~4 之间");
        }
        int[] pg = normalizePage(pageNo, pageSize);

        // 2. 拼真实业务 SQL（条件按需拼接，和真实 Mapper XML 的 <where> 动态 SQL 行为一致）
        StringBuilder where = new StringBuilder("WHERE user_id = ").append(userId);
        if (status != null) {
            where.append(" AND status = ").append(status);
        }
        String sql = "SELECT " + ORDER_COLS
                + " FROM mall_order " + where
                + " ORDER BY create_time DESC"
                + " LIMIT " + pg[0] + ", " + pg[1];

        return runAndExplain("myOrders", sql);
    }

    // ==================== 运营后台订单列表（阶段6） ====================

    @Override
    public IndexDemoVO adminOrders(Integer status, String beginTime, String endTime, int pageNo, int pageSize) {
        if (status == null || status < 0 || status > 4) {
            throw new IllegalArgumentException("status 必填，且必须在 0~4 之间");
        }
        int[] pg = normalizePage(pageNo, pageSize);

        // 时间窗口：不传 begin/end 就默认覆盖全部订单（真实后台的“全部时间段”场景）
        String begin = beginTime == null || beginTime.isBlank() ? "2023-01-01 00:00:00" : beginTime;
        String end = endTime == null || endTime.isBlank() ? "2030-12-31 23:59:59" : endTime;
        if (!DATE_PATTERN.matcher(begin.trim()).matches() || !DATE_PATTERN.matcher(end.trim()).matches()) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }

        String sql = "SELECT " + ORDER_COLS
                + " FROM mall_order"
                + " WHERE status = " + status
                + " AND create_time BETWEEN '" + begin.trim() + "' AND '" + end.trim() + "'"
                + " ORDER BY create_time DESC"
                + " LIMIT " + pg[0] + ", " + pg[1];

        return runAndExplain("adminOrders", sql);
    }

    // ==================== 商品列表（阶段4：回表 vs 覆盖索引） ====================

    @Override
    public IndexDemoVO products(Integer categoryId, String mode, int pageNo, int pageSize) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("categoryId 必须是正整数");
        }
        boolean light = !"all".equalsIgnoreCase(mode);
        int[] pg = normalizePage(pageNo, pageSize);

        // mode=light：只取列表要展示的列（id/category_id/price/stock），
        // 当联合索引 (category_id,status,price,stock) 存在时可完全覆盖，不产生回表。
        String cols = light
                ? "id, category_id, price, stock"
                : "*";
        String sql = "SELECT " + cols
                + " FROM mall_product"
                + " WHERE category_id = " + categoryId + " AND status = 1"
                + " ORDER BY price DESC"
                + " LIMIT " + pg[0] + ", " + pg[1];

        return runAndExplain(light ? "products-light" : "products-all", sql);
    }

    // ==================== 按手机号查用户（阶段1/2） ====================

    @Override
    public IndexDemoVO userByPhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("phone 必须是 5~11 位数字");
        }
        String sql = "SELECT " + USER_COLS
                + " FROM mall_user"
                + " WHERE phone = '" + phone.trim() + "'";

        return runAndExplain("userByPhone", sql);
    }

    // ==================== 通用 EXPLAIN（各阶段练习） ====================

    @Override
    public IndexDemoVO explain(String rawSql) {
        IndexDemoVO vo = new IndexDemoVO();
        vo.setDemo("explain");
        if (rawSql == null || rawSql.trim().isEmpty()) {
            throw new IllegalArgumentException("sql 不能为空");
        }

        // 去掉末尾分号，防止多语句拼接走私
        String sql = rawSql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        // 兼容用户粘贴 “explain select ...”（把 explain 前缀去掉，统一由服务加）
        String lowerHead = sql.toLowerCase();
        if (lowerHead.startsWith("explain ")) {
            sql = sql.substring("explain ".length()).trim();
        }
        if (!SELECT_PATTERN.matcher(sql).matches()) {
            throw new IllegalArgumentException("仅支持 SELECT 查询语句");
        }
        if (!MALL_TABLE_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("仅支持查询索引教学表：mall_user / mall_product / mall_order");
        }
        String lower = sql.toLowerCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lower.contains(keyword)) {
                throw new IllegalArgumentException("禁止包含危险关键字：" + keyword.trim());
            }
        }
        // 通用 EXPLAIN：只分析执行计划，不真正跑业务 SELECT（避免全表扫描卡死页面）
        return runExplainOnly(sql);
    }

    // ==================== 实验台：单语句执行 / 数据概览 / 重置索引 ====================

    @Override
    public SqlRunResultVO runSql(String rawSql) {
        SqlRunResultVO vo = new SqlRunResultVO();
        if (rawSql == null || rawSql.trim().isEmpty()) {
            return fail(vo, "SQL 不能为空", rawSql);
        }
        String sql = rawSql.trim();
        // 去掉单个末尾分号；再出现分号 = 多语句拼接，直接拒绝
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            return fail(vo, "实验台只允许一次执行一条语句", rawSql);
        }
        vo.setSql(sql);

        // 判断语句种类（query=返回结果集；ddl=索引变更）
        String kind;
        if (Q_SELECT.matcher(sql).matches() || Q_EXPLAIN.matcher(sql).matches()) {
            kind = "query";
            if (!MALL_TABLE_PATTERN.matcher(sql).find()) {
                return fail(vo, "SELECT 只能查询索引教学表：mall_user / mall_product / mall_order", rawSql);
            }
            if (containsDangerous(sql)) {
                return fail(vo, "禁止包含危险关键字", rawSql);
            }
        } else if (Q_SHOW_INDEX.matcher(sql).matches() || Q_ANALYZE.matcher(sql).matches()) {
            kind = "query";
        } else if (D_CREATE_INDEX.matcher(sql).matches()
                || D_DROP_INDEX.matcher(sql).matches()
                || D_ALTER_INDEX.matcher(sql).matches()) {
            kind = "ddl";
        } else {
            return fail(vo, "实验台支持：SELECT/EXPLAIN(查 mall_* 表)、SHOW INDEX、ANALYZE TABLE、"
                    + "以及 mall_* 表上的 CREATE/DROP/ALTER INDEX", rawSql);
        }

        try {
            long start = System.nanoTime();
            if ("query".equals(kind)) {
                List<Map<String, Object>> rows = indexDemoMapper.executeQuery(sql);
                vo.setKind("query");
                vo.setOk(true);
                vo.setElapsedMs(Math.max(1, (System.nanoTime() - start) / 1_000_000));
                vo.setRows(rows);
                if (!rows.isEmpty()) {
                    vo.setColumns(new ArrayList<>(rows.get(0).keySet()));
                } else {
                    vo.setColumns(new ArrayList<>());
                }
                log.info("【实验台:query】{}ms rows={} | sql: {}", vo.getElapsedMs(), rows.size(), sql);
            } else {
                int affected = indexDemoMapper.executeUpdate(sql);
                vo.setKind("ddl");
                vo.setOk(true);
                vo.setElapsedMs(Math.max(1, (System.nanoTime() - start) / 1_000_000));
                vo.setAffectedRows(affected);
                log.info("【实验台:ddl】{}ms | sql: {}", vo.getElapsedMs(), sql);
            }
            return vo;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            if (msg.contains("Duplicate key name")) {
                msg = "该索引已存在（名称冲突）。可先执行：DROP INDEX <同名索引> ON 对应表";
            } else if (msg.contains("check that column/key exists") || msg.contains("doesn't exist")) {
                msg = "目标索引/表不存在，可能是名字写错或还没创建";
            }
            return fail(vo, msg, sql);
        }
    }

    @Override
    public Map<String, Object> state() {
        Map<String, Object> out = new LinkedHashMap<>();

        // 体积（information_schema，数据来自数据字典）
        Map<String, Map<String, Object>> sizeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : indexDemoMapper.selectMallTableStats()) {
            sizeMap.put(String.valueOf(row.get("tbl")), row);
        }
        // 当前索引清单（name -> 列列表，保持顺序）
        Map<String, List<String>> indexCols = new LinkedHashMap<>();
        for (Map<String, Object> row : indexDemoMapper.selectMallIndexes()) {
            String key = row.get("tbl") + "." + row.get("idx");
            indexCols.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(String.valueOf(row.get("col")));
        }
        // 三张表精确行数 + 一个示例手机号
        String[] tables = {"mall_user", "mall_product", "mall_order"};
        List<Map<String, Object>> tablesOut = new ArrayList<>();
        String samplePhone = null;
        for (String t : tables) {
            Map<String, Object> tRow = new LinkedHashMap<>();
            List<Map<String, Object>> cnt = indexDemoMapper.executeQuery("SELECT COUNT(*) AS cnt FROM " + t);
            tRow.put("table", t);
            tRow.put("rows", cnt.isEmpty() ? 0L : cnt.get(0).get("cnt"));
            Map<String, Object> sz = sizeMap.get(t);
            tRow.put("dataMb", sz == null ? null : sz.get("data_mb"));
            tRow.put("idxMb", sz == null ? null : sz.get("idx_mb"));
            List<Map<String, Object>> idxList = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : indexCols.entrySet()) {
                if (e.getKey().startsWith(t + ".")) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("name", e.getKey().substring(t.length() + 1));
                    im.put("columns", e.getValue());
                    idxList.add(im);
                }
            }
            tRow.put("indexes", idxList);
            tablesOut.add(tRow);
        }
        List<Map<String, Object>> phoneRows =
                indexDemoMapper.executeQuery("SELECT phone FROM mall_user WHERE phone IS NOT NULL ORDER BY id LIMIT 1");
        if (!phoneRows.isEmpty() && phoneRows.get(0).get("phone") != null) {
            samplePhone = String.valueOf(phoneRows.get(0).get("phone"));
        }
        out.put("tables", tablesOut);
        out.put("samplePhone", samplePhone);
        return out;
    }

    @Override
    public List<String> resetIndexes() {
        // 找出所有非 PRIMARY、非 uk_* 业务唯一索引（即本教程建过的二级索引）
        List<String> toDrop = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (Map<String, Object> row : indexDemoMapper.selectMallIndexes()) {
            String tbl = String.valueOf(row.get("tbl"));
            String idx = String.valueOf(row.get("idx"));
            if (idx.equalsIgnoreCase("PRIMARY") || idx.startsWith("uk_")) {
                continue;
            }
            String stmt = "DROP INDEX `" + idx + "` ON `" + tbl + "`";
            if (!toDrop.contains(stmt)) {
                toDrop.add(stmt);
            }
        }
        for (String stmt : toDrop) {
            try {
                indexDemoMapper.executeUpdate(stmt);
                dropped.add(stmt);
            } catch (Exception e) {
                log.warn("重置索引时跳过（可能已不存在）：{} - {}", stmt, e.getMessage());
            }
        }
        // 顺手刷新统计信息，保证后面 EXPLAIN 的 rows 估算准确
        for (String t : new String[]{"mall_user", "mall_product", "mall_order"}) {
            try {
                indexDemoMapper.executeUpdate("ANALYZE TABLE " + t);
            } catch (Exception ignored) {
            }
        }
        return dropped;
    }

    /** 把执行失败信息封装进 VO */
    private SqlRunResultVO fail(SqlRunResultVO vo, String msg, String sql) {
        vo.setKind(null);
        vo.setOk(false);
        vo.setErrorMessage(msg);
        vo.setSql(sql);
        return vo;
    }

    /** 危险关键字检查（用于 SELECT 语句） */
    private boolean containsDangerous(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 内部工具 ====================

    /** 执行 SQL + 执行 EXPLAIN，合并为一个 VO */
    private IndexDemoVO runAndExplain(String demo, String sql) {
        IndexDemoVO vo = new IndexDemoVO();
        vo.setDemo(demo);
        vo.setSql(sql);

        try {
            // ① 执行业务 SQL（模拟真实接口调用）
            long s1 = System.nanoTime();
            List<Map<String, Object>> rows = indexDemoMapper.executeQuery(sql);
            long cost1 = Math.max(1, (System.nanoTime() - s1) / 1_000_000);

            // ② 对同一 SQL 执行 EXPLAIN
            long s2 = System.nanoTime();
            List<Map<String, Object>> explainRows = indexDemoMapper.executeQuery("EXPLAIN " + sql);
            long cost2 = Math.max(1, (System.nanoTime() - s2) / 1_000_000);

            vo.setSuccess(true);
            vo.setElapsedMs(cost1);
            vo.setRowCount(rows.size());
            vo.setRows(rows);
            vo.setExplainElapsedMs(cost2);
            vo.setExplainRows(explainRows);
            vo.setExplainSummary(summarize(explainRows));

            log.info("【IndexDemo:{}】{}ms -> type={} key={} rows={} | sql: {}",
                    demo, cost1, firstField(explainRows, "type"),
                    firstField(explainRows, "key"), firstField(explainRows, "rows"), sql);
            return vo;
        } catch (Exception e) {
            // 统一转成“可读的失败”，不让堆栈把学习页淹没
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            if (msg.contains("doesn't exist") || msg.contains("does not exist")) {
                msg = "表不存在：请先在 MySQL 执行 docs/mysql-index/sql/00_建表与造数.sql 完成建表+造数";
            }
            vo.setSuccess(false);
            vo.setErrorMessage(msg);
            log.warn("【IndexDemo:{}】执行失败: {}", demo, msg);
            return vo;
        }
    }

    /** 只执行 EXPLAIN、不执行业务 SELECT（供通用 explain 端点使用） */
    private IndexDemoVO runExplainOnly(String sql) {
        IndexDemoVO vo = new IndexDemoVO();
        vo.setDemo("explain");
        vo.setSql(sql);
        try {
            long s = System.nanoTime();
            List<Map<String, Object>> explainRows = indexDemoMapper.executeQuery("EXPLAIN " + sql);
            long cost = Math.max(1, (System.nanoTime() - s) / 1_000_000);

            vo.setSuccess(true);
            vo.setElapsedMs(cost);
            vo.setRowCount(0);
            vo.setRows(List.of());
            vo.setExplainElapsedMs(cost);
            vo.setExplainRows(explainRows);
            vo.setExplainSummary(summarize(explainRows));
            log.info("【IndexDemo:explain】type={} key={} rows={} | sql: {}",
                    firstField(explainRows, "type"), firstField(explainRows, "key"),
                    firstField(explainRows, "rows"), sql);
            return vo;
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setErrorMessage(e.getMessage());
            log.warn("【IndexDemo:explain】执行失败: {}", e.getMessage());
            return vo;
        }
    }

    /** 分页参数规范化：pageNo>=1、pageSize 1~100 */
    private int[] normalizePage(int pageNo, int pageSize) {
        int pn = Math.max(pageNo, 1);
        int ps = Math.min(Math.max(pageSize, 1), 100);
        return new int[]{ (pn - 1) * ps, ps };
    }

    /** 从 EXPLAIN 输出第一行拼一行人话总结 */
    private String summarize(List<Map<String, Object>> explainRows) {
        if (explainRows == null || explainRows.isEmpty()) {
            return "";
        }
        Map<String, Object> first = explainRows.get(0);
        return "type=" + nvl(first.get("type"))
                + " key=" + nvl(first.get("key"))
                + " rows=" + nvl(first.get("rows"))
                + " extra=" + nvl(first.get("Extra"));
    }

    /** 取 EXPLAIN 第一行的某个字段值（兼容大小写） */
    private String firstField(List<Map<String, Object>> explainRows, String field) {
        if (explainRows == null || explainRows.isEmpty()) {
            return "-";
        }
        Map<String, Object> first = explainRows.get(0);
        Object v = first.get(field);
        if (v == null) {
            for (Map.Entry<String, Object> e : first.entrySet()) {
                if (e.getKey().equalsIgnoreCase(field)) {
                    v = e.getValue();
                    break;
                }
            }
        }
        return nvl(v);
    }

    private String nvl(Object v) {
        return v == null ? "NULL" : String.valueOf(v);
    }
}
