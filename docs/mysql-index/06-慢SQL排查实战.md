# 阶段 6：慢 SQL 排查实战（模拟线上事故）

> 这是整套教程的毕业演练，也是文档 `项目总结.md【## 五、慢 SQL 分析与优化（阶段6）】`
> 的实战深化版。
>
> 配套脚本：`sql/06_慢SQL排查实战.sql`；业务接口：`GET /api/index-demo/admin-orders?...`

## 6.1 事故回放（完全真实的线上剧本）

**周一 10:05，运营在群里 @你**：
> “后台导出《6 月已支付订单》，页面一直转圈，5 分钟都出不来！以前很快的！”

**你查了版本记录**：订单表这周刚上过新需求，订单量也从 30 万涨到了 100 万。
你打开慢查询日志，抓到了这条 SQL：

```sql
SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;
```

**老接口“我的订单”一直正常**——它长这样：

```sql
SELECT ... FROM mall_order
WHERE user_id = ? AND status = ?        -- 注意：带 user_id
ORDER BY create_time DESC LIMIT 20;
```

## 6.2 排查第 1 步：先看线上有什么索引

```sql
SHOW INDEX FROM mall_order;
```

线上的订单表只有：`PRIMARY`、`uk_order_no`（订单号唯一）、
`idx_user_status_ct (user_id, status, create_time)`（为“我的订单”建的）。

## 6.3 排查第 2 步：EXPLAIN 慢 SQL（别猜，看执行计划）

```sql
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;
```

真实输出（关键列）：

```
type = ALL    key = NULL    rows = 996250
Extra = Using where; Using filesort
```

**看懂这张表，事故就破案一半了：**

| EXPLAIN | 说明 |
|---------|------|
| `type = ALL` | 在扫 100 万行全表 |
| `rows ≈ 996250` | 预估扫描 99.6 万行 |
| `Extra = Using filesort` | 扫完还要整体排序 |
| `key = NULL` | 一个索引都没用上 |

**耗时佐证**（本机真实，接口 5 次）：`273 / 240 / 236 / 240 / 242 ms`。

**慢日志里它是这样被记录的**（真实行）：

```
# Query_time: 0.233415  Lock_time: 0.000002  Rows_sent: 50  Rows_examined: 1000050
SELECT id, ... FROM mall_order WHERE status = 1 AND create_time BETWEEN ... LIMIT 50;
```

注意 `Rows_examined = 1000050`：为了 50 行结果，翻了 100 万行。
（怎么开慢日志见脚本注释：`SET GLOBAL slow_query_log=ON; SET GLOBAL long_query_time=0.1;`）

## 6.4 定位根因：索引与 SQL 形态不匹配

把两件事放在一起看：

```
慢 SQL 的形态：  WHERE status=? AND create_time BETWEEN ?   ORDER BY create_time DESC
现有索引列序：   (user_id, status, create_time)

对照阶段3学的最左前缀：(user_id, status, create_time) 必须从 user_id 用起
而这条 SQL 根本没有 user_id → 整个联合索引用不上 → 全表扫
```

**结论**：不是数据量的问题，不是硬件的问题，是 **“查询形态”和“索引列序”对不上**。
这也是生产上最常见的索引问题——索引是给旧接口设计的，新需求换了查询条件，
索引没跟上。

**顺带自证**：老接口“我的订单”为什么没坏？

```sql
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC LIMIT 20;
-- type = ref, key = idx_user_status_ct —— user_id 在最左列，索引照常工作
```

## 6.5 修复：为“状态+时间”这个真实形态单独建索引

```sql
CREATE INDEX idx_order_status_ct ON mall_order (status, create_time);
```

**为什么是这两列、这个顺序？** 回看阶段 3 的列序规则：
等值列 `status = ?` 在前，排序列 `create_time` 放最后一位 ——
这样索引内部“同状态段内 create_time 有序”，倒着读索引就能满足
`ORDER BY create_time DESC`，连 filesort 都省了。

## 6.6 再 EXPLAIN（同一句慢 SQL）

```sql
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE status = 1
  AND create_time BETWEEN '2024-06-01 00:00:00' AND '2024-06-30 23:59:59'
ORDER BY create_time DESC
LIMIT 50;
```

真实输出（关键列）：

```
type = range   key = idx_order_status_ct   rows ≈ 39158
Extra = Using index condition; Backward index scan
```

- `type=ALL → range`：从全表扫变成索引范围扫；
- `rows 996250 → 39158`：扫描量降了两个数量级；
- `filesort` 消失：create_time 排序交给索引（Backward index scan = 倒着读索引）。

## 6.7 验证优化效果（真实数据对比表）

| 指标 | 修复前 | 修复后 |
|------|--------|--------|
| EXPLAIN type | ALL | range |
| EXPLAIN rows | ≈ 996250 | ≈ 39158 |
| Extra | Using where; Using filesort | Using index condition; Backward index scan |
| 客户端耗时（5 次） | 273/240/236/240/242 ms | 27/23/20/17/17 ms |
| 慢日志 Query_time | 0.233415 s | 0.007312 s（约 **32 倍**提升） |
| 慢日志 Rows_examined | 1000050 | **50** |

> `Rows_examined 1000050 → 50` 是最有说服力的证据：为了 50 条结果，
> 现在真的只碰了 50 行（索引范围从最新往旧读，命中 50 条即停）。

## 6.8 回归：确认其它接口没被影响

```sql
SHOW INDEX FROM mall_order;
-- 现在应有：PRIMARY、uk_order_no、
--          idx_user_status_ct（我的订单用）、idx_order_status_ct（本次新增，后台用）

-- “我的订单”仍走自己的索引：
EXPLAIN SELECT id, order_no, user_id, status, pay_amount, create_time
FROM mall_order
WHERE user_id = 29997 AND status = 1
ORDER BY create_time DESC LIMIT 20;
-- type = ref, key = idx_user_status_ct
```

两个联合索引服务于**两种不同的查询形态**，互不干扰，这就是正确姿势
（别试图用一个索引包打天下，也别建一堆冗余重复的索引）。

## 6.9 用项目接口再走一遍（把“排查”变成日常习惯）

启动应用后模拟这个事故接口：

```bash
# 修复前（先把 idx_order_status_ct 删掉模拟线上现状）：
curl "http://localhost:8080/api/index-demo/admin-orders?status=1&beginTime=2024-06-01&endTime=2024-06-30&pageNo=1&pageSize=50"
# 返回里的 explainSummary ≈ type=ALL key=NULL rows=996250 ...，elapsedMs ≈ 200+

# 执行 CREATE INDEX idx_order_status_ct ... 后再调一次：
# explainSummary ≈ type=range key=idx_order_status_ct rows=39158 ...，elapsedMs ≈ 20
```

## 6.10 复盘：这套排查流程，背下来

```
出事后（企业流程）
  1. 复现 + 抓慢 SQL       （看慢日志 / 应用日志 / 监控里的 SQL）
  2. EXPLAIN 慢 SQL        （盯 type / key / rows / Extra）
  3. 定位根因               （没索引？还是索引没对上 SQL 形态？对号入座阶段5五类失效）
  4. 设计修复               （改 SQL 优先；确实高频的形态再补索引，用最左前缀反推列序）
  5. 再 EXPLAIN            （rows 是否数量级下降、filesort 是否消失）
  6. 压测 / 看慢日志         （对比 Query_time 与 Rows_examined）
  7. 回归其它查询           （SHOW INDEX + 相邻接口各 EXPLAIN 一遍）
```

**面试版本（背这个就够）**：
> “先抓慢 SQL，EXPLAIN 看 type/key/rows/Extra 判断是没索引还是索引不匹配；
> 用最左前缀反推正确的索引列序，或改写 SQL；建/改完再 EXPLAIN 对比 rows，
> 结合慢日志的 Rows_examined 验证；最后回归线上其它查询不受影响。”

## 6.11 拓展：这次优化“毕业”了，下次还可能遇到什么？

- **深分页**：`LIMIT 200000, 50` 即使走了索引，也要先翻过 20 万条索引记录。换“游标分页”
  （`WHERE (status,create_time) < (?,?) ORDER BY ... LIMIT 50`，拿上一页最后一条继续翻）。
- **大结果排序**：能进索引的排序列进索引；实在排不动再考虑汇总表/数仓。
- **SELECT 列裁剪**：把需要的列写全，给覆盖索引留机会（阶段 4）。
- **大表加索引的 DDL 窗口**：100 万行加索引约十几秒~分钟级；生产上要用在线 DDL /
  低峰期执行 / 变更平台，别在业务高峰期直接跑（本教程数据量小，直接跑没问题）。

## 6.12 阶段小结

**我学会了什么**
- 一套完整的企业级慢 SQL 排查流程（复现 → EXPLAIN → 定位 → 修复 → 验证 → 回归）；
- “接口突然变慢”最常见根因：索引是旧查询形态设计的，新 SQL 形态用不上它；
- EXPLAIN 四字段 + 慢日志 Query_time / Rows_examined 是判断与验证的双重证据。

**日常开发怎么用**
- 上线带新查询条件的代码前，先 EXPLAIN 一遍、再评估索引是否匹配；
- 每个索引在代码注释/索引注释里写明“服务哪个查询形态”，防止后来人误删/乱加；
- 收到慢 SQL 告警：先看 rows 数量级与 filesort，再决定改 SQL 还是补索引。

**面试怎么答**
- Q：线上订单查询突然变慢怎么排查？→ 复现抓 SQL → EXPLAIN → 看 type/key/rows/Extra →
  判断索引缺失还是形态不匹配 → 改 SQL 或按最左前缀补索引 → 再 EXPLAIN → 回归。
- Q：什么时候加索引、什么时候改 SQL？→ SQL 能改且语义不变优先改 SQL；
  高频查询形态确实需要时补索引，列序按等值在前、范围/排序在后。

**一句话记忆锚点**：`慢 SQL 先 EXPLAIN：type=ALL 有 filesort → 索引没对上 SQL 形态；让索引匹配查询，用 rows 与 Rows_examined 验收。`

→ 最后一步：[07-索引实战速查表.md](07-索引实战速查表.md)
