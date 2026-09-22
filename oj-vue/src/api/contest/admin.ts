import type {IdType} from "@/api/common.ts";
import type {PagedResponse} from "@/api/pagedType";
import type {ContestForm, ContestView, PageContest} from "@/api/contest";
import {getWithParams} from "@/utils/http";
import {add, remove, update} from "@/utils/simpleCRUD";

export type ContestAdminQuery = PageContest & {
    keyword?: string;
    status?: 'pending' | 'running' | 'ended';
};

const endpoint = "/problem-api/contest";

export const getContestAdminPage = async (query: ContestAdminQuery) => {
    const {data} = await getWithParams<PagedResponse<ContestView>, ContestAdminQuery>(`${endpoint}/adminPage`, query);
    return data;
};

export const createContest = (form: ContestForm) => add(form, endpoint);

export const updateContest = (form: ContestForm) => update(form, endpoint);

export const removeContests = (ids: IdType | IdType[]) => remove(ids, endpoint);
