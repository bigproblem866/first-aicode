# CLAUDE.md

给 AI 助手的项目说明。**这个文件会被自动读取**，所以只写「每次开工都必须知道、且从代码里看不出来」的东西。
详细信息一律指向 docs/，不在这里重复。

---

## 一、这是什么

**模块化单体** ERP（不是微服务）：一个 Spring Boot 进程 + Maven 多模块分包。
包边界就是将来拆微服务的切缝，但**本期不拆**。

阶段是 MVP 骨架：本地能跑、结构清晰、方便加模块。不做分库分表、多租户、微服务、Docker。

## 二、先读哪个文档

| 要做什么 | 读哪份 |
|---|---|
| **写任何后端代码前** | `docs/接口与开发规范.md` —— 路径/权限串/返回格式/并发写法/新增模块 SOP，**必读** |
| 查「某个东西验证过没有」「为什么这么选」 | `docs/来源与设计笔记.md` —— 已验证/未验证清单 + 评审记录 |
| 查架构全貌、建表规范 | `ERP系统开发环境设计文档.md` |
| 环境准备 / 启动 / 验收步骤 | `README.md` |

**`docs/接口与开发规范.md` 里有两节是硬性要求，改代码前务必看**：
- 「五之二、单据状态流转：并发下唯一正确的写法」—— 状态流转**必须**用带条件的原子 UPDATE
- 「六、新增一个 CRUD 模块的 SOP」—— 新增模块照它的文件清单来

---

## 三、环境硬约束（不要随手升级）

这几条是**实测本机环境反推出来的**，不是口味偏好：

| 约束 | 原因 |
|---|---|
| **`JAVA_HOME` 必须是 JDK 21** | 系统变量指向 JDK 8，Maven 读 `JAVA_HOME` 而非 `PATH`。不设的话报 `invalid target release: 21` |
| **Spring Boot 3.5.x，不上 4.x** | 4.x 是 Spring Framework 7 新主线，生态跟进有限 |
| **Vite 6.x / vue-router 4.x，不上 7/8/5** | Vite 7+ 要求 Node ≥20.19，本机 Node 20.11.1 不满足；vue-router 5 强制要求 Vite 7+ |
| **不引入 Redis / Nacos / Docker / MQ** | 本机都没有。缓存用 Caffeine（进程内），单体单进程足够 |
| **MySQL 8 用本机 MySQL80 服务** | 已在 3306 跑，不装第二个 |

版本都经 Maven Central / npm 实测确认存在，**升级前先确认本机 Node/JDK 是否满足**。

---

## 四、模块结构与边界规则

```
erp-server/
├── erp-common/        统一返回 R、分页、BaseEntity、异常、工具类（无业务依赖）
├── erp-framework/     安全(JWT) / MyBatis-Plus / 异常处理 / Caffeine / 接口文档
├── erp-modules/
│   ├── erp-system/    登录认证 + 用户/角色/菜单/部门/字典
│   ├── erp-mdm/       物料·分类·客户·供应商·仓库
│   ├── erp-inventory/ 库存台账·出入库单·库存流水
│   └── erp-purchase/  采购订单·采购收货
└── erp-admin/         启动类 `com.erp.ErpApplication` + yml + Flyway 建表脚本
```

依赖方向单向：`erp-admin → erp-modules/* → erp-framework → erp-common`。

### 跨模块调用规则（重要）

**业务模块之间只能通过对方 `api` 包里的接口/DTO 互相调用，禁止直接注入别人的 Service 或访问别人的表。**

- ✅ 正面例子：`erp-purchase` 用 `com.erp.mdm.api.MdmApi` 查物料名/仓库，用
  `com.erp.inventory.api.dto.InboundRequest` 发起入库
- ⚠️ **已知偏离（有意的技术债）**：`PurReceiptServiceImpl` 直接注入了
  `com.erp.inventory.service.IInvStockService`，绕过了 `api` 包。
  MVP 阶段图快，正确做法是在 `erp-inventory/api` 下建接口（那个目录已存在，只有 dto）。
  **新代码不要照抄这个写法**。

---

### 前端布局约定（2026-10 重构后）

骨架（参考金蝶 AI 套件布局图）：
**白色顶栏（52px，含品牌 + 菜单入口 + 搜索 + 功能入口 + 用户）+
页签（40px）+ 内容区 `#f2f2f2`（全宽，16px 边距）**。

### 布局基准：采购订单（2026-10 用户确认，以后新页面与改造页面都照它做）

- **列表页基准** `views/purchase/order/index.vue`：
  - 不用 `PageHeader`，**没有页面标题与说明**
  - 查询区：`<el-card class="search-card is-compact">` + `<el-form size="small">`，
    输入/下拉宽度 120~160px，搜索/重置按钮 `size="small"`
  - 表格工具条：左侧「新增 / 删除」（`size="small"`），右侧 `RightToolbar`
  - 表格无 `border` / `stripe`，点击行进入卡片页，分页右对齐
- **卡片页基准** `views/purchase/order/card.vue`：
  - 不用 `PageHeader`，顶部用 `DocToolbar`（按钮 small；状态不可用则置灰 + tooltip）
  - 单头卡片用全局类 `.bill-card`（定义在 `index.scss`）：**宽度自适应**
    （`width: 100%`），大屏限制在 `--page-max-width`（1400px）并居中；
    内边距 14/16、表单项间距 14px、标签字号 13px
  - 表单 `<el-form size="small">`；明细表 `border size="small"`，
    表内输入控件也加 `size="small"`
  - **不要在页面里再写 `.bill-card { max-width: ... }`**：scoped 优先级更高，会覆盖全局
  - 路由 `meta.noTag: true`，不新建标签
- **全局基准**：顶栏含菜单图标与搜索框，内容区不显示面包屑（见下文各条）
- **存量页面已全部对齐**（2026-10）：所有列表页（system / mdm / inventory）都已按上面基准改造完成

- **没有常驻左侧栏**：菜单收进顶栏品牌区左侧的 ☰ 图标（`layout/components/MenuLauncher.vue`），
  悬停或点击弹出 265px 浮层，覆盖在内容上，关闭后内容区恢复全宽。
  浮层必须 `<Teleport>` 到 body —— 外层布局有 `overflow: hidden`，写在组件内会被裁掉。
  关闭时机：移出触发区/浮层（延迟 150ms）、点击可导航菜单项、`Esc`、点空白、路由变化。

- 顶栏是**白底**（`--header-bg` = `#ffffff`），与侧栏、搜索栏共用同一套红白主题；
  红色只用在品牌标记、AI 入口和「建议与反馈」按钮上。
  参考图本身是深蓝顶栏，这里刻意不跟随 —— 大面积深色会让「红白」割裂成两张皮。
  改配色只需改 `variables.scss` 的 `$header-bg` / `$header-text` / `$header-text-muted`。
- 搜索框在**顶栏右侧**：`layout/components/GlobalSearch.vue` 渲染在
  `GlobalHeader.vue` 的 `.gh-right` 里（排在 AI 图标之前），宽 240px、
  `size="small"`、浅灰填充，占位文字「请输入应用/业务对象」；
  紧跟「功能导航」九宫格按钮。**搜索栏不单独占一行**，相关高度令牌已删除。
- **内容区不显示面包屑**：当前页位置由「侧栏高亮 + 标签栏」表达
  （`Breadcrumb.vue` 组件保留未使用，需要时在 `AppMain.vue` 加回一行即可）。
- **菜单浮层只渲染到二级目录**（2026-10，对齐参考图）：
  - 一级目录（供应链）可展开
  - 二级目录（库存管理 / 采购管理）渲染成**可点菜单项**，点击**不跳转**，
    而是在菜单浮层右侧展开一张子菜单卡片（`MenuLauncher.vue`），列出该目录的三级功能；
    **三级功能不在菜单里出现**，底层页面保持不变
  - 点卡片里的三级功能才跳转，两层浮层一起收起
  - 停在三级功能页时，菜单高亮对应的二级目录项
    （`Sidebar/index.vue` 的 `activeMenu` 做了反向查找）
- **功能导航页** `/module/:key`（`views/module/index.vue`）：按 `group_name` 分区块、
  多列列出全部功能，数据来自已下发菜单（加菜单自动出现）。
  入口：二级目录的「工作台」标签、子菜单卡片右上的「工作台」，或搜索栏右侧的九宫格按钮。
- 功能导航页的**分区标题**来自 `sys_menu.group_name`（V5 迁移新增，随菜单接口下发）。
  为空的菜单回退到所属目录名。新增功能时若想单独成区，
  在 `R__seed_system_menu_group.sql` 里补一行 UPDATE。
- 菜单浮层：宽 265px、行高 50px、字号 14px；选中项是**通栏**浅色底 + 主色文字，
  没有左侧指示条（参考图没有）。
- 二级及以上目录的标题右侧有「**工作台**」标签（`SidebarItem.vue`），点击进入该目录的
  功能导航页 `/module/<目录路径>`；默认隐藏、悬停出现，当前所在目录常亮。
  一级目录不显示标签（参考图里只有展开箭头）。
- **菜单里没有「首页」**：首页固定存在于标签栏（`affix`），所以 `constantRoutes` 里的
  `/` 路由标了 `hidden: true`，避免菜单和标签重复。路由本身仍然注册，
  登录后 `/` → `/dashboard` 的重定向不受影响。
- **不进标签栏的路由用 `meta.noTag: true` 标记**（`TagsView.vue` 统一判断）：
  - 功能导航页（`ModuleNav`）：二级目录的启动器、不是业务页，且所有模块共用一个
    `meta.title`，进标签会变成多个同名标签
  - 单据卡片页：从列表点进来，标签栏保留列表那个标签即可
  - 停在这类页面时标签栏无高亮项，但侧栏高亮（`activeMenu`）仍然正确
- **标签栏支持右键菜单**（关闭当前 / 关闭其他 / 关闭全部，`TagsView.vue`）：
  关闭当前页后跳**右邻居**（不是数组最后一个）；固定标签（`affix`，首页）不可关闭。
  菜单必须 `Teleport` 到 body —— `.tags-scrollbar` 有 `overflow: hidden`，写在组件内会被裁掉。
- **标签支持拖动排序**（原生 HTML5 DnD，无第三方依赖）：
  - 排序逻辑在 `store/modules/tagsView.js` 的 `moveView(fromIndex, toIndex)`，
    `toIndex` 是插入前的目标位置（0..length），调用方不用处理先删后插的下标偏移
  - **整个 `.tags-wrap` 是拖放热区**：插入位置按每个标签的水平中点计算，
    落在标签之间的空隙或右侧空白也能排序，不需要精确对准某个标签
  - 首页（`affix`）固定第一位：`ensureAffixViews()` 从 `router.getRoutes()`
    找 `meta.affix` 路由并补进标签栏，`addVisitedView` 对 affix 项做头部插入；
    它不能被拖动，插入位置也会被钳制到它之后
  - 目标位置显示 2px 主色插入线；被拖标签半透明
  - **顺序不持久化**：刷新后恢复默认（与「标签本身不持久化」策略一致）
  - `cachedViews` 不需要跟着排序 —— keep-alive 按 `route.path` 缓存，顺序无意义

用户已确认的两项：菜单浮层**保持白色**、骨架**保持顶栏通栏**。

- **颜色只有两个来源**：`assets/styles/variables.scss`（SCSS 令牌）与
  `assets/styles/element-override.scss`（Element Plus CSS 变量）。
  不要在页面里写死色值；改主色时两处都要改，且派生色
  `--el-color-primary-light-*` 必须一起换，否则按钮 hover 会跳回旧色。
- `PageHeader` 现在**只用于工作台与功能导航页**；列表页与单据页都不再用它
  （布局基准见上面的「采购订单」一节）。
- **AI 助手**在顶栏右上角，面板是 `layout/components/AiAssistant.vue`。
  后端没有对话接口前**不要伪造回答**。
- **所有列表页已按采购订单基准对齐**（2026-10）：
  - 系统管理、基础资料、库存查询/流水、采购、库存单据等列表页统一为：
    无 `PageHeader` + 紧凑查询区（`search-card is-compact`）+
    「新增 / 删除」在表格工具条左侧 + 表格无 `border`/`stripe`
  - **单据页不用 `PageHeader`**：标题与说明已省略，操作按钮见下面的「单据操作条」
  - `system/dict` 左右双栏、`dashboard` 统计卡、`login` 左右分栏
  - `error/401` 与 `error/404` 共用 `components/ErrorPage`（改样式只改一处）
  - 新页面照列表页的结构写
- **弹窗里的明细编辑表刻意保留 `border`**：多行输入时没有格线容易看串行。
- **主数据也采用「列表 → 卡片页」范式**（2026-10）：
  - 列表点行进入卡片页，**操作列已全部移除**；新增/修改弹窗已下线
  - 卡片路由（`constantRoutes`，均带 `meta.noTag` + `activeMenu`）：
    - `/system/{user,role,menu,dept}/:id`
    - `/mdm/{material,materialCategory,customer,supplier}/:id`
    - `/supply/inventory/warehouse/:id`（仓库菜单挂在库存管理下，不是 /mdm）
  - 工具条按钮：
    - 通用：`新增`、`删除`（多选）
    - 需要单条目标的按**恰好勾选 1 行**启用：用户「重置密码」/「分配角色」、
      角色「分配权限」、菜单/部门/物料分类「新增下级」
    - 用户「分配角色」弹窗保存时先 `getUser(id)` 拿完整数据再改 `roleIds`：
      `roleIds` 不是表字段，只传 `{ id, roleIds }` 会让 updateById 没有任何 SET 列
  - 树形页的「新增下级」通过 `?parentId=` 预选上级；菜单还带
    `?parentType=`，卡片据此决定默认菜单类型（目录下建菜单，其余建按钮）
  - 角色分配权限、用户重置密码仍是弹窗（需要树/表单交互，不适合单独开卡片）
- **单据统一采用「列表 → 卡片页」范式**（2026-10）：
  - 列表只负责浏览：点击任意一行进入卡片页；点击勾选框不触发跳转（`column.type === 'selection'` 直接 return）
  - 「新增」进入空白卡片：`/{模块}/{单据}/new`
  - 卡片页是**常驻路由**（`src/router/index.js` 的 `constantRoutes`），路径带 `:id`，
    用 `Layout` 包一层，`meta.activeMenu` 让侧栏继续高亮对应列表菜单
  - 卡片页 **不新建标签**（路由 `meta.noTag: true`）：从列表点行进入时，
    标签栏继续高亮列表那个标签；直接刷新卡片 URL 时标签栏无高亮，属预期
    （标签不持久化，见 `store/modules/tagsView.js`）
  - 列表被 keep-alive 缓存，**返回时必须在 `onActivated` 里刷新**，否则看到旧数据
  - 卡片页按状态决定可编辑性：草稿可改，已审核/已作废只读；按钮可见性要**与后端规则一致**
    （例如库存单只有「已审核」能作废，草稿不行）
  - 现有卡片页：`inventory/order/card.vue`（入库/出库共用）、
    `purchase/order/card.vue`、`purchase/receipt/card.vue`
  - **新增单据页面照这个范式做，不要再用弹窗编辑单据**
- **单据操作条（2026-10）**：
  - 列表页：去掉标题区，「新增 / 删除」放在表格工具条的左侧（`.toolbar-left`）
  - 单据列表页的查询区用 `class="search-card is-compact"` + `<el-form size="small">`：
    控件更矮、内边距与行距更小（样式在 `assets/styles/index.scss` 的 `.is-compact`）；
    输入/下拉宽度按 120~160px 配置，搜索/重置按钮也加 `size="small"`
  - 卡片页：顶部用全局组件 `DocToolbar`（`components/DocToolbar/index.vue`），
    按钮由 actions 数组配置，**不要再在模板里写一串 v-if**
  - 按钮三态规则：
    1. 状态不可用 → **常驻置灰 + tooltip 说明原因**（已审核单的「保存」就是这种）
    2. 业务上不存在 → 不配置（`visible: false`），例如库存单没有「下推」
    3. 无权限 → 不渲染（`DocToolbar` 内部用 `checkPermi` 过滤）
  - 删除按钮按**选中行状态**判断：三张单据后端都只允许草稿删除，混选非草稿时置灰
  - 「下推收货」：采购订单（已审核/部分收货）跳到
    `/supply/purchase/receipt/new?orderId=<id>`，收货卡片读取该参数自动预选订单
  - ⚠ disabled 的 el-button 不触发鼠标事件，tooltip **必须包一层 `<span>`**
    （`DocToolbar` 已处理；自己写列表页按钮时也要这样）

## 五、常用命令

### 后端（**先设 JAVA_HOME，否则必失败**）

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
cd "E:/AI-Cladue/AI_MyErp/erp-server/erp-admin"
mvn spring-boot:run                      # 开发启动（改代码不用重打包）
```

> 必须在 `erp-admin` 目录下跑。根目录 `erp-server` 是聚合 POM（packaging=pom），
> 没有启动类，在那跑 `spring-boot:run` 会报找不到主类。
>
> ⚠ **改了 `erp-system` / `erp-mdm` 等兄弟模块的代码后，这条命令不会生效**：
> 它只重编译 `erp-admin`，兄弟模块用的是本地仓库里的旧 jar。
> 现象是「代码明明改了，接口行为没变」。正确做法（二选一）：
> ```bash
> # 方式一：先全量安装，再启动
> cd "E:/AI-Cladue/AI_MyErp/erp-server" && mvn clean install -DskipTests
> cd erp-admin && mvn spring-boot:run
>
> # 方式二：从根目录带 -am 启动（会先构建依赖模块）
> cd "E:/AI-Cladue/AI_MyErp/erp-server"
> mvn -pl erp-admin -am spring-boot:run
> ```

```bash
cd "E:/AI-Cladue/AI_MyErp/erp-server"
mvn clean package -DskipTests            # 打包 → erp-admin/target/erp-server.jar
```

**不连数据库的冒烟启动**（排查「是装配问题还是数据库问题」时用，很有价值）：

```bash
cd erp-server/erp-admin/target
"$JAVA_HOME/bin/java" -jar erp-server.jar \
  --spring.datasource.hikari.initialization-fail-timeout=-1 \
  --spring.flyway.enabled=false --erp.init.enabled=false
```

### 前端

```bash
cd "E:/AI-Cladue/AI_MyErp/erp-web"
npm run dev                              # http://localhost:3000，/api 代理到 8080
```

### 验收（改完必跑）

```bash
cd "E:/AI-Cladue/AI_MyErp/tools/e2e-verify"
python -I e2e_verify.py                  # 49 项断言，退出码表结果

cd ../api-audit
python -I ApiAudit.py   E:/AI-Cladue/AI_MyErp/erp-web/src http://localhost:8080  # 路由对账
python -I FieldAudit.py E:/AI-Cladue/AI_MyErp/erp-web/src http://localhost:8080  # 字段名对账
```

**前后端字段名不一致是静默失效**（接口 200、页面正常、数据不对），
改了前端 queryParams / prop 就重跑 `FieldAudit.py`。

---

## 六、写代码时的硬性约定

违反这些不会编译报错，但会在运行时出问题：

1. **单据状态流转必须原子**。不能「select 查状态 → 判断 → update」，并发下两个请求都会通过。
   用 `LambdaUpdateWrapper` 带 `.eq(状态, 期望值)` 并按影响行数判断，且放在动数据**之前**。
   例见 `InvOrderServiceImpl.transitStatus`。
2. **手写 XML 的查询要自己加 `is_deleted = 0`**。MyBatis-Plus 的逻辑删除只作用于自动方法。
3. **金额/数量统一 `DECIMAL(18,4)`**，Java 侧用 `BigDecimal`。
4. **`Long` 序列化成 String**（防 JS 精度丢失）。只转包装类型，`long total` 保持数字。
5. **业务失败返回 HTTP 200 + `body.code=400`**；但 401/403 用真实 HTTP 状态码
   （前端据此区分「登录过期」和「无权限」）。
   → **验收必须看响应体里的 `code`，只看 HTTP 状态码会把失败当成功**。
6. **`body.code=500` 只可能意味着程序有 bug**（业务错误按 400 返）。
7. **权限用 `@PreAuthorize("@ss.hasPermi('mdm:material:add')")`**，
   权限串必须与 `sys_menu` 表里的 `perms` 完全一致，否则按钮看得见但点了 403。
8. **新建表要加 `V6__xxx.sql`**（V5 已被「菜单功能分组」占用），不要改已执行过的 V 脚本（Flyway 校验 checksum，改了启动直接报错）。
   `R__` 开头的可重复脚本要写成幂等的。
9. **前端菜单的 `component` 值必须与 `erp-web/src/views/` 下路径逐字对应**，否则菜单加了但页面加载不出来。
10. **管理员判断按用户名/角色名，不要按 ID == 1**（主键自增，重建库后就不是 1 号了）。
11. **Vue 的 `<style scoped>` 默认是普通 CSS**：注释用 `/* */`，写 `//` 会直接导致构建失败
    （`[vite:vue] Unexpected '/'`）。除非显式写 `lang="scss"`。
12. **不要在 `el-menu` 上加 `background-color` / `text-color` / `active-text-color`**：
    它们会被 Element 写成 CSS 变量注入，压过 `sidebar.scss` 的统一配色。
13. **单据页面用「列表 + 卡片」范式**，不要用弹窗编辑单据。
    卡片路由登记在 `constantRoutes`（路径带 `:id`），列表用 `@row-click` 进入，
    并在 `onActivated` 里刷新列表（keep-alive 缓存不会自动重新请求）。

---

## 七、当前状态

**已验证**（可复现，详见 `docs/来源与设计笔记.md` 第二节）：
后端构建与启动、Flyway 建表、前端构建、Vite 代理链路、
**端到端业务闭环 49/49 断言通过**（鉴权、权限隔离 401/403、基础资料 CRUD、
库存闭环含重复审核被拒/库存不足被拒/事务回滚、采购入库闭环 100+60=160）、
路由对账 92↔92、字段对账 0 不匹配。

**未验证**（改动这些区域时要格外小心，也欢迎顺手补验）：
- 浏览器里的真实交互与渲染（表单校验、`v-hasPermi` 是否真的隐藏按钮、表格列、字典标签）
  —— 注意：**49 项断言全过也抓不到前端交互问题**，第 15 条评审记录就是这么漏掉的
- 真正的并发竞争（脚本是串行的；实现靠数据库行锁+条件更新，逻辑正确但未实测）

**有意未实现**：数据权限（只有"全部"）、操作日志、Excel 导入导出、金蝶对接（只留了
`external_code` / `source_system` 字段）。

---

## 八、这个文件本身

改了架构决策、模块边界、环境约束、验收方式，**回来更新这里**。
踩到新的坑，记到 `docs/来源与设计笔记.md` 的「代码评审记录」表里，别只写在这里。
