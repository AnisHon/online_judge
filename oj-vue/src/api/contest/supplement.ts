import type {IdType} from "@/api/common.ts";
import {del, get, post, resultNotify} from "@/utils/http.ts";

export interface Supplement {
    contestId: IdType;
    userId: IdType;
    nikeName: string;
    deadline: Date;
}

export interface SupplementForm {
    userId?: IdType;
    contestId: IdType;
    deadline?: Date;
}


export const getSupplements = async (contestId: IdType) => {
    const {data} = await get<Supplement[]>("/problem-api/contest/lateSubmission", contestId);
    return data;
}

export const removeSupplement = async (contestId: IdType, userId: IdType) => {
    const {data} = await del<boolean>(`/problem-api/contest/lateSubmission/${contestId}/${userId}`);
    resultNotify(data, "删除成功", "删除失败");
    if (data !== true) throw new Error("删除迟交权限失败");
}

export const addSupplement = async (form: SupplementForm) => {
    const {data} = await post<SupplementForm, boolean>("/problem-api/contest/lateSubmission", form);
    resultNotify(data, "添加成功", "添加失败")
}
