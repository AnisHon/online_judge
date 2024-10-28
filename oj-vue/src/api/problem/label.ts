import {get, post, type successCallback} from "@/utils/http"
import {add, batchAdd, fetch, postedRemove, remove} from "@/utils/simpleCRUD";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";
import type {ProblemForm} from "@/api/problem/index";

export interface TagView {

    tagId: number;
    tagName: string;
    tagColor: string;
    createTime: Date;
}

export interface ProblemTagRelation {
    tagId: number;
    problemId: number;
}

const fetchTagByProblemId = async (problemId: number) => {
    const {data} = await get<TagView[], number>("/problem-api/tag/problem", problemId);
    return data;
}


const delTagForProblem = async (param: ProblemTagRelation | ProblemTagRelation[]) => {
    await postedRemove(
        param,
        "/problem-api/problem/batch-del-tag",
        '/problem-api/problem/del-tag'
    );
}

const addTagForProblem = async (param: ProblemTagRelation | ProblemTagRelation[]) => {
    await batchAdd(param, "/problem-api/problem/batch-add-tag", "/problem-api/problem/add-tag")
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
    const { code, data, message} = await get<TagView[]>("/problem-api/tag/getAll")
    if (code !== 200) {
        ElMessage.warning(message)
    }
    return data;
}

export {
    getAllTags,
    delTagForProblem,
    debouncedAddTagProblem,
    fetchTagByProblemId
}