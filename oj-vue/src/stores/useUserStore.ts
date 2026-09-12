import {defineStore} from "pinia";
import {ref} from "vue";
import {getMe} from "@/api/auth/authentication";
import type {IdType} from "@/api/common.ts";


export interface LoginUser {
    userId: IdType;
    userName: string;
    nikeName: string;
    email: string;
    createTime: string;
    auths: string[];
}


export const useUserStore = defineStore('user', () => {
    const user =  ref<LoginUser | null>(null)
    const avatarVersions = ref<Record<string, number>>({})

    const loadUser = async () => {
        user.value = await getMe()
    }

    const clear = () => {
        user.value = null
    }

    const refreshAvatar = (userId?: IdType) => {
        const id = String(userId ?? user.value?.userId ?? '')
        if (!id) return
        avatarVersions.value[id] = (avatarVersions.value[id] ?? 0) + 1
    }

    const getUser = async () => {
        if (user.value === null) {
            await loadUser();
        }
        return <LoginUser>user.value;
    }

    const getAuths = (): string[] => {
        if (!user.value) {
            return []
        }
        return (<LoginUser>user.value).auths
    }

    return {
        user,
        loadUser,
        clear,
        avatarVersions,
        refreshAvatar,
        getAuths,
        getUser,
    }
});
