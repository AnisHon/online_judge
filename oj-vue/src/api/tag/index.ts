import {get, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, remove, update} from "@/utils/simpleCRUD";
import type {IdType} from "@/api/common.ts";

interface TagView {
    tagId: IdType;
    tagName: string;
    tagColor: string;
    createTime?: string | Date;
}

interface TagForm {
    tagId?: IdType;
    tagName?: string;
    tagColor?: string;
}


const removeTag = async (id: IdType | IdType[]) => {
    await remove(id, "/problem-api/tag");
}

const addTag = async (form: TagForm) => {
    await add(form, "/problem-api/tag");
}

const debouncedAddTag = (form: TagForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = () => {
        addTag(form)
            .then(success)
            .finally(finish);
    }
    return {loading, isLoading, add};
}



const updateTag = async (form: TagForm) => {
    await update(form, "/problem-api/tag");
}

const debouncedUpdateTag = (form: TagForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = () => {
        updateTag(form)
            .then(success)
            .finally(finish);
    }
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
    }, 500);
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

