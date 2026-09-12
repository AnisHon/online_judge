import type {PagedResponse, PagedType, SortedPagedType,} from "@/api/pagedType";
import {get, getWithParams, put, type successCallback} from "@/utils/http";
import __, {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, remove, update} from "@/utils/simpleCRUD";
import {useUserStore} from "@/stores/useUserStore";
import {ElNotification} from "element-plus";
import type {IdType} from "@/api/common.ts";

enum UserStatus {
    NORMAL,
    BANNED
}



interface UserAddForm {
    userName: string;
    nikeName: string;
    email: string;
    password: string;
    status: UserStatus;
    role: IdType;
    remark: string;
}

interface UserUpdateForm {
    userId?: IdType;
    nikeName?: string;
    email?: string;
    status?: UserStatus;
    remark?: string;
}

interface UserView {
    userId: IdType;
    userName: string;
    email: string;
    points: number;
    nikeName: string;
    status: UserStatus;
    createTime: Date;
    remark: string;
}

interface UserForm {
    userId?: IdType;
    userName?: string;
    nikeName?: string;
}

interface QueryUser extends SortedPagedType{
    userId?: IdType;
    userName?: string;
    nikeName?: string;
    email?: string;
    status?: UserStatus;
}

interface QueryRoleUser extends PagedType{
    roleId?: IdType,
    userId?: IdType,
    username?: string
    email?: string,
    nikeName?: string,
}

const dict = {
    userStatus: [
        {
            value: UserStatus.NORMAL,
            label: "正常"
        },
        {
            value: UserStatus.BANNED,
            label: "封禁"
        }
    ],
};

const rank = async (limit: number) => {
    const {data} = await get<UserView[]>("/user-api/user/rank", limit);
    return data;
}



const removeUser = async (id: IdType | IdType[]) => {
    await remove(id,  "/user-api/user");
}

export const getMyPoint = async (): Promise<string> => {
    const {data} = await get<string>("/user-api/user/point");
    return data;
}

export const banUser = async (id: IdType | IdType[]) => {
    const {data} = await put("/user-api/auth/ban/" + id, undefined);
    if (data) {
        ElNotification.success("封禁成功")
    } else {
        ElNotification.warning("封禁失败")
    }
}

export const unbanUser = async (id: IdType) => {
    const {data} = await put("/user-api/auth/unban/" + id, undefined);
    if (data) {
        ElNotification.success("解封成功")
    } else {
        ElNotification.warning("解封失败")
    }
}

export const resetToDefault = async (id: IdType) => {
    const {data} = await put("/user-api/auth/resetToDefault/" + id, undefined);
    if (data) {
        ElNotification.success("重置成功")
    } else {
        ElNotification.warning("重置失败")
    }
}



const addUser = async (form: UserAddForm) => {
    await add(form, "/user-api/user");
}

const debouncedAddUser = (form: UserAddForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addUser(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateUser = async (form: UserUpdateForm) => {
    await update(form, "/user-api/user");
}

const debouncedUpdateUser = (form: UserUpdateForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateUser(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getUser = async (queryData: QueryUser): Promise<PagedResponse<UserView>> => {
    // 后端 UserState 按枚举名称绑定查询参数，不能把数字枚举值直接传成 "0/1"。
    const statusValue = String(queryData.status ?? '').toUpperCase();
    const status = queryData.status === undefined
        ? undefined
        : statusValue === String(UserStatus.BANNED) || statusValue === 'BANNED' ? 'BANNED' : 'NORMAL';
    return await fetch({...queryData, status} as unknown as QueryUser, "/user-api/user/page", "/user-api/user/query")
}





const debouncedGetUser = (queryData: QueryUser, success: successCallback<PagedResponse<UserView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getUser(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


const getRoleUser = async (queryData: QueryRoleUser): Promise<PagedResponse<UserView>> => {
    const {data} = await getWithParams<PagedResponse<UserView>, QueryRoleUser>("/user-api/user/getByRole", queryData);
    return data;
}


const debouncedGetRoleUser = (queryData: QueryRoleUser, success: successCallback<PagedResponse<UserView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getRoleUser(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


const changeSelf = async (form: UserForm) => {
    await update(form, "/user-api/user/change-myself");
    const userStore = useUserStore();
    await userStore.loadUser()
}

const change = __.debounce(changeSelf, 1000);

export type {
    QueryUser,
    UserAddForm,
    UserUpdateForm,
    QueryRoleUser,
    UserView,
    UserForm
}

export {
    getUser,
    debouncedGetUser,
    removeUser,
    addUser,
    debouncedAddUser,
    updateUser,
    debouncedUpdateUser,
    getRoleUser,
    debouncedGetRoleUser,
    UserStatus,
    rank,
    change,
    dict
}
