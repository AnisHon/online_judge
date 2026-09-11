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
            this.token = token_;

            await user.loadUser();
        },

        setTokens(accessToken: string, refreshToken?: string) {
            this.token = accessToken;
            if (refreshToken) this.refreshToken = refreshToken;
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

    persist: true
});
