package com.dev.springmybatissql.service;

import com.dev.springmybatissql.vo.IndexDemoVO;
import com.dev.springmybatissql.vo.SqlRunResultVO;

import java.util.List;
import java.util.Map;

/**
 * 索引实战演示服务（IndexDemo 模块专用）
 *
 * 一组“看起来就是真实后端接口”的查询模拟，供 docs/mysql-index 教程分阶段调用：
 * - myOrders   ：模拟“我的订单列表”接口（阶段3：联合索引）
 * - adminOrders：模拟“运营后台按状态+时间段查订单”接口（阶段6：慢SQL排查）
 * - products   ：模拟“商品列表”接口，mode=all 全列 vs mode=light 只取必要列（阶段4：回表与覆盖索引）
 * - userByPhone：模拟“按手机号查用户”接口（阶段1/2：第一次建索引）
 * - explain    ：对任意 mall_* 表 SELECT 跑 EXPLAIN（配合各阶段练习）
 */
public interface IndexDemoService {

    /**
     * 我的订单列表：WHERE user_id=? AND status=? ORDER BY create_time DESC
     */
    IndexDemoVO myOrders(Long userId, Integer status, int pageNo, int pageSize);

    /**
     * 运营后台订单列表：WHERE status=? AND create_time BETWEEN ? AND ? ORDER BY create_time DESC
     */
    IndexDemoVO adminOrders(Integer status, String beginTime, String endTime, int pageNo, int pageSize);

    /**
     * 商品列表：WHERE category_id=? AND status=1 ORDER BY price DESC
     * mode=all  -> SELECT *（会回表）
     * mode=light-> 只 SELECT 需要展示的列（配合覆盖索引可不回表）
     */
    IndexDemoVO products(Integer categoryId, String mode, int pageNo, int pageSize);

    /**
     * 按手机号精确查用户：WHERE phone = ?
     */
    IndexDemoVO userByPhone(String phone);

    /**
     * 对一条 mall_* 表上的 SELECT 执行 EXPLAIN（SQL 由用户从教程里复制）
     */
    IndexDemoVO explain(String sql);

    /**
     * 实验台：执行单条语句（SELECT/EXPLAIN/SHOW/ANALYZE = 查询；
     * CREATE/DROP/ALTER INDEX = DDL），带白名单安全校验。
     */
    SqlRunResultVO runSql(String sql);

    /**
     * 实验台：mall_* 三张表的“数据概览”
     * （行数 / 当前索引清单 / 数据与索引体积 / 一个真实手机号示例）
     */
    Map<String, Object> state();

    /**
     * 实验台：一键删除本教程建过的所有二级索引（不删数据），
     * 回到 mall_* 只有主键 + 业务唯一索引的初始态，返回被删的索引名列表。
     */
    List<String> resetIndexes();
}
