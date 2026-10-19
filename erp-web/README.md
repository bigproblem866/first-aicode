# erp-web —— ERP 管理系统前端

Vue 3 + Vite + Element Plus + Pinia + Vue Router 的后台管理前端。
接口契约以 `docs/接口与开发规范.md` 为准，本 README 只讲前端怎么跑、怎么加页面。

## 环境要求

- Node.js 20.x（本机验证版本 **v20.11.1**）
- npm 10.x

> 依赖版本是**锁定**的，请勿随意升级：
> `vite` 停留在 6.x —— Vite 7/8 的 `engines` 要求 `^20.19.0 || >=22.12.0`，在 Node 20.11.1 上装不上。
> `vue-router` 停留在 4.x —— 5.x 依赖 Vite 7/8。
> 未引入 `unplugin-auto-import` / `unplugin-vue-components`，Element Plus 全量引入，减少构建层的不确定性。

## 启动

```bash
cd erp-web
npm install

# 国内网络慢时改用镜像源
npm install --registry=https://registry.npmmirror.com

npm run dev      # 开发服务器 http://localhost:3000
npm run build    # 产物输出到 dist/
npm run preview  # 本地预览 dist
```

开发时 `vite.config.js` 把 `/api` 代理到 `http://localhost:8080`（网关），**不做任何 rewrite**：
后端 Controller 上写的就是 `/api/xxx` 全路径，去掉前缀反而会 404。

## 目录结构

```
src/
├── api/                接口层，按模块分文件（页面里禁止直接 import axios）
│   ├── auth.js
│   ├── system/         user / role / menu / dept / dict
│   ├── mdm/            material / materialCategory / customer / supplier / warehouse
│   ├── inventory/      stock / order / stockLog
│   └── purchase/       order / receipt
├── assets/styles/      index.scss（入口）+ variables.scss + sidebar.scss
├── components/         Pagination / DictSelect / DictTag / RightToolbar（已全局注册）
├── directive/          v-hasPermi / v-hasRole
├── layout/             主框架：Sidebar（递归 SidebarItem）/ Navbar / TagsView / AppMain
├── router/             只放常量路由，业务路由由后端下发
├── store/modules/      user / permission / app / tagsView / dict
├── utils/              request（axios 封装）/ auth / permission / dict / format
├── views/              页面，目录结构与后端菜单 component 字段逐字对应
├── permission.js       全局路由守卫
└── main.js
```

## 关键约定

### 1. 接口返回

后端统一返回 `{ code, msg, data }`。`src/utils/request.js` 的响应拦截器已经把外层拆掉，
**页面里拿到的就是 `data` 本身**，不需要再 `.data`：

```js
// 列表：data 形如 { total, rows }
const res = await listMaterial(query)
list.value = res.rows
total.value = res.total
```

失败分两类，拦截器已分开处理：

| 情况 | 处理 |
|------|------|
| HTTP 401（未登录 / Token 过期） | 清 Token → 跳登录页 → 提示「登录已过期」 |
| HTTP 403（已登录无权限） | **只弹提示**，绝不清 Token、绝不跳登录页 |
| HTTP 200 但 `code !== 200`（业务失败） | `ElMessage.error(res.msg)` 并 reject |

### 2. 动态路由

业务菜单不在前端硬编码。登录后调 `GET /api/auth/routers`，后端返回的 `component` 是字符串：

- `Layout` → `src/layout/index.vue`
- `ParentView` → `src/layout/components/ParentView/index.vue`
- `system/user/index` → `src/views/system/user/index.vue`

映射发生在 `store/modules/permission.js`，用 `import.meta.glob('../../views/**/*.vue')` 查表。
**新增页面时，目录和文件名必须与菜单表的 `component` 字段逐字一致**，否则动态路由找不到组件。

### 3. 权限

- 按钮：`<el-button v-hasPermi="['mdm:material:add']">`，权限串取自 `接口与开发规范.md`
- 字典下拉：`<DictSelect v-model="form.status" type="sys_normal_disable" />`，不要手写 options
- 状态列：`<DictTag :value="row.status" type="sys_normal_disable" />`，颜色取字典的 `list_class`

### 4. 主键是字符串

后端把 `Long` 主键序列化成了字符串（防止 JS 精度丢失）。
**比较 id 时用 `String(a) === String(b)` 或 `==`，不要拿字符串直接和数字做 `===`。**

### 5. 入库单 / 出库单共用一个组件

菜单表里「入库单」和「出库单」的 `component` 都是 `inventory/order/index`，只有 `path` 不同
（`inbound` / `outbound`）。页面按路由判断 `orderType`：

1. 优先取 `route.query.orderType`
2. 否则按 `route.path` 判断（`outbound` → `OUT`，其余 → `IN`）

## 新增一个 CRUD 页面

1. 在 `src/api/{模块}/{资源}.js` 里加接口（照抄 `api/mdm/material.js`）
2. 在 `src/views/{模块}/{资源}/index.vue` 建页面（照抄 `views/mdm/material/index.vue`，它是模板）
3. 后端菜单表加一行：`component` 填 `{模块}/{资源}/index`，`perms` 与页面里的 `v-hasPermi` 保持一致
4. 重新登录（或清掉 Token 后刷新）让前端重新拉取动态路由

## 已实现的页面

| 路径 | 说明 |
|------|------|
| `login/index` | 登录页 |
| `dashboard/index` | 首页 |
| `error/401`、`error/404` | 错误页 |
| `system/user` | 用户管理（含角色分配、重置密码） |
| `system/role` | 角色管理（含菜单权限分配树） |
| `system/menu` | 菜单管理（树形表格） |
| `system/dept` | 部门管理（树形表格） |
| `system/dict` | 字典管理（左类型右数据） |
| `mdm/material` | 物料管理（CRUD 模板页） |
| `mdm/materialCategory` | 物料分类（树形表格） |
| `mdm/customer` / `mdm/supplier` / `mdm/warehouse` | 客户 / 供应商 / 仓库 |
| `inventory/stock` | 库存查询（只读） |
| `inventory/order` | 出入库单（入库/出库共用，含明细编辑与审核） |
| `inventory/stockLog` | 库存流水（只读） |
| `purchase/order` | 采购订单（主从表单 + 审核） |
| `purchase/receipt` | 采购收货（选单收货 + 确认收货） |

## 已知简化

- **keep-alive** 用 `:max="20"` + `route.path` 作 key，而非 `include` 按组件名匹配：
  视图文件没有声明 `name`，用 `include` 一个也缓存不上。代价是关闭标签页不会立即销毁缓存实例。
- **标签页关闭** 不会强制刷新被缓存的页面。
- 首页的统计卡片是占位符（显示 “—”），等业务统计接口定了再接。
