import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 常量路由：不需要权限、所有人可访问的部分。
 * 业务路由（system / mdm / inventory / purchase）全部由后端下发，见 store/modules/permission.js。
 */
export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/401',
    name: 'NoAuth',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    // hidden：首页固定存在于标签栏（affix），菜单里不再重复放一个「首页」入口。
    // 路由本身仍然注册，登录后 redirect 到 /dashboard 不受影响。
    hidden: true,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true }
      }
    ]
  },

  // -------------------------------------------------------------------------
  // 单据卡片页：从列表点某一行进入，URL 形如 /supply/inventory/inbound/12
  //
  // 为什么不放进后端菜单表：
  //   这类页面的路径带参数（:id），不适合做菜单项；也没有自己的权限点，
  //   权限仍由列表菜单（inv:order:list）与按钮权限控制。
  // 用 Layout 包一层，卡片页才能在主框架内渲染（顶栏/侧栏/页签都还在）；
  // meta.activeMenu 让侧边栏继续高亮对应的列表菜单。
  // -------------------------------------------------------------------------
  {
    path: '/supply/inventory/inbound/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'InventoryInboundCard',
        component: () => import('@/views/inventory/order/card.vue'),
        // noTag：卡片页不新建标签，标签栏保留列表那个标签
        meta: { title: '入库单', activeMenu: '/supply/inventory/inbound', noTag: true }
      }
    ]
  },
  {
    path: '/supply/inventory/outbound/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'InventoryOutboundCard',
        component: () => import('@/views/inventory/order/card.vue'),
        meta: { title: '出库单', activeMenu: '/supply/inventory/outbound', noTag: true }
      }
    ]
  },
  {
    path: '/supply/purchase/order/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'PurchaseOrderCard',
        component: () => import('@/views/purchase/order/card.vue'),
        meta: { title: '采购订单', activeMenu: '/supply/purchase/order', noTag: true }
      }
    ]
  },
  {
    path: '/supply/purchase/receipt/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'PurchaseReceiptCard',
        component: () => import('@/views/purchase/receipt/card.vue'),
        meta: { title: '采购收货', activeMenu: '/supply/purchase/receipt', noTag: true }
      }
    ]
  },

  // -------------------------------------------------------------------------
  // 主数据卡片页：从列表点行进入（list → card 范式，与采购订单一致）
  //
  // 路径带 :id，新增时 id 为 'new'。meta.activeMenu 让菜单/标签继续高亮列表项，
  // meta.noTag 表示卡片页不单独占一个标签。
  // -------------------------------------------------------------------------
  {
    path: '/system/user/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'SystemUserCard',
        component: () => import('@/views/system/user/card.vue'),
        meta: { title: '用户', activeMenu: '/system/user', noTag: true }
      }
    ]
  },
  {
    path: '/system/role/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'SystemRoleCard',
        component: () => import('@/views/system/role/card.vue'),
        meta: { title: '角色', activeMenu: '/system/role', noTag: true }
      }
    ]
  },
  {
    path: '/system/menu/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'SystemMenuCard',
        component: () => import('@/views/system/menu/card.vue'),
        meta: { title: '菜单', activeMenu: '/system/menu', noTag: true }
      }
    ]
  },
  {
    path: '/system/dept/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'SystemDeptCard',
        component: () => import('@/views/system/dept/card.vue'),
        meta: { title: '部门', activeMenu: '/system/dept', noTag: true }
      }
    ]
  },
  {
    path: '/mdm/material/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MdmMaterialCard',
        component: () => import('@/views/mdm/material/card.vue'),
        meta: { title: '物料', activeMenu: '/mdm/material', noTag: true }
      }
    ]
  },
  {
    path: '/mdm/materialCategory/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MdmMaterialCategoryCard',
        component: () => import('@/views/mdm/materialCategory/card.vue'),
        meta: { title: '物料分类', activeMenu: '/mdm/materialCategory', noTag: true }
      }
    ]
  },
  {
    path: '/mdm/customer/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MdmCustomerCard',
        component: () => import('@/views/mdm/customer/card.vue'),
        meta: { title: '客户', activeMenu: '/mdm/customer', noTag: true }
      }
    ]
  },
  {
    path: '/mdm/supplier/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MdmSupplierCard',
        component: () => import('@/views/mdm/supplier/card.vue'),
        meta: { title: '供应商', activeMenu: '/mdm/supplier', noTag: true }
      }
    ]
  },
  {
    // 仓库菜单挂在「库存管理」下，所以是 /supply/inventory/warehouse 而不是 /mdm/warehouse
    path: '/supply/inventory/warehouse/:id',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'MdmWarehouseCard',
        component: () => import('@/views/mdm/warehouse/card.vue'),
        meta: { title: '仓库', activeMenu: '/supply/inventory/warehouse', noTag: true }
      }
    ]
  },

  // 功能导航页：按模块/目录列出全部功能
  //   /module/supply            → 供应链（库存管理 / 采购管理 两个分区）
  //   /module/supply/inventory  → 库存管理（其下全部功能）
  // key 用 (.*) 以匹配带斜杠的多级路径。
  {
    path: '/module/:key(.*)',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        name: 'ModuleNav',
        component: () => import('@/views/module/index.vue'),
        // noTag：功能导航页是启动器，不是业务页，不占用标签位
        meta: { title: '功能导航', noTag: true }
      }
    ]
  }
]

/**
 * 兜底路由。刻意**不放进 constantRoutes**：
 * 动态路由是在登录后才 addRoute 的，如果兜底路由一开始就在表里且优先级更高，
 * 刷新页面时会先命中 404。放到动态路由之后注册最稳妥。
 */
export const anyRoute = {
  path: '/:pathMatch(.*)*',
  redirect: '/404',
  hidden: true
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
