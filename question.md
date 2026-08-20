# SQL 实验中心题干更新不生效问题排查与解决

## 问题描述

修改了 `SqlCaseInitializer.java` 中每道题的 `description`（题干）内容，但前端 SQL 实验中心页面仍然显示修改前的内容。

## 根因分析

### 原因1：幂等跳过（主要原因）

`SqlCaseInitializer` 启动时会检查 `sql_case` 表是否已有数据：

```java
Long count = sqlCaseMapper.selectCount(new QueryWrapper<>());
if (count != null && count > 0) {
    log.info("SQL 实验中心：sql_case 已有 {} 道题，跳过初始化", count);
    return;
}
```

`sql_case` 表已有旧数据，初始化器直接跳过，不会使用新代码重新生成 `description`。

### 原因2：枚举初始化顺序（次要原因）

重构为 `SqlCaseEnum` 枚举时，若在构造方法中直接调用 `buildDescription()` 引用 `TABLE_SCHEMAS` 静态 Map，会触发 `ExceptionInInitializerError`。

Java 枚举类加载顺序：枚举常量 → 静态字段/静态块。枚举常量构造时 `TABLE_SCHEMAS` 尚未初始化。

## 解决方案

### 步骤1：清空 sql_case 表

```sql
TRUNCATE TABLE sql_case;
```

### 步骤2：修复枚举初始化顺序（如适用）

将 `description` 从构造时拼接改为 `getDescription()` 懒计算：

```java
// ❌ 错误：构造时直接拼接
SqlCaseEnum(...) {
    this.description = buildDescription(tableKeys, ...);  // TABLE_SCHEMAS 还是 null
}

// ✅ 正确：getter 中懒计算
public String getDescription() {
    // TABLE_SCHEMAS 此时已初始化完毕
    return buildDescription(tableKeys, ...);
}
```

### 步骤3：重启应用

重启 Spring Boot 应用，`SqlCaseInitializer` 检测到表为空，使用新代码重新初始化所有题目。

## 后续维护注意事项

- 修改 `SqlCaseEnum` 中任何题目的 `description` 内容后，需重新 TRUNCATE + 重启才能生效。
- 如需频繁调试题干内容，可考虑将初始化策略从"幂等跳过"改为"覆盖更新"（按 id upsert）。
- `SqlCaseInitializer.buildCases()` 已简化为遍历 `SqlCaseEnum.values()`，新增题目只需在枚举中加一个常量即可。

---

# SQL 实验中心间歇性 500 错误（No database selected）

## 问题描述

项目启动后 `/api/sql/cases` 接口正常，运行一段时间后返回 500 错误：

```json
{
    "timestamp": "2026-08-04T09:19:16.435+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/sql/cases"
}
```

重启应用后恢复，IDEA Database 直接查 MySQL 数据一直正常。

## 日志关键信息

```
java.sql.SQLException: No database selected
SQL: SELECT id,title,description,... FROM sql_case ORDER BY id ASC
Cause: java.sql.SQLException: No database selected; SQL state [3D000]; error code [1046]
```

## 根因分析

**JDBC URL 没有指定数据库名。**

```yaml
# ❌ 修复前：URL 只到端口，没有数据库名
url: jdbc:mysql://localhost:3306/?useUnicode=true&...
```

启动时 `spring.sql.init` 通过 `schema.sql` 中的 `USE sql_learning_platform` 给初始连接设置了数据库上下文，连接似乎正常。但 HikariCP 连接池在运行中回收/重建连接时，新连接只走 JDBC URL 初始化——URL 里没有库名，新连接便不知道要选哪个数据库。

之前错误地加了 `connection-test-query: SELECT 1`，`SELECT 1` 不需要选库也能成功，所以 HikariCP 以为连接健康。直到 MyBatis 执行 `SELECT ... FROM sql_case` 时才暴露 `No database selected`。这就是"启动正常、过一段时间 500"的时间窗口原因。

## 解决方案

JDBC URL 中直接指定数据库名：

```yaml
# ✅ 修复后
url: jdbc:mysql://localhost:3306/sql_learning_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

无论连接池如何回收重建，每个新连接都知道目标库是 `sql_learning_platform`。

---

# SQL 实验中心：格式化 SQL 报"仅允许执行 SELECT 查询语句"

## 问题描述

SQL 实验中心页面执行查询时，**只要把 SQL 格式化（换行/缩进）就报错**：

```
执行失败：实验中心仅允许执行 SELECT 查询语句
```

同样的 SQL 写成单行就能正常执行，格式化后（多行）反而被拒。

## 根因分析

### 原因1（直接原因）：校验正则不匹配换行符

`SqlCaseServiceImpl.java` 中的 SELECT 白名单校验：

```java
// ❌ 修复前
private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select\\s+.*", Pattern.CASE_INSENSITIVE);
```

问题在于 Java 正则：

- `.` 默认**不匹配换行符** `\n`
- `Matcher.matches()` 要求**整个字符串**完全匹配

SQL 一格式化就产生换行，例如：

```sql
select name,
       salary
from   employee
```

`.*` 匹配到 `select name,` 之后的 `\n` 就停止了，整个字符串匹配失败 → 抛"实验中心仅允许执行 SELECT 查询语句"。

### 原因2（"改了还报错"的坑）：应用未重启，JVM 里跑的还是旧 class

修改代码并重新编译后，用户侧仍报同样的错误。排查发现：

- 运行中的应用进程是 17:21 启动的
- 修复代码 17:35 才编译到 `target/classes`
- **JVM 加载过的类不会自动重新读取**，不重启应用就永远执行旧逻辑

用 curl 实测证实：

```bash
# 旧实例（未重启）→ 格式化 SQL 依旧报错
{"success":false,"errorMessage":"实验中心仅允许执行 SELECT 查询语句"}

# 停止旧实例、用新代码重启后 → 同样的 SQL 立即通过
{"success":true}
```

## 解决方案

在 SELECT 白名单正则上添加 `Pattern.DOTALL` 标志，让 `.` 也能匹配换行符：

```java
// ✅ 修复后
private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*select\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
```

修改后**必须重启应用**才能生效（无 devtools 热部署时尤其注意）。

## 验证

curl 实测 `/api/sql/execute`：

| 场景 | 结果 |
|------|------|
| 格式化 SQL（多行） | ✅ `success: true` |
| 单行 SQL | ✅ `success: true`（无回归） |
| `select ...; \ndrop table user` 注入 | ✅ 沙箱仍拦截 `drop` |
| `insert ...` 非 SELECT | ✅ 仍被拒绝 |

安全性不受影响：DOTALL 只是让 SELECT 校验通过换行，沙箱的黑名单关键字检测（防多语句注入）依然生效。

## 排查方法论

1. **jshell 单测正则**：先脱离应用直接验证 `Pattern.matches()` 行为，确认是换行导致匹配失败
2. **javap 反编译确认产物**：`javap -c` 查看编译产物，确认 flags 为 `bipush 34`（2=CASE_INSENSITIVE + 32=DOTALL），证明新代码已编译
3. **对比进程时间**：`ps aux` 查明应用启动时间（17:21）早于修复编译时间（17:35），锁定"未重启"根因
4. **curl 黑盒复现/回归**：重启前后各打一次接口，用同一段格式化 SQL 对比，快速验证修复是否真实生效
