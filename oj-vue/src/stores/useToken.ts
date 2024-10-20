import {defineStore} from 'pinia'
import {useUser} from '@/stores/useUserStore'




export const useToken = defineStore('token', {
    state: () => {
        return {
            token: ""
        }
    },
    actions: {
        setToken(token_: string) {
            const user = useUser()
            this.token = token_

            user.loadUser()
        },

        clearToken(){
            this.token = ''
        }
    },

    persist: true
});


