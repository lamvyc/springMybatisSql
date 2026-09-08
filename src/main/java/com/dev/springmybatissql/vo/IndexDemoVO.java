package com.dev.springmybatissql.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 索引实战演示结果 VO（IndexDemo 模块专用）
 *
 * 每次“真实业务调用”同时返回：
 * 1. 业务 SQL 与它的真实执行结果（rows、elapsedMs）
 * 2. 同一条 SQL 的 EXPLAIN 结果（explainRows、explainSummary）
 *
 * 目的：把线上排查“接口慢 → 抓 SQL → EXPLAIN → 看 type/key/rows/extra”的
 * 完整动作，直接搬到学习接口里，Java 只做业务调用模拟，不喧宾夺主。
 */
@Data
public class IndexDemoVO {

    /** 演示场景标识：myOrders / adminOrders / products-all / products-light / userByPhone / explain */
    private String demo;

    /** 是否执行成功 */
    private Boolean success;

    /** 失败原因（失败时才有） */
    private String errorMessage;

    /** 实际执行的业务 SQL（带参数值） */
    private String sql;

    /** 业务 SQL 执行耗时（毫秒） */
    private Long elapsedMs;

    /** 返回行数 */
    private Integer rowCount;

    /** 业务 SQL 返回的数据行 */
    private List<Map<String, Object>> rows;

    /** EXPLAIN 执行耗时（毫秒） */
    private Long explainElapsedMs;

    /** EXPLAIN 原始输出（type/key/rows/Extra 等列） */
    private List<Map<String, Object>> explainRows;

    /** EXPLAIN 一行总结：type=ref key=idx_xxx rows=12 extra=Using index condition */
    private String explainSummary;
}
