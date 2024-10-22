import {get, post} from "@/utils/http"
import {type TagView} from "./label"
import {ElMessage} from "element-plus";
// 1 OJ, 2 FILL, 3 CHOICE
export enum ProblemType {
    OJ = 1,
    FILL,
    CHOICE,
}

export interface ProblemView {
    auth: number,
    createTime: Date,
    description: string,
    hint: string,
    problemId: 0,
    source: string,
    title: string,
    type: ProblemType
}

export interface TaggedProblemView {
    auth: number;
    createTime: Date;
    description: string;
    hint: string;
    problemId: 0;
    source: string;
    title: string;
    type: ProblemType;
    tags: TagView[];
}

export interface ProblemParam {
    currentPage: number;
    pageSize: number;
    problemId?: string | null;
    tagIds?: number[] | null;
}

export interface ChoiceProblemView {
    content: string,
    order: string
}

export interface OjProblemView {
    problemId: number,
    difficulty: number,
    memoryLimit: number,
    stackLimit: number,
    timeLimit: number,

    input: string,
    output: string,

    inputExample: string,
    outputExample: string,

    createTime: Date,
}

export interface ProblemDetailView {
    choices?: ChoiceProblemView[];
    ojProblemVo?: OjProblemView;
    problemVo: ProblemView;
    tagVo: TagView[];
}

export interface PagedData {
    data: TaggedProblemView[];
    currentPage: number;
    pageSize: number;
    totalRecords: number;
}

async function getProblems(problemParam: ProblemParam): Promise<PagedData> {
    const param: ProblemParam = {currentPage: 0, pageSize: 0}
    Object.assign(param, problemParam);

    if (param.problemId === "") {
        param.problemId = null;
    } else if (
        problemParam.problemId != null &&
        isNaN(parseFloat(problemParam.problemId))
    ){
        param.problemId = null;
    }
    try {
        const {data: { data, currentPage, pageSize, totalRecords}} = await post("/problem-api/problem/tagged-list", problemParam);
        return {data, currentPage, pageSize, totalRecords};
    } catch (msg) {

        if (typeof msg === "string") {
            ElMessage.warning(msg);
        } else if (typeof msg === "object") {
            ElMessage.error((<Error>msg).message);
        }
        return Promise.reject(msg)
    }

}

async function getDetailProblem(id: number): Promise<ProblemDetailView> {
    const {code, data, message} = await get("/problem-api/problem/get", id)
    if (code !== 200) {
        ElMessage.warning(message)
        return Promise.reject(message)
    }
    return <ProblemDetailView>data
}


export {
    getProblems,
    getDetailProblem
}