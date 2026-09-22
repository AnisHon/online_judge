import {useUserStore} from "@/stores/useUserStore.ts";
import type {IdType} from "@/api/common.ts";

export const BACKEND_ACCESS_PERMISSION = 'system:backend:access';

export const hasBackendAccess = (auths: readonly string[] = useUserStore().getAuths()): boolean =>
    auths.includes(BACKEND_ACCESS_PERMISSION);

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

export const hasAnyPerm = (perm: string | string[] | undefined): boolean => {
    if (!perm) {
        return true;
    }
    const auths = useUserStore().getAuths();
    return Array.isArray(perm) ? perm.some(role => auths.includes(role)) : auths.includes(perm);
}


export const isUserIdEqual = (id: IdType | undefined): boolean => {
    const userStore = useUserStore();

    if (id === undefined || id === null || userStore.user?.userId === undefined || userStore.user?.userId === null) {
        return false;
    }

    return String(userStore.user.userId) === String(id);
}
