# MySQL 索引实战教程（基于本项目「阶段6：慢 SQL 分析与优化」扩展）

> 从 0 开始、在企业级 Java + Spring Boot + MyBatis + MySQL 工程里学会 MySQL 索引。
> 不背概念：亲手经历 **没有索引 → 查询 → EXPLAIN → 建索引 → 再查询 → EXPLAIN 对比 → 理解索引 → 设计联合索引 → 优化真实 SQL**。

---

## 一、这个教程是什么

本目录是仓库 `docs/项目总结.md` 中【## 五、慢 SQL 分析与优化（阶段6）】小节的**实战深化版**：

| 项目总结.md【## 五】的速记 | 本教程                        | 
|------------------|----------------------------|
| EXPLAIN 四字段速查    | 阶段1：第一次使用索引（把四字段看懂）        |
| 联合索引最左前缀         | 阶段3：联合索引（亲手从单列升级到三列）       |
| 回表 / 覆盖（一句话带过）   | 阶段4：回表与覆盖索引（看 Using index） |
| 索引失效 4 条         | 阶段5：索引失效（每条都亲手制造再修复）       |
| （无实战步骤）          | 阶段6：慢 SQL 排查实战（完整线上事故演练）   |

**它没有改动原项目任何已有代码**，只新增：
1. `docs/mysql-index/` —— 本套教程（文档 + 可执行 SQL 脚本）
2. 一张隔离的业务表集 `mall_user / mall_product / mall_order`（百万级数据，前缀 `mall_` 与原有表互不影响）
3. 一个隔离的演示接口模块 `/api/index-demo/*`（模拟真实业务调用）

---

## 二、知识地图与学习顺序

```
阶段0 准备：建表 + 造数(30万用户/20万商品/100万订单)      【必做，约10分钟】
   │
   ▼
阶段1 第一次使用索引   L1  必须掌握    索引生效的完整证据链
   │
   ▼
阶段2 单列索引         L1  必须掌握    什么字段值得建？主键/二级/唯一；为什么不是越多越好
   │
   ▼
阶段3 联合索引         L1  必须掌握    列序设计 + 最左前缀 + WHERE/ORDER BY 利用
   │
   ▼
阶段4 回表与覆盖索引    L1  必须掌握    Using index 判断、SELECT * 的代价
   │
   ▼
阶段5 索引失效         L1  必须掌握    5 大失效场景亲手制造 + 修复
   │
   ▼
阶段6 慢 SQL 排查实战   L1  综合演练    线上“订单接口突然变慢”完整排查流程
   │
   ▼
速查表（阶段7）        L2  随手查
```

每个阶段都遵循同一个学习闭环：

```
问题(真实业务SQL)
   → 操作(执行SQL/建索引)
   → 结果(真实 EXPLAIN + 耗时)
   → 原因(为什么)
   → 对比(前后两张 EXPLAIN)
   → 小结(学会了什么 / 日常怎么用 / 面试怎么答)
```

> 文中的 EXPLAIN 结果、耗时、Rows_examined **全部来自本机 MySQL 8.0 的真实执行**。
> 你的机器结果可能因随机造数略有不同（耗时差几十毫秒正常），但 **type/key/rows/Extra 的形态变化完全一致**。

---

## 三、三种“动手”方式（任选）

| 方式                  | 怎么用                                                                                               | 适合                              |
|---------------------|---------------------------------------------------------------------------------------------------|---------------------------------|
| **A. 跑 SQL 脚本**     | `mysql -h127.0.0.1 -uroot -proot1234 sql_learning_platform < docs/mysql-index/sql/01_第一次使用索引.sql` | 想一口气把该阶段跑完                      |
| **B. 交互式一段段执行**（推荐） | 打开 Navicat / DataGrip / mysql 客户端，从每个文档里复制 SQL 一段段执行                                              | 想看每一步的中间结果                      |
| **C. 项目 HTTP 接口**   | 启动 Spring Boot，调 `/api/index-demo/*` 模拟真实业务调用（内部自动带 EXPLAIN）                                      | 想体会“真实接口慢 → 抓 SQL → EXPLAIN”的链路 |
| **D. 浏览器互动验证台**（推荐新人从这里开始） | 启动 Spring Boot 后打开 `http://localhost:8080/index-learning.html`                                          | 不想碰命令行：左侧数据/索引现状 + 右侧按阶段“点按钮跑 SQL” |

> 互动验证台把 `docs/mysql-index/sql/*.sql` 搬进了浏览器：每阶段带“一键回到初始态”，
> SQL 可直接改参数重跑，结果区会高亮 `type/key/rows/Extra`。
> 首次打开建议从 **阶段01** 开始，按卡片从上到下点 ▶ 运行。

接口清单（在 `src/main/java/com/dev/springmybatissql/controller/IndexDemoController.java`）：

```
GET  /api/index-demo/user/by-phone?phone=xxx      # 阶段1/2 按手机号查用户
GET  /api/index-demo/my-orders?userId=29997&status=1&pageNo=1&pageSize=20   # 阶段3 “我的订单”
GET  /api/index-demo/admin-orders?status=1&beginTime=2024-06-01&endTime=2024-06-30   # 阶段6 后台订单
GET  /api/index-demo/products?categoryId=37&mode=all|light&pageNo=1&pageSize=20      # 阶段4 商品列表(回表/覆盖)
POST /api/index-demo/explain    body: {"sql":"select ..."}   # 任意 mall_* 查询的 EXPLAIN
POST /api/index-demo/run        body: {"sql":"create index ..."}  # 互动台：执行 SELECT/EXPLAIN/建删索引
GET  /api/index-demo/state      # 互动台：数据概览（行数/索引/体积/示例手机号）
POST /api/index-demo/reset-indexes   # 互动台：一键回到“无二级索引”初始态
```

每个接口返回统一结构（`IndexDemoVO`）：

```json
{
  "demo": "adminOrders",
  "sql": "SELECT id, ... FROM mall_order WHERE status = 1 AND ... LIMIT 0, 50",
  "elapsedMs": 240,            // 业务 SQL 真实执行耗时
  "rows": [...],               // 业务数据
  "explainRows": [...],        // EXPLAIN 输出
  "explainSummary": "type=ALL key=NULL rows=996250 extra=Using where; Using filesort"
}
```

---

## 四、文件清单

```
docs/mysql-index/
├── README.md                      本文件（地图 + 使用方法）
├── 00-准备与造数.md                建表 + 造数说明
├── 01-第一次使用索引.md
├── 02-单列索引.md
├── 03-联合索引.md
├── 04-回表与覆盖索引.md
├── 05-索引失效.md
├── 06-慢SQL排查实战.md
├── 07-索引实战速查表.md
└── sql/
    ├── 00_建表与造数.sql           建 mall_* 三张表 + 造 30万/20万/100万 行（可调参）
    ├── 01_第一次使用索引.sql
    ├── 02_单列索引.sql
    ├── 03_联合索引.sql
    ├── 04_回表与覆盖索引.sql
    ├── 05_索引失效.sql
    └── 06_慢SQL排查实战.sql
```

配套 Java 演示模块（新增、隔离）：
```
controller/IndexDemoController.java     模拟业务接口 + 实验台 /run /state /reset-indexes
service/IndexDemoService(Impl).java     拼 SQL + 执行 + EXPLAIN + 单语句白名单执行
mapper/IndexDemoMapper.java             动态执行（同 SqlExecutorMapper 模式）
vo/IndexDemoVO.java / SqlRunResultVO.java   业务调用结果 / 实验台单语句结果
dto/ExplainSqlRequest.java / SqlRunRequest.java
```
前端验证台：
```
src/main/resources/static/index-learning.html   互动验证台（浏览器打开 /index-learning.html）
```

---

## 五、常见问题

- **我不想生成 100 万行？** 改 `00_建表与造数.sql` 顶部三个 `@变量`。建议至少：用户 10 万、商品 2 万、订单 30 万，太小就看不到“全表扫”和“索引”的差别。
- **顺序执行某阶段报“索引已存在”？** 每个阶段脚本开头都有“幂等重置”段（把教程二级索引清空、不删数据），从阶段脚本**开头整体执行**即可，不需要手动清理。
- **手机号对不上？** 造数是随机的，先执行 `SELECT phone FROM mall_user ORDER BY id LIMIT 5;` 取一个真实号码替换占位符。
- **这些 mall_* 表会不会影响原 SQL 实验台？** 不会。表名、前缀均独立，原 22 道题只查原有表。
- **耗时怎么忽快忽慢？** 客户端耗时含“连接 MySQL”的固定开销（约 15~20ms）；想测服务器真实时间，用第六阶段的慢日志（Query_time / Rows_examined）。

开始吧 → [00-准备与造数.md](00-准备与造数.md)
