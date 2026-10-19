# ERP 系统

开发阶段（MVP）的 ERP 工程。目标是**本地能跑、结构清晰、方便往上加模块**，不做分库分表、多租户、微服务拆分。

架构：**模块化单体**（一个 Spring Boot 进程 + Maven 多模块分包），后期要拆微服务时按包边界拆分即可。

---

## 一、技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| 后端 | Spring Boot | 3.5.16 |
| | Spring Security（JWT 无状态） | 随 Boot |
| | MyBatis-Plus | 3.5.17（`-spring-boot3-` starter） |
| | JDK | 21 |
| | 接口文档 | springdoc-openapi 2.8.17（Swagger UI） |
| | 缓存 | Caffeine（进程内，替代 Redis） |
| | 表结构管理 | Flyway |
| 数据库 | MySQL | 8.0（本机 MySQL80 服务） |
| 前端 | Vue 3 + Vite 6 + Element Plus + Pinia 3 + vue-router 4 | 见 `erp-web/package.json` |

**两个版本上必须注意的坑**（环境实测推导出来的，不要随手升级）：

- **Spring Boot 用 3.5.x 而不是 4.x**：4.x 是 Spring Framework 7 新主线，周边生态跟进有限，MVP 阶段不值得冒编译不过的风险。
- **Vite 用 6.x 而不是 7/8**：Vite 7/8 的 `engines` 要求 Node `^20.19.0 || >=22.12.0`，本机 Node 是 20.11.1，装不上。相应地 vue-router 只能用 4.x（5.x 强制要求 Vite ^7/^8）。想升级得先把 Node 升到 20.19+ 或 22 LTS。

---

## 二、目录结构

```
AI_MyErp/
├── CLAUDE.md                      ★ 给 AI 助手的项目说明（会被自动读取）
├── ERP系统开发环境设计文档.md      架构设计与环境规范的完整说明（人读的话先读这个）
├── docs/
│   ├── 来源与设计笔记.md           设计决策、已验证/未验证清单、代码评审记录
│   ├── 接口与开发规范.md           ★ 写代码前必读：路径/权限/返回格式/SOP
│   └── archive/                    原始设计文档留档
├── tools/
│   ├── e2e-verify/                 端到端验证脚本（49 项断言，一条命令跑完）
│   ├── api-audit/                  前后端路由与字段名对账脚本
│   └── reset-mysql-root-password.*  MySQL root 密码重置（忘了密码时用）
├── sql/
│   └── 00_bootstrap.sql            唯一需要 root 执行一次的脚本（建库 + 建应用账号）
├── erp-server/                     后端 Maven 父工程
│   ├── erp-common/                 统一返回、分页、基础实体、异常、工具类
│   ├── erp-framework/              安全（JWT）、MyBatis-Plus、异常处理、缓存、接口文档
│   ├── erp-modules/
│   │   ├── erp-system/             登录认证、用户、角色、菜单、部门、字典
│   │   ├── erp-mdm/                物料、物料分类、客户、供应商、仓库
│   │   ├── erp-inventory/          库存台账、出入库单、库存流水
│   │   └── erp-purchase/           采购订单、采购收货
│   └── erp-admin/                  启动模块（唯一可执行 jar）+ 配置文件 + 建表脚本
└── erp-web/                        前端 Vue3 工程
```

模块依赖是单向的：`erp-admin → erp-modules/* → erp-framework → erp-common`。
业务模块之间**只能通过对方 `api` 包里的接口互相调用**，不许直接读别人的表。

---

## 三、本地环境准备

### 1. JDK —— 这一步不做的话后端根本编译不了

本机装了 JDK 21（`C:\Program Files\Java\jdk-21.0.12`），但 `JAVA_HOME` 指向的是 JDK 8，
Maven 会读 `JAVA_HOME` 而不是 `PATH`，结果构建 Spring Boot 3 时报 `invalid target release: 21`。

改成：

```
JAVA_HOME = C:\Program Files\Java\jdk-21.0.12
```

改完**必须重开终端**（环境变量不热更新），然后确认：

```bash
mvn -v          # 必须显示 Java version: 21.x，platform encoding: UTF-8
```

### 2. MySQL

本机 MySQL80 服务已在 3306 运行，无需 Docker、无需额外安装。

### 3. 不需要装的东西

Redis、Nacos、RocketMQ、Docker **都不需要**：
- 缓存用 Caffeine（进程内），单体单进程部署足够
- 没有网关和注册中心，前端直接连后端 8080
- 异步消息本期用不到

### 4. Node.js

需要 Node 20+（本机 20.11.1 可用）。

---

## 四、数据库初始化

### 第一步：建库 + 建应用账号（需要 root，只执行一次）

```bash
# Git Bash 里执行；mysql.exe 不在 PATH，需要写全路径
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p < "E:/AI-Cladue/AI_MyErp/sql/00_bootstrap.sql"
```

脚本会创建：
- 数据库 `erp_dev`（utf8mb4）
- 应用账号 `erp` / `Erp@123456`（**不用 root 跑应用**）

### 第二步：建表（不需要手工做）

所有建表脚本在 `erp-server/erp-admin/src/main/resources/db/migration/`，
应用启动时由 **Flyway 自动执行**：

| 脚本 | 内容 |
|---|---|
| `V1__schema_system.sql` | sys_* 表 |
| `V2__schema_mdm.sql` | mdm_* 表 |
| `V3__schema_inventory.sql` | inv_* 表 |
| `V4__schema_purchase.sql` | pur_* 表 |
| `V5__menu_group.sql` | `sys_menu.group_name`（功能导航页的分区字段） |
| `R__seed_system_dept.sql` | 部门种子数据 |
| `R__seed_system_dict.sql` | 字典种子数据 |
| `R__seed_system_menu.sql` | 菜单与权限点（前端动态路由的数据源） |
| `R__seed_system_menu_group.sql` | 菜单的功能分组（功能导航页分区用） |
| `R__seed_system_role.sql` | 角色与角色菜单授权 |

表结构要改就**新增** `V6__xxx.sql`（V5 已被菜单分组占用），不要改已经执行过的 V 脚本（Flyway 校验 checksum，改了会启动报错）。
`R__` 开头的可重复脚本内容变了会自动重跑，所以它们都是幂等的。

### 第三步：初始管理员账号

启动时由 `SysDataInitializer` 自动创建（幂等），**密码不在 SQL 里写死** ——
BCrypt 哈希没法手写正确，一旦抄错，现象是"密码明明对却登录失败"，极难排查。

```
账号：admin
密码：123456
```

---

## 五、启动

### 后端

**方式一：Maven 直接启动（开发时最方便，改代码不用重新打包）**

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
cd "E:/AI-Cladue/AI_MyErp/erp-server/erp-admin"
mvn spring-boot:run
```

> 必须在 **erp-admin** 目录下执行，不能在 `erp-server`（根目录）。
> 根目录是聚合 POM（packaging=pom），没有启动类，在那里跑 `spring-boot:run` 会报找不到主类。

> ⚠ **改了 `erp-system` / `erp-mdm` / `erp-inventory` / `erp-purchase` 的代码后，
> 这条命令不会生效**——它只重编译 `erp-admin`，兄弟模块用的是本地仓库里的旧 jar，
> 现象是「代码明明改了，接口行为没变」。要么先在 `erp-server` 根目录跑
> `mvn clean install -DskipTests` 再启动，要么用
> `mvn -pl erp-admin -am spring-boot:run`。

**方式二：打包成可执行 jar**

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
cd "E:/AI-Cladue/AI_MyErp/erp-server"
mvn clean package -DskipTests
java -jar erp-admin/target/erp-server.jar
```

**方式三：IDEA** —— 直接运行 `com.erp.ErpApplication`。

### 关于 JAVA_HOME（这一步不做，上面三条都会失败）

Maven 读的是 **`JAVA_HOME` 环境变量，不是 `PATH`**。这台机器的系统环境变量里
`JAVA_HOME` 指向的是 JDK 8，所以直接跑 Maven 会失败。

**你可以不改系统变量，只在当前终端临时指定**：

```bash
# Git Bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"

# PowerShell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"

# cmd
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12
```

或者一次性改掉系统变量（一劳永逸，但需要重开所有终端）：

```
系统属性 → 高级 → 环境变量 → 把 JAVA_HOME 改成 C:\Program Files\Java\jdk-21.0.12
```

**如果 JDK 不对，构建会立刻停下并打印明确的提示**（不是那种看不懂的编译错误）：

```
BUILD STOPPED: JDK 21+ required, but Maven is running on another JDK.
Maven reads JAVA_HOME (NOT PATH). To fix:
  1) Point JAVA_HOME at JDK 21, e.g. C:\Program Files\Java\jdk-21.0.12
  2) Reopen the terminal (env vars are not hot-reloaded)
  3) Confirm with: mvn -v   -> should show "Java version: 21.x"
```

校验用的 `mvn -v` 必须显示 `Java version: 21.x`；显示 1.8 就是还没生效。

启动成功后：
- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/api/health

### 前端

```bash
cd "E:/AI-Cladue/AI_MyErp/erp-web"
npm install
npm run dev          # http://localhost:3000
```

用 `admin / 123456` 登录。

---

### 不连数据库的冒烟启动（排查启动问题很有用）

还没建库、或者想确认「是不是数据库的问题」时，可以用这条命令让应用在没有库的情况下起来：

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
cd erp-server/erp-admin/target
"$JAVA_HOME/bin/java" -jar erp-server.jar \
  --spring.datasource.hikari.initialization-fail-timeout=-1 \
  --spring.flyway.enabled=false \
  --erp.init.enabled=false
```

三个参数的含义：连接池不因连不上库而中止启动、跳过建表、跳过初始账号创建。

**它能验证**：构建产物能否启动、Spring 上下文装配、Security 过滤器链、接口文档、各接口是否注册成功
（`/api/health` 能返回，不带 Token 调业务接口能返回规范的 401）。

**它验证不了**：任何读写数据库的接口（登录、列表、增删改查）依然会失败。

价值在于**把「应用装配问题」和「数据库问题」分开**。本项目就是靠这种方式抓到了三个
会让应用完全起不来的配置错误（`%clr` 日志、MyBatis 别名冲突、knife4j 不兼容），
这些在「编译通过」阶段完全看不出来。

> 另外，`tools/api-audit/` 下的接口对账脚本也需要应用处于运行状态（只读 OpenAPI 文档，不碰数据库），
> 用这条命令起的应用就能跑。

---

## 六、验收

### 自动验证（推荐，一条命令）

```bash
cd "E:/AI-Cladue/AI_MyErp/tools/e2e-verify"
python -I e2e_verify.py
```

跑完 **49 项断言**，覆盖：鉴权与权限隔离、主数据 CRUD、库存闭环（含重复审核被拒、
库存不足被拒、事务回滚）、采购入库闭环（含重复确认被拒、库存数量核对）、
跨模块取数、id 字符串化等。用退出码表示结果，可直接挂 CI。

已实测通过，全部 49 项 PASS。**脚本可以反复运行**（每次用唯一编码，不会撞唯一索引）。

另外两个对账脚本（前端改了接口后建议跑）：

```bash
cd tools/api-audit
python -I ApiAudit.py   E:/AI-Cladue/AI_MyErp/erp-web/src http://localhost:8080   # 路由对账
python -I FieldAudit.py E:/AI-Cladue/AI_MyErp/erp-web/src http://localhost:8080   # 字段名对账
```

### 人工确认（脚本验不了渲染与交互）

- [ ] 打开 http://localhost:3000，用 `admin / 123456` 登录
- [ ] 左侧出现 3 个一级目录：系统管理 / 基础资料 / **供应链**
      供应链下是 库存管理、采购管理；仓库管理在**库存管理**下
- [ ] 物料管理页：表格、分页、新增/编辑弹窗、删除确认框都正常
- [ ] 表格里的状态列显示成彩色标签
- [ ] 新建一个采购员账号并赋「采购员」角色，用它登录：菜单只剩 2 个，
      且「新增/删除」按钮**在界面上确实不显示**

### 还需要专门测的

- [ ] **并发**：用 ab/JMeter 同时打同一个单据的审核接口，确认库存没有被加两次
      （实现上靠数据库行锁 + 条件更新保证，逻辑正确，但未实测）

---

## 七、常见问题

| 现象 | 原因与处理 |
|---|---|
| `invalid target release: 21` | `JAVA_HOME` 还指着 JDK 8，见第三节 |
| `Public Key Retrieval is not allowed` | 连接串缺 `allowPublicKeyRetrieval=true&useSSL=false`（MySQL 8 默认 caching_sha2_password 导致），已配好，改连接串时别删 |
| 时间差 8 小时 | 连接串缺 `serverTimezone=Asia/Shanghai` |
| 分页查出全部数据、`total` 不对 | XML 里 `IPage` 参数没放在第一个位置 |
| 删掉的数据还在列表里出现 | 手写 SQL 忘了加 `is_deleted = 0`（MyBatis-Plus 的逻辑删除只管自动方法） |
| 菜单加了但前端看不到 | 检查 `sys_menu` 的 `component` 是否与 `erp-web/src/views/` 下的路径逐字对应 |
| 按钮看得见但点了 403 | 菜单表里的 `perms` 与代码里 `@PreAuthorize` 的权限串没对齐 |

### 关于 Maven 本地仓库

如果本机 `%USERPROFILE%\.m2\settings.xml` 里自定义了 `<localRepository>`
（例如放到了其他磁盘），依赖就会下载到那个目录，而不是默认的 `~/.m2/repository`。
IDEA 用的是同一份 settings，两边一致，不影响使用；若想改回默认位置，
把 `settings.xml` 里的 `<localRepository>` 一行注释掉即可。

---

## 八、下一步可以做什么

按优先级：

1. **操作日志**（`sys_oper_log` + `@Log` 注解 + AOP）。ERP 上线后第一个被要求的就是"谁改的"。
2. **数据权限**：`sys_role.data_scope` 字段已预留，目前只实现了"全部"。
3. **Excel 导入导出**：主数据批量导入是刚需。
4. **单据打印 / 导出 PDF**。
5. **引入 Redis**：多实例部署时把 `TokenService` 里的 Caffeine 换成 Redis 即可，接口不用变。
6. **金蝶云星空对接**：`mdm_material` / `mdm_customer` / `mdm_supplier` 已预留
   `external_code` 与 `source_system` 字段，加一个独立的对账同步模块即可。

---

更详细的架构说明、建表规范、开发规范见 `ERP系统开发环境设计文档.md` 与 `docs/接口与开发规范.md`。
用 AI 助手改这个项目时的工作约定（环境硬约束、模块边界、踩过的坑）见 `CLAUDE.md`。
