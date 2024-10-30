import {
    type PagedResponse,
    type SortedPagedType,
} from "@/api/pagedType";
import {type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, fetch, remove, update} from "@/utils/simpleCRUD";

interface ListView {
    listId: number;
    listName: string;
    description: string;
}

interface ListForm {
    listId?: number;
    listName?: string;
    remark?: string;
}

interface QueryList extends SortedPagedType{
    listId?: number;
    listName?: string;
}

const removeList = async (id: number | number[]) => {
    await remove(id, "/user-api/list/del", "/user-api/list/del");
}

const addList = async (form: ListForm) => {
    await add(form, "/user-api/list/add");
}

const debouncedAddList = (form: ListForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addList(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateList = async (form: ListForm) => {
    await update(form, "/user-api/list/update");
}

const debouncedUpdateList = (form: ListForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateList(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getList = async (queryData: QueryList): Promise<PagedResponse<ListView>> => {
    return await fetch(queryData, "/user-api/list/page", "/user-api/list/query");
}

const debouncedGetList = (queryData: QueryList, success: successCallback<PagedResponse<ListView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getList(queryData)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

export type {
    QueryList,
    ListForm,
    ListView
}

export {
    getList,
    debouncedGetList,
    removeList,
    addList,
    debouncedAddList,
    updateList,
    debouncedUpdateList,
}



