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
    let loadPromise: Promise<LoginUser> | null = null;
    let sessionVersion = 0;

    const loadUser = async (force = false): Promise<LoginUser> => {
        if (!force && user.value) {
            return user.value;
        }
        if (loadPromise) {
            return loadPromise;
        }

        const version = sessionVersion;
        const request = getMe().then((data) => {
            if (version === sessionVersion) {
                user.value = data;
            }
            return data;
        });
        loadPromise = request;
        request.then(
            () => { if (loadPromise === request) loadPromise = null; },
            () => { if (loadPromise === request) loadPromise = null; },
        );
        return request;
    }

    const clear = () => {
        sessionVersion++;
        loadPromise = null;
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
        return user.value?.auths || []
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
