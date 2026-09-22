import {defineStore} from 'pinia';
import {useUserStore} from '@/stores/useUserStore';

export type AuthStatus = 'unknown' | 'initializing' | 'authenticated' | 'unauthenticated';

export const useToken = defineStore('token', {
    state: () => {
        return {
            token: "",
            // Access Token 只保存在当前 Tab 的 Pinia 运行时内存中。
            sessionVersion: 0,
            authStatus: 'unknown' as AuthStatus
        }
    },
    actions: {
        async setToken(token_: string) {
            const user = useUserStore();
            this.startSession(token_);

            await user.loadUser();
        },

        setAccessToken(accessToken: string) {
            this.token = accessToken || '';
            this.authStatus = this.token ? 'authenticated' : 'unauthenticated';
        },

        startSession(accessToken: string) {
            this.sessionVersion++;
            this.setAccessToken(accessToken);
        },

        markInitializing() {
            if (this.authStatus === 'unknown') this.authStatus = 'initializing';
        },

        markUnauthenticated() {
            this.sessionVersion++;
            this.token = '';
            this.authStatus = 'unauthenticated';
        },

        getSessionVersion() {
            return this.sessionVersion;
        },

        hasToken() {
            return this.token.trim().length > 0;
        },

        clearToken(){
            this.markUnauthenticated();
            useUserStore().clear();
        }
    }
});
