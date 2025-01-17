import type {PagedResponse, SortedPagedType,} from "@/api/pagedType";
import {del, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, putRemove, remove, update} from "@/utils/simpleCRUD";
import {ElNotification} from "element-plus";
import type {IdType} from "@/api/common.ts";

enum RoleStatus {
    NORMAL,
    SUSPEND
}

interface UserRoleRelation{
    userId: IdType;
    roleId: IdType;
}

interface RoleForm {
    roleId?: IdType;
    roleName?: string;
    status?: RoleStatus;
    remark?: string;
}

interface RoleView {
    roleId: IdType;
    roleName: string;
    status: RoleStatus;
    createTime: Date;
    remark: string;
}

interface QueryRole extends SortedPagedType{
    roleId?: IdType;
    roleName?: string;
    status?: RoleStatus;
    remark?: string;
}

const dict = {
    roleStatus: [
        {
            value: 0,
            label: "正常"
        },
        {
            value: 1,
            label: "停用"
        }
    ],
};

const removeRole = async (id: IdType | IdType[]) => {
    await remove(id, "/user-api/role");
}


const refreshRoleCache = async () => {
    await del("/user-api/role/refresh")
        .then(() => ElNotification.success("刷新成功"))
        .catch(() => ElNotification.warning("刷新失败"));
}

const addRole = async (form: RoleForm) => {
    await add(form, "/user-api/role");
}

const debouncedAddRole = (form: RoleForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addRole(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateRole = async (form: RoleForm) => {
    await update(form, "/user-api/role");
}

const debouncedUpdateRole = (form: RoleForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateRole(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getRole = async (queryData: QueryRole): Promise<PagedResponse<RoleView>> => {
    return await fetch(queryData, "/user-api/role/page", "/user-api/role/query")
}

const debouncedGetRole = (queryData: QueryRole, success: successCallback<PagedResponse<RoleView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getRole(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

const revoke = async (form: UserRoleRelation | UserRoleRelation[]) => {
    await putRemove(form, "/user-api/role/batchRevoke", "/user-api/role/revoke");
}

const grant = async (form: UserRoleRelation[]) => {
    await add(form, "/user-api/role/batchGrant");
}

const debouncedGrant = (form: UserRoleRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        grant(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}


export type {
    QueryRole,
    RoleForm,
    RoleView,
    UserRoleRelation
}

export {
    getRole,
    debouncedGetRole,
    removeRole,
    addRole,
    debouncedAddRole,
    updateRole,
    debouncedUpdateRole,
    debouncedGrant,
    RoleStatus,
    refreshRoleCache,
    revoke,
    dict
}



