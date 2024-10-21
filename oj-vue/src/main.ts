import '@/assets/common.css'
import 'normalize.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedState from 'pinia-plugin-persistedstate' //引入持久化插件

import App from '@/App.vue'
import router from './router'

const app = createApp(App)

const pinia = createPinia();
app.use(pinia)
pinia.use(piniaPluginPersistedState)

app.use(router)



app.mount('#app')
