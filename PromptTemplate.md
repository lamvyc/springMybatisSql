你现在作为我的 Java 后端导师、数据库架构师和项目实战导师。
我已经使用 IDEA 创建好了一个 Spring Boot 项目。
请帮助我逐步完善一个：
Spring Boot + MyBatis + MyBatis Plus + SQL 综合训练项目。
一、我的学习背景
我的情况：
已经学习 Java 基础。
已经学习 Spring Boot 基础。
已经学习 SQL。
目前已经掌握：
单表查询
多表查询
JOIN
GROUP BY
HAVING
聚合查询
子查询
已经学习 MyBatis。
了解：
Mapper接口
XML配置
参数传递
ResultMap
动态SQL
但是：
目前只是跟着代码学习和添加注释理解。
还没有形成完整开发链路：
业务需求 → 数据库设计 → SQL → Mapper → Service → Controller → 接口返回
已经接触 MyBatis Plus。
但是没有完全理解：
MyBatis 和 MyBatis Plus 的关系。
什么时候使用 MyBatis。
什么时候使用 MyBatis Plus。
二、项目目标
这个项目不是简单 CRUD 项目。
核心目标：
通过真实项目建立：
业务需求 → 数据库模型 → SQL设计 → MyBatis实现 → Spring Boot接口 完整认知。
同时掌握：
SQL能力
包括：
查询
统计
多表关联
排名
分析
优化
MyBatis能力
理解：
Mapper作用
XML作用
参数映射
ResultMap
动态SQL
执行流程
MyBatis Plus能力
理解：
解决什么问题
和MyBatis区别
企业中如何结合使用
AI代码审查能力
能够判断：
SQL是否合理
查询是否存在性能问题
Mapper设计是否合理
后端代码结构是否正确
三、项目定位
项目名称：sql-learning-platform
项目类型：企业后台数据管理与分析系统。模拟互联网企业后台。
业务模块：
用户中心
员工组织中心
商品中心
订单中心
权限中心
数据统计中心
SQL实验中心
注意：不要设计成传统 emp/dept/salgrade 教学项目。需要将经典SQL题迁移到现代业务场景。
例如：
经典题：查询每个部门最高工资员工。
转换：
业务：查询每个部门薪资最高员工。
数据模型：employee、department
保留SQL思想：group by、max、join
四、技术栈
技术：Spring Boot 3.x、Java 17+、Maven、MySQL 8、MyBatis、MyBatis Plus、Lombok
五、MyBatis 与 MyBatis Plus 学习策略
不要一开始混合使用。分阶段学习。
阶段一：主要使用原生 MyBatis。
目的：建立完整调用链。
重点：Controller → Service → Mapper → Mapper XML → SQL → MySQL
覆盖：单表查询、多表查询、动态SQL、ResultMap、参数映射、复杂查询
阶段二：引入 MyBatis Plus。
目的：理解企业开发效率工具。
使用场景：简单CRUD。
例如：用户管理（新增用户、修改用户、删除用户、根据ID查询、分页查询）
要求：同一个业务功能，分别实现 MyBatis版本 VS MyBatis Plus版本
对比：代码量、SQL控制、适用场景
阶段三：复杂业务查询继续使用MyBatis。
例如：报表统计、Top N、排名、多表聚合、数据分析
六、数据库设计要求
设计完整企业数据库。至少包含：
用户模块：user
字段：id、username、email、phone、status、create_time
支持：用户查询、条件搜索、分页、注册统计
组织员工模块：department
字段：id、dept_name、parent_id
employee
字段：id、name、dept_id、leader_id、salary、position、hire_date
支持：部门查询、员工查询、领导关系、薪资统计
商品模块：product
字段：id、name、category_id、price、stock、status、create_time
支持：商品查询、商品排行、库存分析
订单模块：orders
字段：id、user_id、order_no、total_amount、status、create_time
order_item
字段：id、order_id、product_id、quantity、price
支持：订单查询、销售统计、商品销量分析
权限模块：role
字段：id、name
user_role
字段：user_id、role_id
支持：多对多查询。
行为日志模块：operation_log
字段：id、user_id、operation、create_time
支持：用户行为分析。
学生选课模块：student、course、student_course
用于练习：多对多、exists、not exists、having
七、经典SQL35题迁移
我会额外提供35道经典SQL题。
要求：不要直接复制原题表结构。需要迁移成现代业务场景。
每一道题生成：
业务场景
对应数据模型
SQL思路
标准SQL答案
MyBatis实现
接口调用
面试表达
格式：
【业务需求】xxx
【数据关系】xxx
【SQL分析】第一步：第二步：第三步：
【核心知识】join、group by、having
【面试表达】xxx
八、现代业务SQL扩展（重点）
经典35题主要覆盖基础SQL。项目必须额外增加现代互联网业务SQL。至少包含以下类型：
分页查询
场景：后台管理列表。包括：用户分页、订单分页、商品分页
要求理解：limit、offset、MyBatis分页参数。
同时说明：大数据量分页问题。包括：offset分页缺陷、游标分页(seek pagination)思想。
连续问题分析
增加：用户连续登录、连续签到、连续购买。
学习窗口函数：row_number()、lag()、lead()
案例：查询连续登录7天用户、查询连续购买3天用户。
解释：如何处理连续日期分组。
Top N 分组问题
增加：每个分类销量前三商品、每个部门工资最高员工、每个月销售额TOP5。
要求学习：row_number()、rank()、dense_rank()
解释：为什么limit无法解决分组Top N问题。
慢SQL分析与优化
增加数据库性能模块。包含：慢SQL案例。
使用 EXPLAIN 分析：type、key、rows、extra
学习：索引设计、联合索引、索引失效场景。
例如：where字段计算、隐式类型转换、like前缀问题
九、SQL实验中心（重点）
项目必须包含：SQL实验中心。
目标：让我自己练习SQL。
流程：查看题目 → 自己编写SQL → 项目执行 → 查看结果 → 对比标准答案
不要让我通过聊天提交SQL。直接在项目中验证。
十、SQL实验中心设计
增加：sql_case表
字段：id、title、description、difficulty、knowledge_point、standard_sql、expected_result
说明：standard_sql保存标准SQL答案。expected_result保存正确结果。
十一、SQL执行方式
提供两种方式。
方式1：控制台执行。输入caseId，执行SQL，输出查询结果。
方式2：简单前端页面SQL实验台。
页面：左侧题目列表、中间SQL编辑器、右侧结果展示。
展示：查询结果、执行时间、错误信息。
十二、SQL验证机制
执行我的SQL后，自动比较：字段数量、数据行数量、数据内容。
输出：正确显示结果一致。错误显示差异。
十三、项目代码结构
采用标准Spring Boot结构：controller、service、service.impl、mapper、entity、dto、vo、config。
resources:mapper
解释每层作用。
十四、开发阶段
不要一次生成完整项目代码。按照阶段输出。
阶段1：项目基础配置。包括：pom检查、application.yml、数据库连接、MyBatis配置、项目目录。等待确认。
阶段2：数据库设计。包括：建表SQL、初始化数据、表关系。等待确认。
阶段3：SQL实验中心设计。包括：题目管理、SQL执行、结果比较。等待确认。
阶段4：MyBatis基础业务开发。使用原生MyBatis。完成完整调用链。等待确认。
阶段5：经典SQL35题迁移。转换为现代业务SQL。等待确认。
阶段6：现代业务SQL扩展。包含：分页、连续问题、Top N、慢SQL优化。等待确认。
阶段7：MyBatis Plus学习。实现CRUD功能。并对比：MyBatis VS MyBatis Plus。等待确认。
阶段8：项目总结。输出：SQL知识地图、MyBatis知识地图、MyBatis Plus知识地图、面试总结。
最终目标：
完成项目后，我应该能够：
根据业务需求设计SQL。
理解MyBatis完整执行流程。
理解MyBatis Plus适用场景。
解决真实业务查询问题。
分析SQL性能问题。
审查AI生成Java后端代码。
请始终以：真实开发、面试准备、能力提升 为设计原则。
不要为了复杂而复杂。优先保证：可运行、可理解、可复习。