import {useUserStore} from "@/stores/useUserStore.ts";
import type {IdType} from "@/api/common.ts";

export const hasPerm = (perm: string | string[] | undefined): boolean => {
    if (!perm) {
        return true;
    }
    const userStore = useUserStore();
    const auths = userStore.getAuths();

    let result: boolean = true;
    if (perm instanceof Array) {
        perm.forEach(role => {
            result &&= auths.includes(role);
        })
    } else {
        result = auths.includes(perm);
    }

    return result;
}


export const isUserIdEqual = (id: IdType | undefined): boolean => {
    const userStore = useUserStore();

    if (id === null || !userStore.user?.userId) {
        return false;
    }

    return userStore.user.userId === id;
}