import {watch, type DirectiveBinding} from "vue";
import {useUserStore} from "@/stores/useUserStore";

type PermissionValue = string | string[] | undefined;
type PermissionElement = HTMLElement & {
    __permissionStop?: () => void;
    __permissionUpdate?: () => void;
    __permissionValue?: PermissionValue;
};

const normalize = (value: PermissionValue): string[] => {
    if (!value) return [];
    return Array.isArray(value) ? value : [value];
};

const hasAll = (permissions: string[], required: PermissionValue): boolean => {
    const values = normalize(required);
    return values.length === 0 || values.every(permission => permissions.includes(permission));
};

const hasAnyPermission = (permissions: string[], required: PermissionValue): boolean => {
    const values = normalize(required);
    return values.length === 0 || values.some(permission => permissions.includes(permission));
};

const applyVisibility = (element: PermissionElement, allowed: boolean) => {
    if (!(element instanceof HTMLElement)) return;
    element.hidden = !allowed;
    if (allowed) {
        element.removeAttribute('aria-hidden');
    } else {
        element.setAttribute('aria-hidden', 'true');
    }
};

const mountPermission = (
    element: PermissionElement,
    binding: DirectiveBinding<PermissionValue>,
    matcher: (permissions: string[], required: PermissionValue) => boolean,
) => {
    const userStore = useUserStore();
    element.__permissionValue = binding.value;
    const update = () => applyVisibility(element, matcher(userStore.getAuths(), element.__permissionValue));

    // 权限异步加载完成后自动恢复按钮，不能再通过 removeChild 永久删除节点。
    update();
    element.__permissionStop?.();
    element.__permissionStop = watch(
        () => userStore.getAuths().slice().sort().join('\u001f'),
        update,
        {flush: 'sync'},
    );
    element.__permissionUpdate = update;
};

const unmountPermission = (element: PermissionElement) => {
    element.__permissionStop?.();
    delete element.__permissionStop;
    delete element.__permissionUpdate;
    delete element.__permissionValue;
};

const has = {
    mounted(element: PermissionElement, binding: DirectiveBinding<PermissionValue>) {
        mountPermission(element, binding, hasAll);
    },
    updated(element: PermissionElement, binding: DirectiveBinding<PermissionValue>) {
        element.__permissionValue = binding.value;
        element.__permissionUpdate?.();
    },
    unmounted(element: PermissionElement) {
        unmountPermission(element);
    },
};

const hasAny = {
    mounted(element: PermissionElement, binding: DirectiveBinding<PermissionValue>) {
        mountPermission(element, binding, hasAnyPermission);
    },
    updated(element: PermissionElement, binding: DirectiveBinding<PermissionValue>) {
        element.__permissionValue = binding.value;
        element.__permissionUpdate?.();
    },
    unmounted(element: PermissionElement) {
        unmountPermission(element);
    },
};

export {has, hasAny};
