package com.dev.springmybatissql.controller;

import com.dev.springmybatissql.common.Result;
import com.dev.springmybatissql.dto.ExplainSqlRequest;
import com.dev.springmybatissql.dto.SqlRunRequest;
import com.dev.springmybatissql.service.IndexDemoService;
import com.dev.springmybatissql.vo.IndexDemoVO;
import com.dev.springmybatissql.vo.SqlRunResultVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 索引实战演示 Controller（IndexDemo 模块专用，与 docs/mysql-index 教程配套）
 *
 * 设计原则：Java 只做“真实业务调用”的模拟——接口内部就是一条真实 SQL + 一次
 * EXPLAIN，把“慢 SQL → EXPLAIN → 看 type/key/rows/Extra”完整搬到 HTTP 上。
 * 建索引 / 改 SQL 仍在 MySQL 里做（docs/mysql-index/sql/*.sql），不提供 DDL 接口。
 */
@RestController
@RequestMapping("/api/index-demo")
public class IndexDemoController {

    @Autowired
    private IndexDemoService indexDemoService;

    /**
     * 模拟：按手机号查用户（阶段1/2 首次建索引的“业务接口”）
     * GET /api/index-demo/user/by-phone?phone=13800138000
     */
    @GetMapping("/user/by-phone")
    public Result<IndexDemoVO> userByPhone(@RequestParam("phone") String phone) {
        try {
            return Result.ok(indexDemoService.userByPhone(phone));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 模拟：“我的订单列表”接口（阶段3：联合索引主战场）
     * GET /api/index-demo/my-orders?userId=1000&status=1&pageNo=1&pageSize=20
     * status 不传 = 查该用户全部订单（动态拼条件，和真实 Mapper XML <where> 一致）
     */
    @GetMapping("/my-orders")
    public Result<IndexDemoVO> myOrders(@RequestParam("userId") Long userId,
                                        @RequestParam(value = "status", required = false) Integer status,
                                        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        try {
            return Result.ok(indexDemoService.myOrders(userId, status, pageNo, pageSize));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 模拟：运营后台“按状态+时间范围”查订单接口（阶段6：订单查询接口突然变慢）
     * GET /api/index-demo/admin-orders?status=1&beginTime=2024-06-01&endTime=2024-06-30&pageNo=1&pageSize=50
     */
    @GetMapping("/admin-orders")
    public Result<IndexDemoVO> adminOrders(@RequestParam(value = "status", defaultValue = "1") Integer status,
                                           @RequestParam(value = "beginTime", required = false) String beginTime,
                                           @RequestParam(value = "endTime", required = false) String endTime,
                                           @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                           @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {
        try {
            return Result.ok(indexDemoService.adminOrders(status, beginTime, endTime, pageNo, pageSize));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 模拟：商品列表接口（阶段4：回表 vs 覆盖索引）
     * GET /api/index-demo/products?categoryId=37&mode=all|light&pageNo=1&pageSize=20
     * mode=all  ：SELECT *（需要回表取全列）
     * mode=light：只 SELECT 列表展示列（配合覆盖索引时不回表）
     */
    @GetMapping("/products")
    public Result<IndexDemoVO> products(@RequestParam("categoryId") Integer categoryId,
                                        @RequestParam(value = "mode", defaultValue = "light") String mode,
                                        @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        try {
            return Result.ok(indexDemoService.products(categoryId, mode, pageNo, pageSize));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 通用 EXPLAIN：对教程里任意一条 mall_* 表 SELECT 做执行计划分析
     * POST /api/index-demo/explain   body: {"sql": "select * from mall_order where status = 1"}
     * 只跑 EXPLAIN、不真正执行业务 SELECT，可以放心贴大查询。
     */
    @PostMapping("/explain")
    public Result<IndexDemoVO> explain(@Valid @RequestBody ExplainSqlRequest request) {
        try {
            return Result.ok(indexDemoService.explain(request.getSql()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实验台：执行单条语句（SELECT/EXPLAIN/SHOW/ANALYZE = 查询；CREATE/DROP/ALTER INDEX = DDL）
     * POST /api/index-demo/run   body: {"sql": "create index idx_user_phone on mall_user(phone)"}
     * 只允许操作 mall_* 表上的索引，不能执行其它 DDL/DML。
     */
    @PostMapping("/run")
    public Result<SqlRunResultVO> run(@Valid @RequestBody SqlRunRequest request) {
        return Result.ok(indexDemoService.runSql(request.getSql()));
    }

    /**
     * 实验台：数据概览（三张表行数 / 当前索引 / 数据与索引体积 / 示例手机号）
     * GET /api/index-demo/state
     */
    @GetMapping("/state")
    public Result<Map<String, Object>> state() {
        try {
            return Result.ok(indexDemoService.state());
        } catch (Exception e) {
            return Result.error(friendly(e));
        }
    }

    /**
     * 实验台：一键删除本教程建过的所有二级索引（不删数据），回到初始态
     * POST /api/index-demo/reset-indexes   → 返回被删的语句列表
     */
    @PostMapping("/reset-indexes")
    public Result<List<String>> resetIndexes() {
        try {
            return Result.ok(indexDemoService.resetIndexes());
        } catch (Exception e) {
            return Result.error(friendly(e));
        }
    }

    /** 把“表不存在”翻译成对新手友好的提示 */
    private String friendly(Exception e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        if (m.contains("doesn't exist") || m.contains("does not exist")) {
            return "mall_* 表不存在：请先在 MySQL 执行 docs/mysql-index/sql/00_建表与造数.sql 完成建表+造数"
                    + "（详见 docs/mysql-index/00-准备与造数.md）";
        }
        return m;
    }
}
