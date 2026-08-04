## 经典SQL 35题（纯题目版）

### 数据模型

本项目将经典 emp/dept/salgrade 教学模型迁移到现代互联网业务场景，对应关系如下：

| 经典表 | 本项目表 | 说明 |
|--------|----------|------|
| emp | **employee** | 员工表 |
| dept | **department** | 部门表 |
| salgrade | **salary_grade** | 薪资等级表 |
| S/C/SC | **student / course / student_course** | 学生选课表 |

#### employee（员工表）
| 列名 | 类型 | 说明 | 经典对应 |
|------|------|------|----------|
| id | BIGINT | 员工ID（PK） | empno |
| name | VARCHAR(50) | 员工姓名 | ename |
| dept_id | BIGINT | 所属部门ID | deptno |
| leader_id | BIGINT | 直属领导ID，指向 employee.id | mgr |
| salary | DECIMAL(10,2) | 月薪 | sal |
| position | VARCHAR(50) | 职位 | job |
| hire_date | DATE | 入职日期 | hiredate |

#### department（部门表）
| 列名 | 类型 | 说明 | 经典对应 |
|------|------|------|----------|
| id | BIGINT | 部门ID（PK） | deptno |
| dept_name | VARCHAR(50) | 部门名称 | dname |
| dept_code | VARCHAR(20) | 部门编码（如 TECH/MKT/SALES） | — |
| parent_id | BIGINT | 上级部门ID | — |

#### salary_grade（薪资等级表）
| 列名 | 类型 | 说明 | 经典对应 |
|------|------|------|----------|
| grade | INT | 等级（PK） | grade |
| low_salary | DECIMAL(10,2) | 该等级最低薪资 | losal |
| high_salary | DECIMAL(10,2) | 该等级最高薪资 | hisal |

#### student / course / student_course（学生选课模块，第35题专用）
| 表 | 列 |
|----|-----|
| student | id(PK), name, age |
| course | id(PK), name, teacher |
| student_course | student_id(PK), course_id(PK), score |

---

**第1题**：取得每个部门最高薪水的人员名称

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.salary；department.id、department.dept_name

---

**第2题**：哪些人的薪水在部门的平均薪水之上

【相关表】employee
【相关列】employee.id、employee.name、employee.dept_id、employee.salary

---

**第3题**：取得每个部门平均薪水的等级

【相关表】employee、salary_grade、department
【相关列】employee.dept_id、employee.salary；salary_grade.grade、salary_grade.low_salary、salary_grade.high_salary；department.id、department.dept_name

---

**第4题**：取得部门中所有人的平均薪水等级

【相关表】employee、salary_grade、department
【相关列】employee.dept_id、employee.salary；salary_grade.grade、salary_grade.low_salary、salary_grade.high_salary；department.id、department.dept_name

---

**第5题**：不准用组函数（Max），取得最高薪水（给出两种解决方案）

【相关表】employee
【相关列】employee.id、employee.name、employee.salary

---

**第6题**：取得平均薪水最高的部门的部门编号（至少给出两种解决方案）

【相关表】employee、department
【相关列】employee.dept_id、employee.salary；department.id

---

**第7题**：取得平均薪水最高的部门的部门名称

【相关表】employee、department
【相关列】employee.dept_id、employee.salary；department.id、department.dept_name

---

**第8题**：求平均薪水的等级最低的部门的部门名称

【相关表】employee、department、salary_grade
【相关列】employee.dept_id、employee.salary；department.id、department.dept_name；salary_grade.grade、salary_grade.low_salary、salary_grade.high_salary

---

**第9题**：取得比普通员工（员工代码没有在 leader_id 字段上出现的）的最高薪水还要高的领导人姓名

【相关表】employee
【相关列】employee.id、employee.name、employee.leader_id、employee.salary

---

**第10题**：取得薪水最高的前五名员工

【相关表】employee
【相关列】employee.id、employee.name、employee.salary

---

**第11题**：取得薪水最高的第六到第十名员工

【相关表】employee
【相关列】employee.id、employee.name、employee.salary

---

**第12题**：取得最后入职的5名员工

【相关表】employee
【相关列】employee.id、employee.name、employee.hire_date

---

**第13题**：取得每个薪水等级有多少员工

【相关表】employee、salary_grade
【相关列】employee.salary；salary_grade.grade、salary_grade.low_salary、salary_grade.high_salary

---

**第14题**：列出所有员工及领导的姓名

【相关表】employee
【相关列】employee.id、employee.name、employee.leader_id

---

**第15题**：列出受雇日期早于其直接上级的所有员工的编号、姓名、部门名称

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.leader_id、employee.hire_date；department.id、department.dept_name

---

**第16题**：列出部门名称和这些部门的员工信息，同时列出那些没有员工的部门

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.salary、employee.position、employee.hire_date；department.id、department.dept_name

---

**第17题**：列出至少有5个员工的所有部门

【相关表】employee、department
【相关列】employee.dept_id；department.id、department.dept_name

---

**第18题**：列出薪金比"SMITH"多的所有员工信息

【相关表】employee
【相关列】employee.id、employee.name、employee.salary（注意：经典模型中 SMITH 是员工姓名，本项目对应 employee.name）

---

**第19题**：列出所有"CLERK"（办事员）的姓名及其部门名称、部门的人数

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.position；department.id、department.dept_name

---

**第20题**：列出最低薪金大于1500的各种工作及从事此工作的全部雇员人数

【相关表】employee
【相关列】employee.id、employee.position、employee.salary

---

**第21题**：列出在部门"SALES"（销售部）工作的员工的姓名，假定不知道销售部的部门编号

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id；department.id、department.dept_name

---

**第22题**：列出薪金高于公司平均薪金的所有员工、所在部门、上级领导、雇员的工资等级

【相关表】employee、department、salary_grade
【相关列】employee.id、employee.name、employee.dept_id、employee.leader_id、employee.salary；department.id、department.dept_name；salary_grade.grade、salary_grade.low_salary、salary_grade.high_salary

---

**第23题**：列出与"SCOTT"从事相同工作的所有员工及部门名称

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.position；department.id、department.dept_name

---

**第24题**：列出薪金等于部门30中员工的薪金的其他员工的姓名和薪金

【相关表】employee
【相关列】employee.id、employee.name、employee.dept_id、employee.salary（注意：经典部门编号 deptno=30，本项目对应 department.id=30）

---

**第25题**：列出薪金高于在部门30工作的所有员工的薪金的员工姓名、薪金、部门名称

【相关表】employee、department
【相关列】employee.id、employee.name、employee.dept_id、employee.salary；department.id、department.dept_name

---

**第26题**：列出在每个部门工作的员工数量、平均工资和平均服务期限

【相关表】employee、department
【相关列】employee.dept_id、employee.salary、employee.hire_date；department.id、department.dept_name

---

**第27题**：列出所有员工的姓名、部门名称和工资

【相关表】employee、department
【相关列】employee.name、employee.dept_id、employee.salary；department.id、department.dept_name

---

**第28题**：列出所有部门的详细信息和人数

【相关表】department、employee
【相关列】department.id、department.dept_name、department.dept_code、department.parent_id；employee.dept_id

---

**第29题**：列出各种工作的最低工资及从事此工作的雇员姓名

【相关表】employee
【相关列】employee.id、employee.name、employee.position、employee.salary

---

**第30题**：列出各个部门的MANAGER（领导）的最低薪金

【相关表】employee
【相关列】employee.id、employee.dept_id、employee.position、employee.salary（注意：MANAGER 是职位，对应 employee.position）

---

**第31题**：列出所有员工的年工资，按年薪从低到高排序

【相关表】employee
【相关列】employee.id、employee.name、employee.salary（年薪 = salary × 12）

---

**第32题**：求出员工领导的薪水超过3000的员工名称与领导名称

【相关表】employee
【相关列】employee.id、employee.name、employee.leader_id、employee.salary

---

**第33题**：求出部门名称中带'S'字符的部门员工的工资合计、部门人数

【相关表】employee、department
【相关列】employee.dept_id、employee.salary；department.id、department.dept_name

---

**第34题**：给任职日期超过30年的员工加薪10%

【相关表】employee
【相关列】employee.id、employee.name、employee.salary、employee.hire_date

---

**第35题**：学生选课综合题

【相关表】student、course、student_course
- student：id(PK)、name、age
- course：id(PK)、name、teacher
- student_course：student_id、course_id、score

子问题：
- 找出没选过"黎明"老师的所有学生姓名
- 列出2门以上（含2门）不及格学生姓名及平均成绩
- 既学过1号课程又学过2号课程所有学生的姓名