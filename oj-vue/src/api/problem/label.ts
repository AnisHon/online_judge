import {get, type successCallback} from "@/utils/http"
import {batchAdd, putRemove} from "@/utils/simpleCRUD";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";

export interface TagView {

    tagId: IdType;
    tagName: string;
    tagColor: string;
    createTime: Date;
}

export interface ProblemTagRelation {
    tagId: IdType;
    problemId: IdType;
}

const fetchTagByProblemId = async (problemId: IdType) => {
    const {data} = await get<TagView[], IdType>("/problem-api/tag/problem", problemId);
    return data;
}


const delTagForProblem = async (param: ProblemTagRelation | ProblemTagRelation[]) => {
    await putRemove(
        param,
        "/problem-api/problem/batchDelTag",
        '/problem-api/problem/delTag'
    );
}

const addTagForProblem = async (param: ProblemTagRelation | ProblemTagRelation[]) => {
    await batchAdd(param, "/problem-api/problem/batchAddTag", "/problem-api/problem/addTag")
}

const debouncedAddTagProblem = (param: ProblemTagRelation | ProblemTagRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addTagForProblem(param)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add, finish};
}

async function getAllTags(): Promise<TagView[]> {
    const {data} = await get<TagView[]>("/problem-api/tag/getAll")
    return Array.isArray(data) ? data : [];
}

export {
    getAllTags,
    delTagForProblem,
    debouncedAddTagProblem,
    fetchTagByProblemId
}
