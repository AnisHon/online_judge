import type {PagedResponse, SortedPagedType} from "@/api/pagedType.ts";
import {ApiError, get, post, put, query} from "@/utils/http.ts";
import {remove} from "@/utils/simpleCRUD.ts";
import type {IdType} from "@/api/common.ts";

export interface Solution {
    solutionId: IdType;
    title: string;
    topUp: boolean;
    userId: IdType;
    nikeName: string;
    private_: boolean;
    problemId: IdType;
    problemTitle: string;
    content: string;
    createTime: Date;
    updateTime: Date;
}

export interface SolutionForm {
    solutionId?: IdType;
    problemId?: IdType;
    topUp?: boolean;
    title: string;
    private_: boolean;
    content: string
}


export interface QuerySolution extends SortedPagedType {
    problemId?: IdType;
    userId?: IdType;
}

export const getSolution = async (id: IdType): Promise<Solution> => {
    const {data} = await get<Solution | null>("/problem-api/solution", id);
    if (!data) {
        throw new ApiError("题解不存在或无权访问", 404);
    }
    return data;
}

export const getSolutionAdmin = async (id: IdType): Promise<Solution> => {
    const {data} = await get<Solution | null>("/problem-api/solution/admin", id);
    if (!data) {
        throw new ApiError("题解不存在", 404);
    }
    return data;
}

export const listSolution = async (param: QuerySolution): Promise<PagedResponse<Solution>> => {
    const {data} = await query<Solution, QuerySolution>("/problem-api/solution/list", param);
    return data;
}

export const listSolutionAdmin = async (param: QuerySolution): Promise<PagedResponse<Solution>> => {
    const {data} = await query<Solution, QuerySolution>("/problem-api/solution/admin/list", param);
    return data;
}

type SolutionMutation = 'add' | 'edit'

const mutateSolution = async (
    form: SolutionForm,
    mutation: SolutionMutation,
    admin = false
): Promise<boolean> => {
    const url = `/problem-api/solution${admin ? '/admin' : ''}`;
    const response = mutation === 'add'
        ? await post<SolutionForm, boolean>(url, {...form})
        : await put<SolutionForm, boolean>(url, {...form});

    if (response.data !== true) {
        throw new ApiError(mutation === 'add' ? '题解发布失败' : '题解保存失败', 400);
    }
    return true;
}

/** 题解提交由页面显式管理 loading/错误/请求锁，API 层不再隐藏 debounce 定时器。 */
export const addSolution = (form: SolutionForm, admin = false): Promise<boolean> =>
    mutateSolution(form, 'add', admin);

export const editSolution = (form: SolutionForm, admin = false): Promise<boolean> =>
    mutateSolution(form, 'edit', admin);

export const deleteSolution = async (ids: IdType[] | IdType): Promise<void> => {
    await remove(ids, "/problem-api/solution");
}

export const deleteSolutionAdmin = async (ids: IdType[] | IdType) => {
    await remove(ids, "/problem-api/solution/admin");
}

export const topUp = async (id: IdType): Promise<boolean> => {
    const {data} = await put<undefined, boolean>("/problem-api/solution/admin/topUp/" + id, undefined);
    return data;
}

export const lowDown = async (id: IdType): Promise<boolean> => {
    const {data} = await put<undefined, boolean>("/problem-api/solution/admin/lowDown/" + id, undefined);
    return data;
}

export const recentSolution = async () => {
    const {data} = await get<Solution[]>("/problem-api/solution/recent");
    return data;
}
