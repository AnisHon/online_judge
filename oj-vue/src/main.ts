import '@/assets/common.css'
import '@/assets/variables.scss'
import 'normalize.css'
import 'element-plus/dist/index.css'

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElCollapseTransition } from 'element-plus'

import piniaPluginPersistedState from 'pinia-plugin-persistedstate' //引入持久化插件

import App from '@/App.vue'
import router from './router'
import mitt from "mitt";

const app = createApp(App)

const pinia = createPinia();
app.use(router)
app.use(pinia)
pinia.use(piniaPluginPersistedState)
app.component(<string>ElCollapseTransition.name, ElCollapseTransition)




app.mount('#app')
