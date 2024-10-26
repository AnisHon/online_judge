import {defineStore} from 'pinia';
import {useUserStore} from '@/stores/useUserStore';
import __ from 'lodash';



export const useToken = defineStore('token', {
    state: () => {
        return {
            token: ""
        }
    },
    actions: {
        setToken(token_: string) {
            const user = useUserStore();
            this.token = token_;

            user.loadUser();
        },

        hasToken() {
            return !__.isEmpty(this.token);
        },

        clearToken(){
            this.token = ''
        }
    },

    persist: true
});


