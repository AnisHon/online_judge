import {useUserStore} from "@/stores/useUserStore.ts";

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