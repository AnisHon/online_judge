import {defineStore} from "pinia";
import {ref} from "vue";
import {getMe} from "@/api/auth/authentication";


export interface LoginUser {
    id: string;
    username: string;
    nikeName: string;
    email: string;
    createTime: string;
    auths: string[];
}


export const useUserStore = defineStore('user', () => {
    const user =  ref<LoginUser | null>(null)

    const loadUser = async () => {
        user.value = await getMe()
    }

    const getUser = async () => {
        if (user.value === null) {
            await loadUser();
        }
        return <LoginUser>user.value;
    }

    const getAuths = (): string[] => {
        if (!user) {
            return []
        }
        return (<LoginUser>user.value).auths
    }

    return {
        user,
        loadUser,
        getAuths,
        getUser,
    }
});
