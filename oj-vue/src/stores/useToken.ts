import {defineStore} from 'pinia';
import {useUserStore} from '@/stores/useUserStore';
export const useToken = defineStore('token', {
    state: () => {
        return {
            token: "",
            refreshToken: "",
            // 只存在当前页面内，不持久化。HTTP 层用它隔离旧请求和新会话。
            sessionVersion: 0
        }
    },
    actions: {
        async setToken(token_: string) {
            const user = useUserStore();
            this.startSession(token_, '');

            await user.loadUser();
        },

        setTokens(accessToken: string, refreshToken = '') {
            // 每次登录/刷新都原子替换双 key，不能残留上一账号的 refresh token。
            this.token = accessToken || '';
            this.refreshToken = refreshToken || '';
        },

        startSession(accessToken: string, refreshToken = '') {
            this.sessionVersion++;
            this.setTokens(accessToken, refreshToken);
        },

        getSessionVersion() {
            return this.sessionVersion;
        },

        hasToken() {
            return this.token.trim().length > 0;
        },

        clearToken(){
            this.sessionVersion++;
            this.setTokens('', '');
            useUserStore().clear()
        }
    },

    // 认证凭据不跨浏览器重启持久化，避免 refresh token 长期残留在 localStorage。
    persist: {
        storage: sessionStorage,
        pick: ['token', 'refreshToken']
    }
});
