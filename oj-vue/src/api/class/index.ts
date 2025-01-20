import {
    type PagedResponse,
    type SortedPagedType,
} from "@/api/pagedType";
import {addResultNotify, get, removeResultNotify, service, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, remove, update} from "@/utils/simpleCRUD";
import type {IdType} from "@/api/common.ts";
import type {UserView} from "@/api/user";

interface ClassView {
    classId: IdType;
    className: string;
    createTime: Date;
    remark: string;
}

interface ClassForm {
    classId?: IdType;
    className?: string;
    remark?: string;
}

interface QueryClass extends SortedPagedType{
    classId?: IdType;
    className?: string;
}

const removeClass = async (id: IdType | IdType[]) => {
    await remove(id, "/user-api/class");
}

const addClass = async (form: ClassForm) => {
    await add(form, "/user-api/class/add");
}

const debouncedAddClass = (form: ClassForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addClass(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateClass = async (form: ClassForm) => {
    await update(form, "/user-api/class");
}

const debouncedUpdateClass = (form: ClassForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateClass(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getClass = async (queryData: QueryClass): Promise<PagedResponse<ClassView>> => {
    return await fetch(queryData, "/user-api/class/page", "/user-api/class/query");
}

const debouncedGetClass = (queryData: QueryClass, success: successCallback<PagedResponse<ClassView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getClass(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

export const getUserByClass = async (classId: IdType) => {
    const {data} = await get<UserView[]>("/user-api/class/user", classId);
    return data;
}

export const addUserForClass = async (classId: IdType, userIds: IdType | IdType[]) => {
    const {data} = await service.post<boolean>(`/user-api/class/user/${classId}/${userIds}`);
    addResultNotify(data);
}

export const removeUserForClass = async (classId: IdType, userIds: IdType | IdType[]) => {
    const {data} = await service.delete<boolean>(`/user-api/class/user/${classId}/${userIds}`);
    removeResultNotify(data);
}



export type {
    QueryClass,
    ClassForm,
    ClassView
}

export {
    getClass,
    debouncedGetClass,
    removeClass,
    addClass,
    debouncedAddClass,
    updateClass,
    debouncedUpdateClass,
}



