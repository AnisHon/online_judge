import type {PagedResponse, PagedType, SortedPagedType,} from "@/api/pagedType";
import {get, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, remove, simpleGet, update} from "@/utils/simpleCRUD";
import type {Ref} from "vue";

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
    role: number;
    remark: string;
}

interface UserUpdateForm {
    userId?: number;
    nikeName?: string;
    email?: string;
    status?: UserStatus;
    remark?: string;
}

interface UserView {
    userId: number;
    userName: string;
    email: string;
    point: number;
    nikeName: string;
    status: UserStatus;
    createTime: Date;
    remark: string;
}

interface QueryUser extends SortedPagedType{
    userId?: number;
    userName?: string;
    nikeName?: string;
    email?: string;
    status?: UserStatus;
}

interface QueryRoleUser extends PagedType{
    roleId?: number,
    userId?: number,
    username?: string
    email?: string,
    nikeName?: string,
}

const dict = {
    userStatus: [
        {
            value: 0,
            label: "正常"
        },
        {
            value: 1,
            label: "封禁"
        }
    ],
};

const rank = async (limit: number) => {
    const {data} = await get<UserView[]>("/user-api/user/rank", limit);
    return data;
}

const removeUser = async (id: number | number[]) => {
    await remove(id, "/user-api/user/removeBatch", "/user-api/user/remove");
}

const resetToDefault = async (id: number) => {
    await simpleGet(id, "/user-api/auth/reset-to-default", "重制成功", "重制失败");
}

const debouncedReset = (id: Ref<number>, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        resetToDefault(id.value)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const addUser = async (form: UserAddForm) => {
    await add(form, "/user-api/user/add");
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
    await update(form, "/user-api/user/update");
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
    return await fetch(queryData, "/user-api/user/page", "/user-api/user/query")
}





const debouncedGetUser = (queryData: QueryUser, success: successCallback<PagedResponse<UserView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getUser(queryData)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}


const getRoleUser = async (queryData: QueryRoleUser): Promise<PagedResponse<UserView>> => {
    const {data} = await post<QueryRoleUser, PagedResponse<UserView>>("/user-api/user/getByRole", queryData);
    return data;
}


const debouncedGetRoleUser = (queryData: QueryRoleUser, success: successCallback<PagedResponse<UserView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getRoleUser(queryData)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}
export type {
    QueryUser,
    UserAddForm,
    UserUpdateForm,
    QueryRoleUser,
    UserView
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
    debouncedReset,
    UserStatus,
    rank,
    dict
}



