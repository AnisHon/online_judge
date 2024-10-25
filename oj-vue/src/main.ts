import '@/assets/common.css'
import 'normalize.css'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElCollapseTransition } from 'element-plus'

import piniaPluginPersistedState from 'pinia-plugin-persistedstate' //引入持久化插件

import App from '@/App.vue'
import router from './router'
const app = createApp(App)

const pinia = createPinia();
app.use(pinia)
pinia.use(piniaPluginPersistedState)
app.use(router)

app.component(<string>ElCollapseTransition.name, ElCollapseTransition)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}


app.mount('#app')
