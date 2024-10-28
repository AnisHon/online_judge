import {
    type PagedResponse,
    type SortedPagedType,
} from "@/api/pagedType";
import {type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, remove, update} from "@/utils/simpleCRUD";

interface ClassView {
    classId: number;
    className: string;
    createTime: Date;
    remark: string;
}

interface ClassForm {
    classId: number;
    className: string;
    remark: string;
}

interface QueryClass extends SortedPagedType{
    classId?: number;
    className?: string;
}

const removeClass = async (id: number | number[]) => {
    await remove(id, "/user-api/class/removeBatch", "/user-api/class/remove");
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
    await update(form, "/user-api/class/update");
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
    }, 1000);
    return {loading, isLoading, get};
}

export type {
    QueryClass,
}

export {
    getClass,
    debouncedGetClass,
    removeClass,
    addClass,
    debouncedAddClass,
    updateClass,
    debouncedUpdateClass,
    dict
}



