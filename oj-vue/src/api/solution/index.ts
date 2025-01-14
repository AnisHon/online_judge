import type {PagedResponse, SortedPagedType} from "@/api/pagedType.ts";
import {type finallyCallback, get, post, put, query, type successCallback} from "@/utils/http.ts";
import {remove} from "@/utils/simpleCRUD.ts";
import {ElNotification} from "element-plus";
import __ from "lodash";

export interface Solution {
    solutionId: number;
    title: string;
    topUp: boolean;
    userId: number;
    nikeName: string;
    private_: boolean;
    problemId: number;
    problemTitle: string;
    content: string;
    createTime: Date;
    updateTime: Date;
}

export interface SolutionForm {
    solutionId?: number;
    problemId?: number;
    topUp?: boolean;
    title: string;
    private_: boolean;
    content: string
}


export interface QuerySolution extends SortedPagedType {
    problemId?: number;
    userId?: number;
}

export const getSolution = async (id: number): Promise<Solution> => {
    const {data} = await get<Solution>("/problem-api/solution", id);
    return data;
}

export const getSolutionAdmin = async (id: number): Promise<Solution> => {
    const {data} = await get<Solution>("/problem-api/solution/admin", id);
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

export const debouncedAddSolution = (success: successCallback<boolean>, final: finallyCallback) => {
    return __.debounce((form: SolutionForm) => {
        addSolution(form)
            .then(success)
            .finally(final)
    }, 1000);
}

const addSolution = async (form: SolutionForm) => {
    const {data} = await post<SolutionForm, boolean>("/problem-api/solution", form);
    if (data) {
        ElNotification.success("发送了一个题解");
    } else {
        ElNotification.warning("题解发送失败");
    }
    return data;
}

export const debouncedAddSolutionAdmin = (success: successCallback<boolean>, final: finallyCallback) => {
    return __.debounce((form: SolutionForm) => {
        addSolutionAdmin(form)
            .then(success)
            .finally(final)
    }, 1000);
}

const addSolutionAdmin = async (form: SolutionForm) => {
    const {data} = await post<SolutionForm, boolean>("/problem-api/solution/admin", form);
    if (data) {
        ElNotification.success("发送了一个题解");
    } else {
        ElNotification.warning("题解发送失败");
    }
    return data;
}

export const debouncedEditSolution = (success: successCallback<boolean>, final: finallyCallback) => {
    return __.debounce((form: SolutionForm) => {
        editSolution(form)
            .then(success)
            .finally(final)
    }, 1000);
}

const editSolution = async (form: SolutionForm) => {
    const {data} = await put<SolutionForm, boolean>("/problem-api/solution", form);
    if (data) {
        ElNotification.success("编辑了一个题解");
    } else {
        ElNotification.warning("题解编辑失败");
    }
    return data;
}

export const debouncedEditSolutionAdmin = (success: successCallback<boolean>, final: finallyCallback) => {
    return __.debounce((form: SolutionForm) => {
        editSolutionAdmin(form)
            .then(success)
            .finally(final)
    }, 1000);
}

const editSolutionAdmin = async (form: SolutionForm) => {
    const {data} = await put<SolutionForm, boolean>("/problem-api/solution/admin", form);
    if (data) {
        ElNotification.success("编辑了一个题解");
    } else {
        ElNotification.warning("题解编辑失败");
    }
    return data;
}

export const debouncedDeleteSolution = (final: finallyCallback) => {
    return __.debounce((id: number | number[]) => {
        deleteSolution(id)
            .finally(final)
    }, 1000);
}

const deleteSolution = async (ids: number[] | number) => {
    await remove(ids, "/problem-api/solution");

}

export const deleteSolutionAdmin = async (ids: number[] | number) => {
    await remove(ids, "/problem-api/solution/admin");
}

export const topUp = async (id: number): Promise<boolean> => {
    const {data} = await put<undefined, boolean>("/problem-api/solution/admin/topUp/" + id, undefined);
    return data;
}

export const lowDown = async (id: number): Promise<boolean> => {
    const {data} = await put<undefined, boolean>("/problem-api/solution/admin/lowDown/" + id, undefined);
    return data;
}