# ERP 系统开发环境设计文档

> 版本：v2.0 　 修订日期：2026-10-16
> v1.0 原文已存档于 `docs/archive/原始设计文档.md`
> 配套文档：`README.md`（怎么跑起来）、`docs/接口与开发规范.md`（写代码的硬约束）

---

## 文档说明

本文面向 ERP 系统**开发阶段**的架构与工程规范，服务于三个目标：**本地能跑、快速开发、结构清晰**。
不涉及商用级高可用、分库分表、多租户等复杂设计。

**v2 与 v1 的核心差异**：v1 描述的是「8 个微服务 + Docker Compose + Nacos 注册中心 + 每服务一个库」，
但这套方案在**本机一步都跑不通**（没有 Docker、没有 Nacos、没有 Redis，且 Maven 跑在 JDK 8 上）。
v2 改为**模块化单体**，并按本机实测环境重写了全部版本与环境说明。
第 9 节逐条列出了 v1 的问题与修改理由。

---

## 一、架构决策：为什么是模块化单体，不是微服务

### 决策

**一个 Spring Boot 进程 + Maven 多模块分包**。按业务分模块（system / mdm / inventory / purchase），
模块之间用 Java 接口调用，不引入网关、注册中心、RPC 框架。

### 理由

| 维度 | 8 微服务（v1 方案） | 模块化单体（v2 方案） |
|---|---|---|
| 本机能否跑通 | **不能**（无 Docker / Nacos / Redis） | 能，一条命令启动 |
| 开发一个「采购收货入库」功能 | 要跨 purchase → inventory → mdm 三个服务，写库存查询要 Feign 拼装 3 次 | 一次方法调用，本地事务保证一致性 |
| 调试 | 断点跨进程，日志分散在 8 个文件 | 单进程，一个断点到底 |
| 联表查询 | 物理上不可能（每服务一个库） | 直接 join（但仍禁止跨模块 join，见规范） |
| 启动成本 | 8 个 JVM + Nacos ≈ 4GB 内存 | 1 个 JVM ≈ 500MB |
| 何时拆 | —— | 团队 >10 人、模块需要独立扩容、或某模块需要不同技术栈时 |

**关键点：包边界就是未来的微服务边界。** 现在把模块之间调用的规矩定死
（只能通过对方 `api` 包的接口，禁止直接读别人的表），将来要拆时，
把 `api` 包的实现从「本地 Bean 调用」换成「Feign 调用」即可，业务代码一行不用改。

**本地事务是白拿的收益**：采购收货 → 生成入库单 → 增加库存 → 写流水，
这四步在同一事务里，任一步失败全部回滚。拆成微服务后这里就必须上 Seata 或最终一致性补偿，
而 MVP 阶段引入 Seata 的复杂度远超它带来的价值。

### 环境分层

开发阶段只维护一套环境：

| 环境 | 用途 | 部署方式 | 数据 |
|------|------|----------|------|
| Local | 本地开发调试 | 本机直接运行 | 测试数据 |

UAT、生产、Dev 联调环境等项目后期再搭建。Redis / Nacos / Docker / RocketMQ **本期一律不引入**。

---

## 二、本机环境（实测结果，非假设）

| 项目 | 本机实际状态 | 处理方式 |
|---|---|---|
| 操作系统 | Windows 10 家庭中文版 | —— |
| JDK | 装了 **21**（`C:\Program Files\Java\jdk-21.0.12`）与 8 | 用 21。**必须改 `JAVA_HOME`**，见下 |
| 环境变量问题 | `JAVA_HOME` 指向 `D:\Program Files\Java\jdk1.8.0_202` | Maven 读的是 `JAVA_HOME` 而不是 `PATH`，不改会报 `invalid target release: 21` |
| Maven | 3.8.3（能跑在 JDK 21 上） | 无需升级，配好 `JAVA_HOME` 即可 |
| Maven 本地仓库 | `settings.xml` 自定义了本地仓库路径 | 该目录会自动创建，不影响使用 |
| 生效的镜像 | id 为 `alimaven`（`mirrorOf=central`） | 原为已停服的 HTTP 老地址，已改为 `https://maven.aliyun.com/repository/public` |
| Node.js | 20.11.1 / npm 10.2.4 | 够用，但**决定了 Vite 只能用 6.x**（见版本矩阵） |
| MySQL | 8.0，Windows 服务 `MySQL80`，监听 3306 | 直接用，不需要 Docker |
| Docker | **未安装** | 不需要 |
| Redis | **未安装**（6379 无监听） | 用 Caffeine 替代 |
| Nacos | **未安装**（8848 无监听） | 不需要（无注册中心） |
| SQL Server | 1433 在跑（本机金蝶云星空账套库） | 本期不参与，字段已预留 |

### 必须做的环境修复

```bash
JAVA_HOME = C:\Program Files\Java\jdk-21.0.12

# 改完必须重开终端，然后验证：
mvn -v     # 期望看到 Java version: 21.x 与 platform encoding: UTF-8
```

顺带收益：JDK 8 下 Maven 的 `platform encoding` 是 GBK，中文注释容易乱码；换到 21 后是 UTF-8。

---

## 三、版本矩阵（全部经 Maven Central / npm 实测确认存在）

### 后端

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **21** | Spring Boot 3.x 要求 17+ |
| Spring Boot | **3.5.16** | 3.5 线最新稳定版 |
| Spring Security | 随 Boot（6.5.x） | 无状态 JWT |
| MyBatis-Plus | **`com.baomidou:mybatis-plus-spring-boot3-starter:3.5.17`** | **必须用 `-spring-boot3-` 后缀**，用成 `mybatis-plus-boot-starter`（Boot 2 的）会启动即失败 |
| MyBatis-Plus JSqlParser | **`mybatis-plus-jsqlparser:3.5.17`** | **3.5.9 起分页必需**，不加会 `NoClassDefFoundError: net/sf/jsqlparser/*` |
| MySQL 驱动 | 由 Boot BOM 仲裁（9.7.0） | 不要手写版本 |
| 连接池 | HikariCP（Boot 默认） | —— |
| JWT | `io.jsonwebtoken:jjwt-api/impl/jackson:0.12.7` | 用 0.12 的新 API（`Jwts.SIG.HS256`） |
| 工具类 | `cn.hutool:hutool-all:5.8.48` | —— |
| 接口文档 | `springdoc-openapi-starter-webmvc-ui:2.8.17` | 面向 Spring Boot 3.5 的 springdoc 线。**不用 knife4j**：实测与 Boot 3.5 不兼容且无可用新版本，详见下方说明 |
| 缓存 | `caffeine`（Boot BOM 仲裁） | 替代 Redis |
| 表结构管理 | `flyway-core` + **`flyway-mysql`** | 只引 core 会报 `Unsupported Database: MySQL` |
| Lombok | Boot BOM 仲裁 | 需 ≥1.18.30 才支持 JDK 21 |

**为什么不选 Spring Boot 4.x**：4.x 是 Spring Framework 7 的新主线，MyBatis-Plus 虽有 `-spring-boot4-starter`，
但周边生态跟进有限，MVP 阶段不值得冒编译不过或运行异常的风险。3.5.x 是生态最完整的一条线。

### 前端（受 Node 20.11.1 约束）

| 组件 | 版本 | 说明 |
|---|---|---|
| Vue | ^3.5.44 | —— |
| Vite | **^6.4.4** | **不能用 7/8**：其 `engines` 要求 `^20.19.0 \|\| >=22.12.0`，本机 Node 20.11.1 装不上 |
| @vitejs/plugin-vue | ^6.0.10 | peer 覆盖 Vite ^5~^8。**注意它内部用了 `crypto.hash()`，这是 Node 20.12+ 才有的 API**，Node 20.11 上会报 `crypto.hash is not a function`。已在 `vite.config.js` 顶部加了一段兼容补丁（Node 升到 20.12+ 后可删），详见下方「落地踩坑记录」 |
| vue-router | **^4.6.4** | **不能用 5.x**：vue-router 5 强制要求 Vite ^7/^8 |
| Pinia | **^3.0.4** | peer 仅需 vue ^3.5.11，API 与经典 `defineStore` 一致 |
| Element Plus | ^2.14.7 | + `@element-plus/icons-vue` ^2.3.2 |
| axios | ^1.20.0 | —— |
| sass | ^1.105.2 | —— |

> 想用 Vite 8 + vue-router 5 + Pinia 4 这套新组合，**必须先把 Node 升到 20.19+ 或 22 LTS**。
> MVP 阶段先跑通比尝鲜重要。
>
> 不引入 `unplugin-auto-import` / `unplugin-vue-components`：Element Plus 全量引入，
> 少一层构建期魔法，出问题时更容易定位。

---

## 四、后端工程结构

### 1. 整体目录

```
erp-server/                          # 父工程（packaging=pom）
├── pom.xml                          # 版本矩阵的唯一真相源
│
├── erp-common/                      # 公共层：无 Spring Web/Security 依赖
│   └── com.erp.common.core
│       ├── domain/                  R、PageQuery、PageResult、BaseEntity
│       ├── enums/ResultCode         统一返回码
│       ├── exception/ServiceException
│       ├── constant/CommonConstants
│       └── utils/                   StringUtils、BeanCopyUtils、TreeUtils
│
├── erp-framework/                   # 基础设施层
│   └── com.erp.framework
│       ├── security/                SecurityConfig、JwtUtils、TokenService、
│       │                            LoginUser、PermissionService("ss")、SecurityUtils
│       ├── mybatis/                 MybatisPlusConfig、ErpMetaObjectHandler
│       ├── web/                     GlobalExceptionHandler、JsonResponseWriter、ServletUtils
│       └── config/                  JacksonConfig、CaffeineCacheConfig、OpenApiConfig
│
├── erp-modules/
│   ├── erp-system/                  登录认证、用户、角色、菜单、部门、字典
│   ├── erp-mdm/                     物料、物料分类、客户、供应商、仓库
│   ├── erp-inventory/               库存台账、出入库单、库存流水
│   └── erp-purchase/                采购订单、采购收货
│
└── erp-admin/                       # 启动模块（唯一可执行 jar）
    └── src/main
        ├── java/com/erp/
        │   ├── ErpApplication.java  # 启动类放 com.erp 根包，才能扫到所有模块
        │   └── controller/HealthController.java
        └── resources/
            ├── application.yml          # 主配置（提交）
            ├── application-local.yml    # 本地覆盖（提交）
            ├── logback-spring.xml
            └── db/migration/            # Flyway 建表脚本
```

### 2. 模块依赖（单向，无环）

```
erp-common  ←  erp-framework  ←  erp-modules/*  ←  erp-admin
                                      ↑
                    erp-system / erp-mdm / erp-inventory / erp-purchase
                    （业务模块之间：erp-inventory → erp-mdm
                                 erp-purchase  → erp-inventory, erp-mdm）
```

三条铁律：

1. **启动类必须放在 `com.erp` 根包**（本项目放在 `erp-admin/src/main/java/com/erp/ErpApplication.java`）。
   放在 `com.erp.admin` 会导致组件扫描扫不到 `com.erp.system` 等业务包，
   症状是「启动成功但所有接口 404」。
2. **业务模块之间只能通过对方 `api` 包里的接口调用**，禁止注入别的模块的 Mapper、禁止 join 别的模块的表。
   库存要物料信息就注入 `MdmApi`。这是将来拆微服务时唯一需要改的地方。
3. **只有 `erp-admin` 声明 `spring-boot-maven-plugin`**。业务模块是被依赖的库，
   加了它会把 jar 打成不可被依赖的 fat jar。

### 3. 单个业务模块内部分层

```
com.erp.mdm
├── controller/     MaterialController          只做参数校验 + 调 Service + 包 R
├── service/        IMaterialService
├── service/impl/   MaterialServiceImpl         写操作加 @Transactional
├── mapper/         MaterialMapper              @Mapper extends BaseMapper
├── domain/         Material                    实体，继承 BaseEntity
├── domain/dto/     MaterialQuery / MaterialDto 入参，带校验注解
├── domain/vo/      MaterialVo                  出参
├── api/            MdmApi                      跨模块调用边界（其他模块只能用这个）
└── resources/mapper/mdm/MaterialMapper.xml     只写复杂联表
```

### 4. 端口规划

| 服务 | 端口 |
|------|------|
| 后端（单体） | 8080 |
| 前端（Vite dev server） | 3000 |
| MySQL | 3306 |
| 接口文档（Swagger UI） | http://localhost:8080/swagger-ui.html |

8081-8087 留作将来拆分服务时用。

---

## 五、鉴权设计

### 方案

Spring Security 6 + JWT，**无状态**，**不需要 Redis**。

```
登录：用户名密码 → AuthenticationManager → BCrypt 校验 → 构建 LoginUser（含权限）
      → TokenService 缓存 LoginUser（key = 随机 tokenId）→ 签发 JWT（JWT 里只放 tokenId）

请求：JwtAuthenticationTokenFilter 解析 JWT 拿 tokenId → 从缓存取 LoginUser
      → 放进 SecurityContextHolder → @PreAuthorize 校验权限

登出：删掉缓存里的 LoginUser → Token 立即失效
```

### 几个关键设计

1. **JWT 里只放 tokenId，不放权限**。
   放权限的话，改了角色权限要等 Token 过期才生效；而且权限多了 Token 会超长。
2. **登录态缓存在 Caffeine（进程内）而不是 Redis**。单体单进程足够；
   重启应用会导致所有人重新登录，开发阶段完全可接受。
   将来要多实例部署，把 `TokenService` 里那个 Cache 换成 RedisTemplate 即可，**对外接口不变**。
3. **超级管理员用通配符 `*:*:*`**，不展开它实际拥有的几百个权限点：
   展开的话每次登录要扫全表，而且新增菜单后旧 Token 里没有新权限，
   表现为「给管理员加了菜单却点不动」。
4. **超级管理员按用户名/角色判断，不按「用户 ID == 1」**：
   主键是数据库自增的，谁也不能保证 admin 一定是 1 号（重建库、导数据都会变），
   判断失效时的症状是「管理员突然没权限了」，极难联想到原因。
5. **权限注解用 `@PreAuthorize("@ss.hasPermi('mdm:material:add')")`**，
   配合 `@Component("ss")` 的 `PermissionService` 与 `@EnableMethodSecurity`。
   **不要用 Shiro 的 `@RequiresPermissions`**，本工程没有引 Shiro。

### 权限串规范

```
模块:资源:动作     例：system:user:add   mdm:material:list   inv:order:audit
```

必须与 `R__seed_system_menu.sql` 里 `perms` 列**逐字一致**。
写错一个字母的现象是「按钮能看见，点下去 403」。

### 前端权限

- 菜单：后端 `/api/auth/routers` 下发路由树，前端 `router.addRoute()` 动态注入，**前端不硬编码任何菜单**
- 按钮：`v-hasPermi="['mdm:material:add']"` 指令

---

## 六、前端工程结构

```
erp-web/
├── vite.config.js          # 端口 3000，/api 代理到 8080，不做 rewrite
├── .env.development
└── src/
    ├── api/                接口层，按模块分文件（system/ mdm/ inventory/ purchase/）
    ├── assets/styles/      全局样式
    ├── components/         Pagination、DictSelect、DictTag、RightToolbar
    ├── directive/          hasPermi、hasRole
    ├── layout/             Sidebar / Navbar / TagsView / AppMain / Breadcrumb
    ├── router/index.js     只有常量路由（login/404/401/dashboard）
    ├── permission.js       路由守卫：无 token 跳登录；有 token 则拉取用户信息 + 动态路由
    ├── store/modules/      user、permission、app、tagsView、dict
    ├── utils/              request.js（axios 封装）、auth.js、permission.js、dict.js
    └── views/              页面，目录与后端菜单表的 component 字段逐字对应
```

### 三个容易踩的点

1. **动态路由的组件映射**：后端返回 `component: 'mdm/material/index'`，
   前端用 `import.meta.glob('../../views/**/*.vue')` 匹配到真实组件。
   这里**用相对路径而不是 `@` 别名**（部分 Vite 版本 glob 不认别名）。
   后端菜单表的 `component` 与前端文件路径必须逐字对应，否则页面白屏。
2. **代理不做 rewrite**：后端接口路径本身就带 `/api`（Controller 上直接写 `@RequestMapping("/api/system/user")`），
   这样浏览器、curl、Swagger 看到的地址完全一致，避免「文档写 /api/xxx，实际要调 /xxx」的歧义。
3. **`id` 是字符串**：后端把 `Long` 序列化成了字符串以防止 JS 精度丢失（见第七节），
   前端比较 id 时用 `==` 或统一转字符串，不要用 `===` 跟数字比。

---

## 七、关键配置说明

### 数据源连接串

```
jdbc:mysql://127.0.0.1:3306/erp_dev
  ?useUnicode=true&characterEncoding=utf8
  &serverTimezone=Asia/Shanghai
  &useSSL=false&allowPublicKeyRetrieval=true
  &rewriteBatchedStatements=true
```

四个参数都不能少，各自解决的问题：

| 参数 | 少了会怎样 |
|---|---|
| `allowPublicKeyRetrieval=true` + `useSSL=false` | MySQL 8 默认 `caching_sha2_password`，非 SSL 连接下报 `Public Key Retrieval is not allowed`，**直接连不上** |
| `serverTimezone=Asia/Shanghai` | 不指定时按 JVM 默认解析，时间差 8 小时 |
| `rewriteBatchedStatements=true` | 批量插入性能差一个数量级 |
| `characterEncoding=utf8` | 中文乱码 |

### MyBatis-Plus

```yaml
mybatis-plus:
  # classpath*: 才能扫到各业务模块 jar 内部的 XML。
  # 只写 classpath: 只会扫启动模块自己的 resources —— 这是 mapper binding 报错最常见的原因
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    db-config:
      logic-delete-field: isDeleted   # 必须与 BaseEntity 上的 @TableLogic 一致
```

插件链（`MybatisPlusConfig`）：分页插件 + 防全表更新删除插件（`BlockAttackInnerInterceptor`）。
后者的价值：本项目的删除是 `UPDATE ... SET is_deleted = 1`，一旦漏写 where 就是**全表逻辑删除**，
除了翻备份没有别的办法挽回。开发期就装上这个安全网。

### 自动填充（`ErpMetaObjectHandler`）

`create_by` / `create_time` / `update_by` / `update_time` / `is_deleted` 由它统一赋值。
**为什么必须要有**：建表 DDL 里的 `DEFAULT CURRENT_TIMESTAMP` 只能兜住时间字段，
**兜不住「谁操作的」**。没有这个类，所有表的 `create_by` 会永远是 NULL。
无登录态场景（启动初始化、定时任务）统一写 `system`，绝不留空。

### 逻辑删除的边界（重要）

MyBatis-Plus 的逻辑删除**只对 `BaseMapper` 自动生成的方法生效**。
自己写的 XML 查询**不会**自动追加 `is_deleted = 0`，必须手写。
这是 MyBatis-Plus 项目里最容易漏、且漏了以后很久才发现的一处：删掉的数据会重新出现在列表里。

### JSON 序列化（`JacksonConfig`）

- **`Long` → 字符串**：JS 的 Number 只精确到 2^53，将来换雪花 ID 时会丢精度且完全看不出来。
  现在用自增主键还碰不到，但等碰到再改就要翻遍前端，所以第一天就配上。
  注意只转包装类型 `Long`，**基本类型 `long` 不转** —— 这样分页的 `total` 仍是数字。
- 时间统一 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`。

### 异常与返回码（`GlobalExceptionHandler`）

```json
成功：{ "code": 200, "msg": "操作成功", "data": {...} }
失败：{ "code": 500, "msg": "物料编码已存在：M001" }
```

HTTP 状态码约定：

| 场景 | HTTP | body.code |
|---|---|---|
| 成功 | 200 | 200 |
| 业务失败 / 参数校验失败 | **200** | 400 / 500 |
| 未登录 / Token 过期 | **401** | 401 |
| 已登录但无权限 | **403** | 403 |

业务失败用 HTTP 200（请求成功送达并处理了，失败在业务语义层），前端统一按 `res.code !== 200` 弹提示。
但 **401/403 必须用真实状态码**：前端要区分「登录过期（清 Token 跳登录页）」和
「无权限（只弹提示，绝不能把人踢下线）」。

**`AccessDeniedException` 必须单独声明处理器**：`@PreAuthorize` 抛的 `AuthorizationDeniedException`
继承自它，且是在 Controller 调用过程中抛出的，会被 `@RestControllerAdvice` 先接住。
不单独处理的话会被兜底成 500，前端把「没权限」当成「系统错误」。

---

## 八、数据库规范

### 1. 库表规划

单库 `erp_dev`，按**表前缀**隔离模块，前缀即未来的库边界：

| 前缀 | 归属模块 | 表 |
|---|---|---|
| `sys_` | erp-system | sys_user、sys_role、sys_menu、sys_dept、sys_dict_type、sys_dict_data、sys_user_role、sys_role_menu |
| `mdm_` | erp-mdm | mdm_material、mdm_material_category、mdm_customer、mdm_supplier、mdm_warehouse |
| `inv_` | erp-inventory | inv_stock、inv_stock_log、inv_order、inv_order_detail |
| `pur_` | erp-purchase | pur_order、pur_order_detail、pur_receipt、pur_receipt_detail |

### 2. 建表规范

**每张业务表必须包含六个基础字段**：

```sql
id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
create_by     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_by     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
PRIMARY KEY (id),
KEY idx_xxx_is_deleted (is_deleted)     -- 逻辑删除后所有查询都带 is_deleted=0，需要索引
```

其他约定：

| 项 | 规范 | 原因 |
|---|---|---|
| 命名 | 小写下划线，表名用模块前缀 | 前缀即库边界 |
| 业务唯一键 | **必须建 UNIQUE**（编码、用户名、字典类型等） | 并发下「先查再插」必然重复，代码校验只能算提示 |
| 金额、数量 | `DECIMAL(18,4)` | 禁止 FLOAT/DOUBLE，二进制浮点算钱会出误差 |
| 主键类型 | `BIGINT` 自增 | 为将来换分布式 ID 留空间 |
| 时间字段 | `DATETIME` | 不用 TIMESTAMP（2038 问题、时区转换隐式行为） |
| 字典表 | **必须拆成「类型」+「数据」两张** | 一张表无法区分不同分类下同名 value 的两条数据，下拉框会串数据 |
| 逻辑删除 | 关联表（`*_role`、`*_menu`）不加 | 取消授权就是删一行，留历史没意义；真审计应记操作日志 |

### 3. 表结构变更流程（Flyway）

```
V{n}__{说明}.sql      版本脚本，只执行一次，序号全局唯一
R__seed_{说明}.sql    可重复脚本，内容变化时自动重跑（必须幂等）
```

- **改表结构要新增 `V5__xxx.sql`，绝不要改已经执行过的 V 脚本** —— Flyway 校验 checksum，改了会启动报错
- `R__` 脚本统一用「固定主键 + `ON DUPLICATE KEY UPDATE`」保证幂等；
  不要用 `REPLACE INTO`（先删后插会重置 create_time）
- 为什么用 Flyway 而不是手工执行 SQL：MVP 阶段表结构一天一变，
  手工同步必然出现「我这儿能跑你那儿不能跑」

### 4. 种子数据

| 内容 | 位置 |
|---|---|
| 部门、字典、菜单权限、角色 | `R__seed_*.sql`（SQL 里） |
| **管理员账号** | **Java 代码** `SysDataInitializer`（不在 SQL 里） |

**为什么管理员密码不用 SQL 写死**：BCrypt 每次加密的盐不同，哈希值无法用肉眼看对错。
从网上抄一个 `$2a$10$...` 写进脚本，一旦明文不是预期的那串，
现象是「密码明明是 123456 却登录失败」，而这条链路要查前端→过滤器→AuthenticationManager→密码比对，
能耗掉大量时间。交给 Java 用它自己的 `PasswordEncoder` 生成，哈希一定是对的，且幂等、可关闭。

---

## 九、v1 文档的问题清单与修改

这一节说明 v2 改了什么、为什么。每一条都是实际会导致失败或返工的问题。

### A. 直接跑不通（硬伤）

| # | v1 的问题 | v2 的修改 |
|---|---|---|
| 1 | 第四节整节「Docker Compose 一键启动中间件」——本机**未装 Docker** | 删除该节，改为直接用本机已装的 MySQL80 服务 |
| 2 | Nacos（8848）、Redis（6379）本机**未安装**，而 `bootstrap.yml` 里写了 `spring.cloud.nacos.*` | 移除 Nacos 与 Redis；缓存改用 Caffeine |
| 3 | **`bootstrap.yml` 在 Spring Boot 3 里不生效**（除非额外引 `spring-cloud-starter-bootstrap`），原文把数据源写在里面 = 静默忽略 = 报「找不到数据源」 | 统一用 `application.yml` + profile |
| 4 | 网关路由 `uri: lb://erp-auth` 依赖 Nacos 服务发现，无注册中心时 `lb://` 不可用 | 移除网关，前端直连 8080 |
| 5 | 只写「JDK 17 最低要求」，没提**本机 Maven 跑在 JDK 8 上**，构建必报 `invalid target release` | 单列一节讲 `JAVA_HOME` 修改与验证 |
| 6 | 说 JDK 17 / Node 18，**本机实际是 JDK 21 / Node 20.11.1** | 按实测环境重写 |
| 7 | 连接串缺 `allowPublicKeyRetrieval=true&useSSL=false` | 补齐，并说明缺了会报什么错 |
| 8 | 写 `username: root / password: root`，用 root 跑应用 | 建专用账号 `erp`，root 只在建库时用一次 |
| 9 | 未提版本，按「装最新」会装到 Vite 8，**Node 20.11.1 装不上** | 钉死版本矩阵并说明每个约束的来源 |

### B. 架构判断对本阶段不划算

| # | v1 的问题 | v2 的修改 |
|---|---|---|
| 10 | 8 个微服务 + 8 个库：开发期要起 8 个 JVM + Nacos + Redis，且物料+库存+采购的联表只能靠 Feign 拼装，**写一个库存查询要跨 3 个服务，开发速度反而更慢** | 改模块化单体，包边界留作拆分接缝 |
| 11 | `erp-auth` 与 `erp-system` 拆成两个服务：登录必须查用户/角色/权限，一拆就产生强耦合 + 每次登录一次网络调用 | 登录认证并入 erp-system |
| 12 | 「跨服务调用必须用 OpenFeign」 | 改为「跨模块调用必须走对方 `api` 包接口」，将来把实现换成 Feign 即可 |
| 13 | 端口表把 8080 给网关，前端 proxy 又指向 8080，语义冲突 | 8080 归单体应用 |

### C. 规范缺漏（照着做会踩坑）

| # | v1 的问题 | v2 的修改 |
|---|---|---|
| 14 | 有 `is_deleted` 字段但没配 MyBatis-Plus 逻辑删除 → 删除变成物理删除，或字段永远为 0 | 配 `logic-delete-field` + `@TableLogic`，并说明手写 XML 的边界 |
| 15 | 没有 `MetaObjectHandler` 的说明 → `create_by/update_by` 全部为 NULL | `ErpMetaObjectHandler` 统一填充 |
| 16 | 「admin/123456」没说要 BCrypt 存储 → 明文入库 + BCrypt 校验 = 永远登录失败 | 用 `SysDataInitializer` 生成哈希 |
| 17 | `sys_dict` 只规划一张表 → 无法区分分类 | 拆成 `sys_dict_type` + `sys_dict_data` |
| 18 | `sys_menu` 没给关键字段说明，**前端动态路由根本没法实现**（这是前端最难的 20%，v1 完全没覆盖） | 明确 `menu_type`/`path`/`component`/`perms`/`icon` 的组合规则，并给出种子数据 |
| 19 | 没提 BIGINT 主键的 JS 精度问题 | Jackson `Long → String`，第一天就配 |
| 20 | DDL 缺业务唯一索引 | 所有业务编码建 UNIQUE |
| 21 | 接口路径 `/api/模块/功能` 与网关 `stripPrefix` 组合有歧义（下游到底带不带 `/api`） | 单体下 Controller 直接写全路径，代理不 rewrite，彻底消除歧义 |
| 22 | axios 封装不完整：没处理 HTTP 401/403、没有下载分支、没防重复提交 | `request.js` 区分 HTTP 层与业务层错误 |
| 23 | 日志文件路径未规定：Windows 下从 IDEA 启动和命令行启动会写到两个目录，排查时以为「日志没了」 | `logback-spring.xml` 统一到工程根 `logs/`，并显式配 `logging.file.name` |
| 24 | `docker-compose.yml` 的 `version: '3.8'` 在 Compose V2 已废弃；Nacos 2.x 还需 9849 端口 | 相关章节整体删除 |

### D. v2 新增的内容（v1 没有）

| 内容 | 位置 |
|---|---|
| 接口文档（springdoc Swagger UI，`/swagger-ui.html`） | `OpenApiConfig` |
| 统一返回 / 全局异常 / 分页 / 自动填充的具体类 | `erp-common`、`erp-framework` |
| 动态菜单与路由的完整方案 | `SysMenuServiceImpl.buildRouters` + 前端 `store/permission.js` |
| 逻辑删除、`BlockAttackInnerInterceptor` 等安全网 | `MybatisPlusConfig` |
| 库存并发的正确做法（带条件的原子更新） | `docs/接口与开发规范.md` §4.4 |
| 跨模块调用的边界（`api` 包） | `erp-mdm/api/MdmApi` |
| 新增一个 CRUD 模块的 SOP | `docs/接口与开发规范.md` §6 |
| 数据库初始化的分步方案（root 只用一次） | `sql/00_bootstrap.sql` + Flyway |

---

## 十、开发阶段不做的事

以下内容**本期不投入精力**，等项目稳定后再补：

- 分库分表、读写分离
- 服务多副本、负载均衡集群
- 多租户隔离
- 分布式事务（Seata）—— 单体下本地事务足够
- 网关、注册中心、配置中心（Nacos）
- Redis（用 Caffeine 替代）
- 消息队列（RocketMQ）
- CI/CD 自动化流水线
- 监控告警、链路追踪（IDEA Debug + 日志足够）
- 数据权限（`sys_role.data_scope` 字段已预留，只实现了「全部」）
- 审批流 / 工作流
- Excel 导入导出
- 定时任务

---

## 十一、后期演进路线

按优先级排列，每一项都标注了「现在留了什么口子」。

| 优先级 | 事项 | 现在已留的口子 |
|---|---|---|
| 1 | **操作日志**（`sys_oper_log` + `@Log` 注解 + AOP） | ERP 上线后第一个被要求的就是「谁改的」 |
| 2 | **数据权限** | `sys_role.data_scope` 字段已建，Service 层加拦截即可 |
| 3 | **Excel 导入导出**（主数据批量导入是刚需） | 无 |
| 4 | **多实例部署 + Redis** | 只需把 `TokenService` 里的 Caffeine Cache 换成 RedisTemplate，**对外接口不变** |
| 5 | **拆微服务** | 业务模块间已只通过 `api` 包调用，把实现换成 Feign 即可；表前缀即库边界 |
| 6 | **对接金蝶云星空** | `mdm_material` / `mdm_customer` / `mdm_supplier` 已预留 `external_code` + `source_system` 字段与索引 |
| 7 | **单据打印 / 导出 PDF** | 无 |
| 8 | **审批流** | 单据表已有 `status` / `audit_by` / `audit_time` |

---

## 十二、本地启动 Checklist

```bash
# 1. 环境：JAVA_HOME 指向 JDK 21，重开终端
mvn -v                      # 期望 Java version: 21.x + UTF-8

# 2. 建库（需要 root，只执行一次）
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p < "E:/AI-Cladue/AI_MyErp/sql/00_bootstrap.sql"

# 3. 启动后端（建表由 Flyway 自动完成，管理员账号自动创建）
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
cd "E:/AI-Cladue/AI_MyErp/erp-server"
mvn clean package -DskipTests
java -jar erp-admin/target/erp-server.jar

# 4. 启动前端
cd "E:/AI-Cladue/AI_MyErp/erp-web"
npm install
npm run dev

# 5. 浏览器打开 http://localhost:3000，用 admin / 123456 登录
```

### 验收清单

**已实测通过**（用 `tools/e2e-verify/e2e_verify.py`，49 项断言全部通过）：

- [x] `mvn -v` 显示 Java 21 与 UTF-8
- [x] 后端全量构建成功，产出可执行 jar
- [x] Flyway 自动建表（`Successfully applied 8 migrations`，版本 v4）
- [x] 初始管理员账号自动创建（BCrypt 哈希由应用生成）
- [x] `GET /api/health` 返回 `{"code":200,...}`
- [x] `http://localhost:8080/swagger-ui.html` 可访问，`/v3/api-docs/*` 返回合法 OpenAPI
- [x] 不带 Token 调 `/api/system/user/list` → **HTTP 401** + 规范 body
- [x] admin/123456 登录拿到 JWT；错误密码被拒（code=400）
- [x] 动态路由返回 4 个一级菜单，一级用 `Layout`、子菜单带 `component`
- [x] 字典接口可按类型取数（前端下拉依赖它）
- [x] 物料/仓库/供应商 CRUD；重复编码被拒且提示可读
- [x] **id 序列化为字符串**（防 JS 精度丢失）
- [x] 建入库单 → 审核 → 台账数量正确、流水写入且变动前后数量正确
- [x] 重复审核同一张单 → **被拒绝，不重复加库存**
- [x] 库存不足时出库被拒 → **且库存未被改动（事务回滚）**
- [x] 采购订单（总金额后端汇总）→ 审核 → 收货 → 确认 → 库存 = 100+60 = **160**
- [x] 重复确认收货 → **被拒绝，不重复入库**
- [x] 采购收货自动生成入库单，可追溯到收货单号；订单状态转「已完成」
- [x] 采购员登录后只剩 2 个菜单；调系统管理接口返回 **HTTP 403**
- [x] 前端 `npm run build` 成功；dev server 起来后**经 Vite 代理**能拿到健康检查与登录 Token

**需要人打开浏览器确认**（脚本验不了渲染与交互）：

- [ ] http://localhost:3000 用 admin/123456 登录，左侧出现 4 个菜单，点进去各页面正常
- [ ] 物料管理页的表格、分页、新增/编辑弹窗、删除确认框都正常
- [ ] 无权限的账号登录后，新增/删除按钮**在界面上确实不显示**（`v-hasPermi` 生效）
- [ ] 表格里的状态列显示成彩色标签（字典 `listClass` 生效）

**尚未验证**：

- [ ] 真正的并发竞争：用 ab/JMeter 同时打同一个审核接口，确认库存没被加两次
      （实现上靠数据库行锁 + 条件更新保证，但没实测过）

---

## 十三、落地踩坑记录（v2 实测，全部已修复）

这一节记的都是「写的时候看不出来、只有真跑起来才暴露」的问题。
按发现顺序排列，附上错误原文 —— 下次遇到同样的报错能直接对上号。

### 1. 自建 logback 配置里用 `%clr` 导致应用完全无法启动

```
Logback configuration error detected:
ERROR - There is no conversion class registered for composite conversion word [clr]
ERROR - Failed to create converter for [%clr] keyword
```

**原因**：`%clr` 不是 logback 内置的转换词，它由 Spring Boot 的
`defaults.xml` 通过 `conversionRule` 注册。自建 `logback-spring.xml` 时如果不 include 它，
logback 在日志系统初始化阶段就抛异常，**应用直接起不来**。
报错看起来像"日志配置问题"，实际是致命启动失败。

**修复**：`logback-spring.xml` 顶部加一行
`<include resource="org/springframework/boot/logging/logback/defaults.xml"/>`。

> 这个坑值得记的原因：它出现在任何业务代码执行之前，且**编译、打包全都正常**，
> 只有真正 `java -jar` 启动才会暴露。所以「构建通过」绝不能当成「能跑」。

### 2. `type-aliases-package` 导致类别名冲突

```
The alias 'TreeSelectVo' is already mapped to the value 'com.erp.mdm.domain.vo.TreeSelectVo'
```

**原因**：`type-aliases-package: com.erp.*.domain` 会把该包下所有类按**简单类名**注册成 MyBatis 别名。
`TreeSelectVo` 作为通用的树形下拉结构被放进了 erp-system，而 erp-mdm 不依赖 erp-system，
于是在 mdm 里又建了一份同名类 —— 两个同名类一进来，别名注册就直接失败。

**修复**（两处一起改）：
1. 把 `TreeSelectVo` 下沉到 `com.erp.common.core.domain`（跨模块共享的结构本就该放 common），删除两份重复。
2. **移除 `type-aliases-package` 配置**。所有 XML 的 `resultType` 都写全限定名，本来就不需要别名；
   留着它等于让任何一次重名类都能炸掉启动。

### 3. knife4j 与 Spring Boot 3.5 不兼容

```
java.lang.NoSuchMethodError:
  'java.util.List org.springdoc.core.properties.SpringDocConfigProperties.getGroupConfigs()'
```

**原因**：knife4j 4.5.0 是按旧版 springdoc 编译的。两种组合都试过：
- 用它传递进来的旧 springdoc —— 版本太旧，与 Boot 3.5 不匹配
- 排除它的 springdoc、换成 2.8.17 —— knife4j 自己的代码找不到那个方法，访问
  `/v3/api-docs/*` 直接 500

而 knife4j 在 Maven Central 上最新就是 4.5.0，没有兼容 springdoc 2.8 的版本。

**修复**：弃用 knife4j，直接用 springdoc 原生 Swagger UI（`/swagger-ui.html`）。
一个打不开的接口文档比朴素但能用的更糟。将来 knife4j 出了兼容版本，把依赖加回来即可。

> **副产物**：这个错误暴露了「业务失败返回 HTTP 200」这个约定的代价 ——
> 该接口返回的是 `{"code":500,...}` 但 HTTP 状态是 200，只看状态码会以为它是好的。
> 所以**验收接口时不能只看状态码，必须看响应体里的 code**。

### 4. `@vitejs/plugin-vue@6` 需要 Node 20.12+

```
[vite:vue] crypto.hash is not a function
```

**原因**：plugin-vue 6.x 用 `crypto.hash()` 算 SFC 缓存 key，而该 API 是 Node 20.12 才有的，本机是 20.11.1。

**修复**：在 `vite.config.js` 顶部给 crypto 模块对象补上该方法（plugin-vue 是默认导入 crypto、
编译时才调用，所以补丁能生效）。**根治办法是把 Node 升到 20.12+ 或 22 LTS，然后删掉补丁。**
这个补丁不引入任何新依赖，且在 Node 20.12+ 上会自动跳过。

### 5. 前后端并行开发导致的字段名不匹配（静默失效）

路由能对上不代表字段能对上。实测发现两处：

| 前端传的 | 后端期望的 | 后果 |
|---|---|---|
| `bizDateStart` | `bizDateBegin` | 起始日期筛选**完全不生效**，且不报错 |
| `materialCode` | 后端无此字段 | 物料编码筛选被忽略，用户看到的是全部数据 |

**这类问题比 404 更危险**：接口调通了、页面也正常，只是结果不对，很容易被当成"数据本来就长这样"。

**修复**：
- 统一命名为 `bizDateStart` / `bizDateEnd`
- 后端补上 `materialCode` 筛选：库存模块不能 join `mdm_material`（模块边界规则），
  所以经 `MdmApi.searchMaterialIdsByKeyword()` 把编码解析成 ID 再按 ID 过滤；
  解析不到任何物料时**返回空页**而不是忽略条件（否则等于"筛选被静默忽略"）

**防复发的手段**：`tools/api-audit/` 下有两个对账脚本，从运行中的应用的 OpenAPI 文档取真实路由
与字段名，与前端代码做集合比对，用退出码表示结果（可直接挂 CI）。用法见该目录的 README。

### 6. 登录查用户时 SQL 漏了一列，导致永远"密码错误"

```
（没有任何异常日志）
接口返回：{"code":400,"msg":"用户名或密码错误"}
```

**原因**：`selectUserByUsername` 的字段列表里漏了 `u.password`。
登录时把密码哈希放进 `LoginUser` 交给 `DaoAuthenticationProvider` 做 BCrypt 比对，
哈希为 null 时比对必然失败，于是抛出正常的 `BadCredentialsException` 并被翻译成
「用户名或密码错误」——**全程没有异常，日志里只有一条 WARN**。

**为什么难查**：账号密码确实是对的，提示却说是密码错。而 SQL 少一个列名，
编译器、IDE、静态检查全都没有意见。

**修复**：该查询补上 `u.password`（**仅此一处**，列表查询不带）。
安全性由实体上的 `@JsonProperty(WRITE_ONLY)` 保证：允许读取但不参与序列化。

**教训**：手写 SQL 的字段列表是"编译器管不到"的区域，改表结构或改实体后要人工核对。

### 7. `@PreAuthorize` 拒绝时返回的是 HTTP 200

**现象**：无权限的用户调接口，响应体是 `{"code":403}` 但 HTTP 状态码是 200。

**原因**：`@PreAuthorize` 是在 Controller 调用过程中抛异常的，
会被 `@RestControllerAdvice` 先接住（而不是走到 Security 的 `AccessDeniedHandler`）。
异常处理器默认返回 HTTP 200，于是与契约里「403 用真实状态码」不符。

**为什么这不是小事**：前端要靠状态码区分「登录过期（清 Token 跳登录页）」
与「无权限（只弹提示，绝不能把人踢下线）」。混在 HTTP 200 里，前端就只能靠 body.code 判断，
而这恰恰是当初规定「401/403 用真实状态码」想避免的。

**修复**：该处理器改用 `ResponseEntity` 返回 `HttpStatus.FORBIDDEN`。

### 8. 业务错误码与系统错误码都是 500，排障时无法区分

**现象**：看到 `code=500` 时，分不清是「用户操作不合法」还是「我们的代码抛异常了」。
实测中就因此多绕了一圈——登录失败只看到一个 500，得去翻日志才知道是业务错误。

**修复**：`BUSINESS_ERROR` 从 500 改为 **400**。
从此 **`body.code=500` 必定意味着程序有 bug**，值得立刻报警；
400 则是正常的业务拒绝。

### 9. 前端构建失败：`crypto.hash is not a function`

`@vitejs/plugin-vue@6` 内部用了 Node 20.12+ 才有的 `crypto.hash()`，本机 Node 20.11.1 没有。
已在 `vite.config.js` 顶部加兼容补丁（Node 升级到 20.12+ 后可删）。

### 10. 「顺手消个告警」反而让真正的错误更难懂

**现象**：用错 JDK（本机 `JAVA_HOME` 指向 JDK 8）时，构建报的是

```
Fatal error compiling: 无效的标记: -proc:full
```

**原因**：`-proc:full` 是本工程为了让 JDK 21 下的一条注解处理告警闭嘴而加的编译参数，
但它是 **JDK 21 才有的选项**。JDK 8 的 javac 不认识它，于是在处理 `release 21` 之前
就先因"无效标记"失败。

**为什么这是个教训**：不加这个参数时，JDK 8 会直接报 `invalid target release: 21`，
**一眼就能看出是 JDK 问题**。加了之后，错误信息指向一个用户从没听说过的编译参数，
排障时要绕一大圈才知道是 JDK 用错了。

**修复**（两处）：
1. 把 `-proc:full` 换成 `-Xlint:-options` —— 同样能消掉告警，但它在 JDK 8 上也是合法选项，
   于是 JDK 8 下的报错回到清晰的 `invalid target release: 21`
2. 增加 `maven-enforcer-plugin` 做 JDK 版本前置检查，JDK 不对时**构建第一步就停下**，
   并直接说明「Maven 读的是 JAVA_HOME 不是 PATH」以及怎么改

**通用教训**：优化错误信息的改动，要验证「出错时是否更容易看懂」，
而不只是验证「正常情况是否更干净」。同类问题还有一处 —— 提示信息用英文（ASCII）打头，
中文附在后面：因为控制台代码页不固定，关键诊断信息如果只有中文，
在别人的终端里可能显示成乱码，那这行提示就白写了。

---

## 十四、常见问题

| 现象 | 原因 |
|---|---|
| `invalid target release: 21` | `JAVA_HOME` 还指着 JDK 8 |
| `Public Key Retrieval is not allowed` | 连接串缺 `allowPublicKeyRetrieval=true&useSSL=false` |
| 启动成功但所有接口 404 | 启动类不在 `com.erp` 根包，组件扫描没覆盖业务模块 |
| `Invalid bound statement (not found)` | `mapper-locations` 没写 `classpath*:`，扫不到模块 jar 里的 XML |
| 分页查出全部、`total` 不对 | XML 里 `IPage` 参数没放在第一位（不报错，静默失效） |
| 删掉的数据还在列表里 | 手写 SQL 忘了加 `is_deleted = 0` |
| 新增/修改时 `createBy` 是 null | 实体没继承 `BaseEntity`，或 `MetaObjectHandler` 没生效 |
| 菜单加了但前端看不到 | `sys_menu.component` 与 `erp-web/src/views/` 下的路径没逐字对应 |
| 按钮可见但点击 403 | 菜单表 `perms` 与 `@PreAuthorize` 的权限串不一致 |
| 时间差 8 小时 | 连接串缺 `serverTimezone=Asia/Shanghai`，或容器时区不对 |
| 「没权限」被报成 500 | `GlobalExceptionHandler` 少了 `AccessDeniedException` 的处理器 |
| 中文乱码 | Maven 用 JDK 8 跑（platform encoding=GBK），或响应没设 UTF-8 |
