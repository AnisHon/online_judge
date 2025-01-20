import '@/assets/styles/common.css';
import '@/assets/styles/transition.css';
// import '@/assets/styles/element-plus.scss'
// import '@/assets/styles/color.scss'
import 'normalize.css';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/theme-chalk/dark/css-vars.css';

import {createApp} from 'vue';
import {createPinia} from 'pinia';
import {ElCollapseTransition} from 'element-plus';

import piniaPluginPersistedState from 'pinia-plugin-persistedstate'; //引入持久化插件
import App from '@/App.vue';
import router from './router';
import {has, hasAny} from "@/utils/hasAuth";
import {initSSE} from "@/utils/sse";


const app = createApp(App)

const pinia = createPinia();
app.use(pinia)
pinia.use(piniaPluginPersistedState)
app.use(router)

app.component(<string>ElCollapseTransition.name, ElCollapseTransition)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.directive("has", has);
app.directive('hasAny', hasAny)

// router.isReady().then(() => app.mount('#app')
app.mount('#app')

initSSE();
