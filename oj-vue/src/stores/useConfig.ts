import {defineStore} from "pinia";
import {reactive, readonly} from "vue";
// 编程语言 不是 国际化

export interface UserConfig {
    previewTheme: string;
}

export default defineStore("config", {
    state() {
        return {
            config: {
                previewTheme: "default",
            }

        }
    },
    getters: {
        get(): UserConfig {
            return readonly(this.config);
        }
    },
    actions: {
        setPreviewTheme(theme: string) {
            this.config.previewTheme = theme;
        }
    },
    persist: true
})
