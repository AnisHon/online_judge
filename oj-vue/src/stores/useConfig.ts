import {defineStore} from "pinia";
import {reactive, readonly} from "vue";
// 编程语言 不是 国际化

export interface UserConfig {
    previewTheme: "default" | "github" | "vuepress" | "mk-cute" | "smart-blue" | "mk-cyanosis";
    codeTheme: "atom" | "a11y" | "github" | "gradient" | "kimbie" | "paraiso" | "qtcreator" | "stackoverflow";
}

export default defineStore("config", {
    state() {
        return {
            config: {
                previewTheme: "default",
                codeTheme: "atom"
            }

        }
    },
    getters: {
        readonly(): UserConfig {
            return readonly<UserConfig>(<UserConfig>this.config);
        },
        get(): UserConfig {
            return <UserConfig>this.config;
        }

    },
    actions: {
        setPreviewTheme(theme: string) {
            this.config.previewTheme = theme;
        },
        setCodeTheme(theme: string) {
            this.config.codeTheme = theme;
        }
    },
    persist: true
})
