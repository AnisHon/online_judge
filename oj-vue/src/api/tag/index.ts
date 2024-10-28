import {get, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, remove, update} from "@/utils/simpleCRUD";

interface TagView {
    tagId: number;
    tagName: string;
    tagColor: string;
    createTime: Date;
}

interface TagForm {
    tagId?: number;
    tagName?: string;
    tagColor?: string;
}


const removeTag = async (id: number | number[]) => {
    await remove(id, "/problem-api/tag/deleteBatch", "/problem-api/tag/delete");
}

const addTag = async (form: TagForm) => {
    await add(form, "/problem-api/tag/add");
}

const debouncedAddTag = (form: TagForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addTag(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateTag = async (form: TagForm) => {
    await update(form, "/problem-api/tag/update");
}

const debouncedUpdateTag = (form: TagForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateTag(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getTag = async (): Promise<TagView[]> => {
    const {data} =  await get<TagView[]>("/problem-api/tag/getAll");
    return data;
}

const debouncedGetTag = (success: successCallback<TagView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getTag()
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

export type {
    TagForm,
    TagView
}

export {
    getTag,
    debouncedGetTag,
    removeTag,
    addTag,
    debouncedAddTag,
    updateTag,
    debouncedUpdateTag,
}



