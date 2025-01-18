import {get, getWithParams, post, type successCallback} from "@/utils/http"
import {type TagView} from "./label"
import {ElMessage} from "element-plus";
import type {PagedResponse, PagedType} from "@/api/pagedType";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";
import {remove, update} from "@/utils/simpleCRUD";
import {type LogSubmit} from "@/api/problem/judge";
import type {IdType} from "@/api/common.ts";


// 1 OJ, 2 FILL, 3 CHOICE
export enum ProblemType {
    OJ = 1,
    FILL,
    CHOICE,
    MULTI_CHOICE,
}

enum ProblemAuth {
    PUBLIC = 1,
    CONTEST
}





// 难度 (0 未分类, 1 简单, 2 中等, 3 困难),可用值:0,1,2,3
export enum Difficulty {
    UNKNOWN,
    SIMPLE,
    MEDIUM,
    DIFFICULT
}

export interface ProblemView {
    problemId: IdType,
    title: string,
    description: string,
    source: string,
    type: ProblemType
    auth: ProblemAuth,
    createTime: Date,
    hint?: string | null,
}

const dict = {
    problemType:[
        {label: "OJ题", value: ProblemType.OJ},
        {label: "填空题", value: ProblemType.FILL},
        {label: "选择题", value: ProblemType.CHOICE},
        {label: "多选题", value: ProblemType.MULTI_CHOICE},
    ],
    difficulty: [
        {label: '不确定', value: Difficulty.UNKNOWN},
        {label: '简单', value: Difficulty.SIMPLE},
        {label: '中等', value: Difficulty.MEDIUM},
        {label: '困难', value: Difficulty.DIFFICULT},
    ],
    problemAuth: [
        {label: "公开题目", value: ProblemAuth.PUBLIC},
        {label: "比赛题目", value: ProblemAuth.CONTEST},
    ]
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
    tagIds?: IdType[] | null;
    title?: string;
    type?: ProblemType;
}

export interface ChoiceProblemView {
    blankIndex?: number;
    content?: string,
    order?: string | number
}

export interface OjProblemView {
    problemId: IdType,
    difficulty: Difficulty,
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

interface AdminQueryProblem extends PagedType{
    problemId?: IdType;
    title?: string;
    type?: ProblemType;
}



interface Answer {
    answerId?: IdType;
    answerText?: string;
    isCorrect?: boolean;
    blankIndex?: number;
    score?: number;
}

interface OjCase {
    caseId?: IdType;
    input?: string;
    output?: string;
    score?: number;
}

export interface MainProblemForm {
    problemId?: IdType,
    title?: string,
    description?: string,
    source?: string,
    type?: ProblemType
    auth?: ProblemAuth,
    hint?: string | undefined | null,
}

export interface OjProblemForm {
    problemId?: IdType,
    difficulty?: Difficulty,
    memoryLimit?: number,
    stackLimit?: number,
    timeLimit?: number,

    input?: string,
    output?: string,

    inputExample?: string,
    outputExample?: string,

    createTime?: Date,
}


interface ProblemForm {
    problem: MainProblemForm;
    ojProblem: OjProblemForm;
    choices: Answer[];
    cases: OjCase[];
}

export const countProblems = async (): Promise<number> => {
    const {data} = await get<number>("/problem-api/problem/count")
    return data
}

async function getProblemsAdmin(queryProblem: AdminQueryProblem): Promise<PagedResponse<ProblemView>> {
    const {data} =
        await getWithParams<PagedResponse<ProblemView>, AdminQueryProblem>("/problem-api/problem/listAll", queryProblem);
    return data;
}

const debouncedGetProblem = (queryData: AdminQueryProblem, success: successCallback<PagedResponse<ProblemView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getProblemsAdmin(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

async function getAdminDetailProblem(id: IdType | undefined): Promise<ProblemForm> {
    const {code, data, message} = await get<ProblemForm, IdType>("/problem-api/problem/detail", id)
    if (code !== 200) {
        ElMessage.warning(message)
    }
    return data;
}

const debouncedAdminGetProblem = (id: IdType | undefined, success: successCallback<ProblemForm>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getAdminDetailProblem(id)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}


async function removeProblems(ids: IdType | IdType[]) {
    await remove(ids, "/problem-api/problem");
}

async function addProblems(form: ProblemForm) {
    const {data} = await post<ProblemForm, IdType>("/problem-api/problem/addProblem", form);
    return data
}

const debouncedAddProblem = (form: ProblemForm, success: successCallback<IdType>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addProblems(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}

async function updateProblems(form: ProblemForm) {
    await update(form, "/problem-api/problem");
}

const debouncedUpdateProblem = (form: ProblemForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateProblems(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}


async function getProblems(problemParam: ProblemParam): Promise<PagedData> {
    const param: ProblemParam = {currentPage: 0, pageSize: 0}
    Object.assign(param, problemParam);


    try {
        const {data: { data, currentPage, pageSize, totalRecords}}
            = await getWithParams<PagedData, ProblemParam>("/problem-api/problem/taggedList", problemParam);
        return {data, currentPage, pageSize, totalRecords};
    } catch (msg) {
        return Promise.reject(msg);
    }

}

export async function getDetailProblem(id: IdType): Promise<ProblemDetailView> {
    const {code, data, message} = await get("/problem-api/problem", id)
    if (code !== 200) {
        ElMessage.warning(message)
        return Promise.reject(message)
    }
    return <ProblemDetailView>data
}

const recentSubmit = async (problemId: IdType) => {
    const {data} = await get<LogSubmit[], IdType>("/problem-api/log/recentSubmit/", problemId);
    return data
}

const recentProblem = async (): Promise<ProblemView[]> => {
    const {data} = await get<ProblemView[], number>("/problem-api/problem/recentProblems", 15);
    return data
}

const debouncedGetDetailProblem = (success: successCallback<ProblemDetailView>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce((x) => {
        getDetailProblem(x)
            .then(success)
            .finally(finish)
    }, 500);
    return {get, isLoading, loading}
}


export type {
    AdminQueryProblem,
    ProblemForm,
    Answer,
    OjCase
}

export {
    getProblems,
    debouncedGetProblem,
    removeProblems,
    ProblemAuth,
    debouncedAdminGetProblem,
    debouncedUpdateProblem,
    debouncedAddProblem,
    debouncedGetDetailProblem,
    recentSubmit,
    recentProblem,
    dict

}