import {
    type PagedResponse, type PagedType,
} from "@/api/pagedType";
import {get, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, pagedFetch, remove, update} from "@/utils/simpleCRUD";
import type {UserView} from "@/api/user";

enum ContestAuth {
    PUBLIC,
    PRIVATE,
    WHITE_LIST
}

interface ContestView {
    contestId: number;
    title: string;
    auth: ContestAuth;
    startTime: string;
    endTime: string;
    joinedNumber?: number;
    problemId?: number;
    pwd?: string;
    listId?: number;
    description?: string;
}

interface ContestForm {
    contestId?: number;
    title?: string;
    auth?: ContestAuth;
    startTime?: Date;
    endTime?: Date;
    pwd?: string;
    listId?: number;
    description?: string;
}

interface JoinContestRequest {
    contestId: number;
    password?: string;
}

interface JoinContestResponse {
    success: boolean;
    message: string;
}

interface ScoredUser {
    userVo: UserView;
    score: number;
}

interface StatisticProblem {
    problemId: number;
    title: string;
    rightNum: number;
    wrongNum: number;
}

const dict = {
    contestAuth: [
        {
            value: ContestAuth.PUBLIC,
            label: "公开赛"
        }, {
            value: ContestAuth.PRIVATE,
            label: "私有赛"
        }, {
            value: ContestAuth.WHITE_LIST,
            label: "白名单"
        }
    ],
}

const rank = async (id: number) => {
    const {data} = await get<ScoredUser[], number>("/problem-api/record/rank", id);
    return data;
}

const statistic = async (id: number) => {
    const {data} = await get<StatisticProblem[], number>("/problem-api/record/statistic", id);
    return data;
}

const join = async (req: JoinContestRequest) => {
    const {data} = await post<JoinContestRequest, JoinContestResponse>("/problem-api/contest/join", req);
    return data;
}

const debouncedJoin = (req: JoinContestRequest, success: successCallback<JoinContestResponse>) => {
    const {loading, isLoading, finish} = useLoading()
    const post = debounce(() => {
        join(req)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, post};
}

const isJoined = async (contestId: number) => {
    const {data} = await get<boolean>("/problem-api/contest/is-joined", contestId);
    return data;
}

const fetchContestById = async (contestId: number) => {
    const {data} = await get<ContestView , number>("/problem-api/contest/get", contestId);
    return data;
}


const debouncedIsJoined = (success: successCallback<boolean>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce((x) => {
        isJoined(x)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


const removeContest = async (id: number | number[]) => {
    await remove(id, "/problem-api/contest/removeBatch", "/problem-api/contest/remove");
}



const addContest = async (form: ContestForm) => {
    await add(form, "/problem-api/contest/add");
}

const debouncedAddContest = (form: ContestForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addContest(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateContest = async (form: ContestForm) => {
    await update(form, "/problem-api/contest/update");
}

const debouncedUpdateContest = (form: ContestForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateContest(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getContest = async (page: PagedType): Promise<PagedResponse<ContestView>> => {
    return await pagedFetch(page, "/problem-api/contest/page");
}


const debouncedGetContest = (page: PagedType, success: successCallback<PagedResponse<ContestView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getContest(page)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

const getContestAdmin = async (page: PagedType): Promise<PagedResponse<ContestView>> => {
    return await pagedFetch(page, "/problem-api/contest/admin-page");
}

const debouncedGetContestAdmin = (page: PagedType, success: successCallback<PagedResponse<ContestView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getContestAdmin(page)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

const getScore = async (contestId: number) => {
    const {data} = await get<number | null>("/problem-api/record/score", contestId);
    return data;
}

export type {
    ContestForm,
    ContestView,
    JoinContestRequest,
    StatisticProblem,
    ScoredUser
}

export {
    getContest,
    debouncedGetContest,
    removeContest,
    addContest,
    debouncedAddContest,
    updateContest,
    debouncedUpdateContest,
    debouncedGetContestAdmin,
    debouncedIsJoined,
    debouncedJoin,
    fetchContestById,
    getScore,
    rank,
    statistic,
    dict,
    ContestAuth
}



