import {addResultNotify, get, service} from "@/utils/http.ts";
import type {IdType} from "@/api/common.ts";
import type {UserView} from "@/api/user";

export const addUserDirect = async (contestId: IdType, userIds: IdType | IdType[]) => {
    const {data} = await service.post<boolean>(`/problem-api/contest/user/${contestId}/${userIds}`);
    addResultNotify(data);
}

export const addUserByClass = async (contestId: IdType, classId: IdType) => {
    const {data} = await service.post<boolean>(`/problem-api/contest/user/${contestId}/class/${classId}`);
    addResultNotify(data);
}

export const removeUser = async (contestId: IdType, userIds: IdType | IdType[]) => {
    const {data} = await service.delete<boolean>(`/problem-api/contest/user/${contestId}/${userIds}`);
    addResultNotify(data);
}

export const getUserByContest = async (contestId: IdType) => {
    const {data} = await get<UserView[]>("/problem-api/contest/user", contestId);
    return data;
}