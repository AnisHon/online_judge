import '@/assets/styles/common.css';
import '@/assets/styles/auth-form.css';
import '@/assets/styles/transition.css';
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
import {useSiteConfig} from '@/stores/useSiteConfig';


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

const bootstrap = async () => {
    // 配置在应用挂载前加载一次，登录/注册页直接读取内存状态。
    // load() 内部保留默认值，即使配置服务暂时不可用也不会阻塞页面启动。
    await useSiteConfig(pinia).load()
    await router.isReady()
    app.mount('#app')
}

void bootstrap()
