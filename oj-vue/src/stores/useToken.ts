import {defineStore} from 'pinia';
import {useUserStore} from '@/stores/useUserStore';
import __ from 'lodash';



export const useToken = defineStore('token', {
    state: () => {
        return {
            token: "",
            refreshToken: ""
        }
    },
    actions: {
        async setToken(token_: string) {
            const user = useUserStore();
            this.setTokens(token_, '');

            await user.loadUser();
        },

        setTokens(accessToken: string, refreshToken = '') {
            this.token = accessToken;
            // 每次登录/刷新都原子替换双 key，不能残留上一账号的 refresh token。
            this.refreshToken = refreshToken;
        },

        hasToken() {
            return !__.isEmpty(this.token);
        },

        clearToken(){
            this.token = ''
            this.refreshToken = ''
            useUserStore().clear()
        }
    },

    // 认证凭据不跨浏览器重启持久化，避免 refresh token 长期残留在 localStorage。
    persist: {
        storage: sessionStorage,
        pick: ['token', 'refreshToken']
    }
});
