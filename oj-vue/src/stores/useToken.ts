import {defineStore} from 'pinia';
import {useUser} from '@/stores/useUserStore';
import __ from 'lodash';



export const useToken = defineStore('token', {
    state: () => {
        return {
            token: ""
        }
    },
    actions: {
        setToken(token_: string) {
            const user = useUser();
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


