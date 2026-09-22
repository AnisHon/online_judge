import {
    type PagedResponse, type PagedType,
} from "@/api/pagedType";
import {get, getWithParams, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, remove, update} from "@/utils/simpleCRUD";
import type {IdType} from "@/api/common.ts";

enum ContestAuth {
    PUBLIC,
    PRIVATE,
    WHITE_LIST
}

export enum ContestType {
    CONTEST,
    HOMEWORK
}

interface ContestView {
    contestId: IdType;
    title: string;
    auth: ContestAuth;
    startTime: string;
    endTime: string;
    joinedNumber?: number;
    problemId?: IdType;
    pwd?: string;
    type?: ContestType;
    listId?: IdType;
    description?: string;
}

interface ContestForm {
    contestId?: IdType;
    title?: string;
    auth?: ContestAuth;
    type?: ContestType;
    startTime?: string;
    endTime?: string;
    pwd?: string;
    listId?: IdType;
    description?: string;
}

interface JoinContestRequest {
    contestId: IdType;
    password?: string;
}

interface JoinContestResponse {
    success: boolean;
    message: string;
}

export type PageContest = PagedType & {type: string}

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
    contestType: [
        {
            value: ContestType.CONTEST,
            label: "比赛"
        },
        {
            value: ContestType.HOMEWORK,
            label: "作业"
        }
    ]
}

const joinContest = async (req: JoinContestRequest) => {
    const {data} = await post<JoinContestRequest, JoinContestResponse>("/problem-api/contest/join", req);
    return data;
}

const debouncedJoin = (req: JoinContestRequest, success: successCallback<JoinContestResponse>) => {
    const {loading, isLoading, finish} = useLoading()
    const post = debounce(() => {
        joinContest(req)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, post};
}

const isContestJoined = async (contestId: IdType) => {
    const {data} = await get<boolean>("/problem-api/contest/isJoined", contestId);
    return data;
}

const fetchContestById = async (contestId: IdType) => {
    const {data} = await get<ContestView , IdType>("/problem-api/contest", contestId);
    return data;
}


const debouncedIsJoined = (success: successCallback<boolean>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce((x) => {
        isContestJoined(x)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}


const removeContest = async (id: IdType | IdType[]) => {
    await remove(id, "/problem-api/contest");
}



const addContest = async (form: ContestForm) => {
    await add(form, "/problem-api/contest");
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
    await update(form, "/problem-api/contest");
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
    const { data } = await getWithParams<PagedResponse<ContestView>, typeof page>("/problem-api/contest/page", page);
    return data;
}


const debouncedGetContest = (page: PageContest, success: successCallback<PagedResponse<ContestView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getContest(page)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

const getContestAdmin = async (page: PageContest): Promise<PagedResponse<ContestView>> => {

    const {data} = await getWithParams<PagedResponse<ContestView>, PageContest>("/problem-api/contest/adminPage", page);
    return data;
}

const debouncedGetContestAdmin = (page: PageContest, success: successCallback<PagedResponse<ContestView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getContestAdmin(page)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

const getScore = async (contestId: IdType) => {
    const {data} = await get<number | null>("/problem-api/record/score", contestId);
    return data;
}

export const getContestStatus = async (contestId: IdType) => {
    const {data} = await get<boolean>("/problem-api/contest/status", contestId);
    return data;
}

export const handInPaper = async (contestId: IdType) => {
    const {data} = await post<void, boolean>(`/problem-api/contest/submit/${contestId}`, undefined);
    return data;
}

export type {
    ContestForm,
    ContestView,
    JoinContestRequest,
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
    joinContest,
    isContestJoined,
    fetchContestById,
    getScore,
    dict,
    ContestAuth
}

