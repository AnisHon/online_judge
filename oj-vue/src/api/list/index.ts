import {
    type PagedResponse, type PagedType,
    type SortedPagedType,
} from "@/api/pagedType";
import {get, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";
import {add, postedRemove, remove, update} from "@/utils/simpleCRUD";
import {ProblemType, type ProblemView} from "@/api/problem";

interface ListView {
    listId: number;
    listName: string;
    description: string;
}

interface ListForm {
    listId?: number;
    listName?: string;
    description?: string;
}

interface ListProblemQuery extends PagedType{
    listId: number,
    problemId?: string | null;
    tagIds?: number[] | null;
    title?: string;
    type?: ProblemType;
}

interface ProblemListRelation {
    listId: number,
    problemId?: number,
    problemOrder?: number,
    score?: number
}

interface ProblemInListView extends ProblemView {
    problemOrder?: number;
    score?: number;
    tempOrder?: number;
    tempScore?: number;
}





const delProblemFromList = async (relations: ProblemListRelation[]) => {
    await postedRemove(relations, '/problem-api/list/del-problem', '/problem-api/list/del-problem');
}

const addProblemToList = async (relations: ProblemListRelation[]) => {
    await add(relations, "/problem-api/list/add-problem");
}

const updateProblemRelation = async (relation: ProblemListRelation) => {
    await update(relation, "/problem-api/list/update-problem")
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

async function getProblemsAdmin(listId: number) {
    const {data} =
        await get<ProblemView[], number>("/problem-api/list/get-problems", listId);
    return data;
}

/**
 * 通过题单获取题目，用户专用
 * @param listId 题单ID
 */
async function getProblems(listId: number) {
    const {data} =
        await get<ProblemInListView[], number>("/problem-api/list/problems", listId);
    return data;
}

const debouncedUserGetProblem = (listId: number, success: successCallback<ProblemInListView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getProblems(listId)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}


const debouncedGetProblem = (listId: number, success: successCallback<ProblemView[]>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getProblemsAdmin(listId)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

interface QueryList extends SortedPagedType{
    listId?: number;
    listName?: string;
}

const removeList = async (id: number | number[]) => {
    await remove(id, "/problem-api/list/del", "/problem-api/list/del");
}

const addList = async (form: ListForm) => {
    await add(form, "/problem-api/list/add");
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
    await update(form, "/problem-api/list/update");
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
    const {data} = await post<QueryList, PagedResponse<ListView>>("/problem-api/list/list", queryData);
    return data;
}

const debouncedGetList = (queryData: QueryList, success: successCallback<PagedResponse<ListView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getList(queryData)
            .then(success)
            .finally(finish);
    }, 1000);
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



