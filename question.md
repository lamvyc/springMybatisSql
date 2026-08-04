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