import {defineStore} from "pinia";
import {ref} from "vue";




export interface LoginUser {
    id: string;
    username: string;
    nikeName: string;
    email: string;

}


export const useToken = defineStore('user', () => {
   const user =  ref()
});
