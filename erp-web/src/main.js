import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import pinia from './store'
import directives from './directive'

import Pagination from '@/components/Pagination/index.vue'
import PageHeader from '@/components/PageHeader/index.vue'
import DocToolbar from '@/components/DocToolbar/index.vue'
import DictSelect from '@/components/DictSelect/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import RightToolbar from '@/components/RightToolbar/index.vue'

// 主题覆盖必须在 element-plus 的样式之后 import：同为 :root 变量时后加载者生效，
// 顺序反了会被 Element 的默认值盖回去，且不会有任何报错。
import '@/assets/styles/element-override.scss'
import '@/assets/styles/index.scss'

// 全局守卫必须在这里 import：它依赖 pinia 已被 use，且要在 mount 之前注册
import './permission'

const app = createApp(App)

// 全量注册 Element Plus 图标：后端菜单表的 icon 字段存的就是这些组件的名字
// （如 Setting / Goods / Box / ShoppingCart），侧边栏用 <component :is="name"/> 动态渲染。
// 这是唯一需要全量注册图标的地方，代价是包体略大，换来的是菜单加图标不用改前端代码。
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 公共组件全局注册，页面里直接用 <DictTag> / <DictSelect> / <Pagination>，
// 免去每个页面重复 import（这也是「字典下拉不要手写 options」这条规范能落地的前提）
app.component('Pagination', Pagination)
app.component('DictSelect', DictSelect)
app.component('DictTag', DictTag)
app.component('RightToolbar', RightToolbar)
app.component('PageHeader', PageHeader)
app.component('DocToolbar', DocToolbar)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(directives)

app.mount('#app')
