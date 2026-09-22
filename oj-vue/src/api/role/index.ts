import type {PagedResponse, SortedPagedType,} from "@/api/pagedType";
import {del, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, putRemove, remove, update} from "@/utils/simpleCRUD";
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
    await del("/user-api/role/refresh");
}

const addRole = async (form: RoleForm) => {
    await add(form, "/user-api/role");
}

const debouncedAddRole = (form: RoleForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const payload = {...form}
    const add = debounce(() => {
        addRole(payload)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add, cancel: () => add.cancel()};
}



const updateRole = async (form: RoleForm) => {
    await update(form, "/user-api/role");
}

const debouncedUpdateRole = (form: RoleForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const payload = {...form}
    const update = debounce(() => {
        updateRole(payload)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update, cancel: () => update.cancel()};
}

const getRole = async (queryData: QueryRole): Promise<PagedResponse<RoleView>> => {
    return await fetch(queryData, "/user-api/role/page", "/user-api/role/query")
}

const debouncedGetRole = (queryData: QueryRole, success: successCallback<PagedResponse<RoleView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const query = {...queryData}
    const get = debounce(() => {
        getRole(query)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get, cancel: () => get.cancel()};
}

const revoke = async (form: UserRoleRelation | UserRoleRelation[]) => {
    await putRemove(form, "/user-api/role/batchRevoke", "/user-api/role/revoke");
}

const grant = async (form: UserRoleRelation[]) => {
    await add(form, "/user-api/role/batchGrant");
}

const debouncedGrant = (form: UserRoleRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const payload = form.map(relation => ({...relation}))
    const add = debounce(() => {
        grant(payload)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add, cancel: () => add.cancel()};
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
    grant,
    RoleStatus,
    refreshRoleCache,
    revoke,
    dict
}

