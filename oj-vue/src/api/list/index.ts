import {
    type PagedResponse, type PagedType,
    type SortedPagedType,
} from "@/api/pagedType";
import {get, getWithParams, post, put, removeResultNotify, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, remove, update} from "@/utils/simpleCRUD";
import {ProblemType, type ProblemView} from "@/api/problem";
import type {IdType} from "@/api/common.ts";

interface ListView {
    listId: IdType;
    listName: string;
    description: string;
}

interface ListForm {
    listId?: IdType;
    listName?: string;
    description?: string;
}

interface ListProblemQuery extends PagedType{
    listId: IdType,
    problemId?: string | null;
    tagIds?: IdType[] | null;
    title?: string;
    type?: ProblemType;
}

interface ProblemListRelation {
    listId: IdType,
    problemId?: IdType,
    problemOrder?: number,
    score?: number
}

interface ProblemInListView extends ProblemView {
    problemOrder?: number;
    score?: number;
    tempOrder?: number;
    tempScore?: number;
    userScore?: number;
    correct?: boolean;
}





const delProblemFromList = async (relations: ProblemListRelation[]) => {
    const {data} = await put<ProblemListRelation[], boolean>('/problem-api/list/delProblem', relations);
    removeResultNotify(data);
}

const addProblemToList = async (relations: ProblemListRelation[]) => {
    await add(relations, "/problem-api/list/addProblem");
}

const updateProblemRelation = async (relation: ProblemListRelation) => {
    await update(relation, "/problem-api/list/updateProblem")
}

const debouncedAddProblemToList = (relations: ProblemListRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addProblemToList(relations)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}

async function fetchProblemsNotInList(problemParam: ListProblemQuery) {
    const {data} = await post<ListProblemQuery, PagedResponse<ProblemView>>(
        "/problem-api/problem/list-new-problems/" + problemParam.listId, problemParam);
    return data;
}

const debouncedFetchProblemsNotInList = (problemParam: ListProblemQuery, success: successCallback<PagedResponse<ProblemView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        fetchProblemsNotInList(problemParam)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

async function getProblemsAdmin(listId: IdType) {
    const {data} =
        await get<ProblemView[], IdType>("/problem-api/list/getProblems", listId);
    return data;
}

/**
 * 通过题单获取题目，用户专用
 * @param listId 题单ID
 */
async function getProblems(listId: IdType) {
    const {data} =
        await get<ProblemInListView[], IdType>("/problem-api/list/problems", listId);
    return data;
}

const debouncedUserGetProblem = (listId: IdType, success: successCallback<ProblemInListView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getProblems(listId)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}


const debouncedGetProblem = (listId: IdType, success: successCallback<ProblemView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getProblemsAdmin(listId)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

interface QueryList extends SortedPagedType{
    listId?: number;
    listName?: string;
}

const removeList = async (id: IdType | IdType[]) => {
    await remove(id, "/problem-api/list");
}

const addList = async (form: ListForm) => {
    await add(form, "/problem-api/list");
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
    await update(form, "/problem-api/list");
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
    const {data} = await getWithParams<PagedResponse<ListView>, QueryList>("/problem-api/list/list", queryData);
    return data;
}

const debouncedGetList = (queryData: QueryList, success: successCallback<PagedResponse<ListView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getList(queryData)
            .then(success)
            .finally(finish);
    }, 500);
    return {loading, isLoading, get};
}

export type {
    QueryList,
    ListForm,
    ListView,
    ListProblemQuery,
    ProblemListRelation,
    ProblemInListView,

}

export {
    getList,
    debouncedGetList,
    removeList,
    addList,
    debouncedAddList,
    updateList,
    debouncedUpdateList,
    debouncedGetProblem,
    debouncedFetchProblemsNotInList,
    debouncedAddProblemToList,
    delProblemFromList,
    updateProblemRelation,
    debouncedUserGetProblem,
    getProblems
}



