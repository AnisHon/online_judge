import {defineStore} from 'pinia'

import {ref} from "vue";

export const useToken = defineStore('token', {
    state: () => {
        return {
            token: null
        }
    },
    persist: true
});


