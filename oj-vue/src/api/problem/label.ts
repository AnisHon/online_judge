import {get, type successCallback} from "@/utils/http"
import {batchAdd, putRemove} from "@/utils/simpleCRUD";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";

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