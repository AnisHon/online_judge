import type {PagedResponse, SortedPagedType,} from "@/api/pagedType";
import {type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, postedRemove, remove, update} from "@/utils/simpleCRUD";

enum RoleStatus {
    NORMAL,
    SUSPEND
}

interface UserRoleRelation{
    userId: number;
    roleId: number;
}

interface RoleForm {
    roleId?: number;
    roleName?: string;
    status?: RoleStatus;
    remark?: string;
}

interface RoleView {
    roleId: number;
    roleName: string;
    status: RoleStatus;
    createTime: Date;
    remark: string;
}

interface QueryRole extends SortedPagedType{
    roleId?: number;
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

const removeRole = async (id: number | number[]) => {
    await remove(id, "/user-api/role/removeBatch", "/user-api/role/remove");
}




const addRole = async (form: RoleForm) => {
    await add(form, "/user-api/role/add");
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
    await update(form, "/user-api/role/update");
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
    }, 1000);
    return {loading, isLoading, get};
}

const revoke = async (form: UserRoleRelation | UserRoleRelation[]) => {
    await postedRemove(form, "/user-api/role/batchRevoke", "/user-api/role/revoke");
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
    revoke,
    dict
}



