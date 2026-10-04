import type {PagedResponse, SortedPagedType} from "@/api/pagedType.ts";
import {ApiError, get, post, put, query, service, type AjaxResult} from "@/utils/http.ts";
import type {IdType} from "@/api/common.ts";

export interface Solution {
    solutionId: IdType;
    title: string;
    topUp: boolean;
    userId: IdType;
    nikeName: string;
    private_: boolean;
    effectiveVisibility?: 'PUBLIC' | 'AUTHOR_ONLY' | 'DELETED';
    likeCount?: string | number | null;
    likedByMe?: boolean;
    commentCount?: string | number | null;
    commentsOpen?: boolean;
    moderationState?: string;
    problemId: IdType;
    problemTitle: string;
    content: string;
    createTime: Date;
    updateTime: Date;
}

export interface LikeResult {
    liked: boolean;
    likeCount: string | number | null;
}

export interface ModerationAction {
    action: string;
    reason: string;
    createdAt: string;
    targetType?: string;
    actionId?: IdType;
    solutionId?: IdType;
    targetId?: IdType;
    authorId?: IdType;
    operatorId?: IdType;
}

export interface SolutionForm {
    solutionId?: IdType;
    problemId?: IdType;
    topUp?: boolean;
    title: string;
    private_?: boolean;
    content: string
}


export interface QuerySolution extends SortedPagedType {
    problemId?: IdType;
    userId?: IdType;
}

export const getSolution = async (id: IdType): Promise<Solution> => {
    const {data} = await get<Solution | null>("/content-api/solution", id);
    if (!data) {
        throw new ApiError("题解不存在或无权访问", 404);
    }
    return data;
}

export const getSolutionAdmin = async (id: IdType, signal?: AbortSignal): Promise<Solution> => {
    const {data} = await service.get<Solution | null, AjaxResult<Solution | null>>(
        `/content-api/solution/admin/${encodeURIComponent(id)}`, {signal, localForbidden: true});
    if (!data) {
        throw new ApiError("题解不存在", 404);
    }
    return data;
}

export const listSolution = async (param: QuerySolution): Promise<PagedResponse<Solution>> => {
    const {data} = await query<Solution, QuerySolution>("/content-api/solution/list", param);
    return data;
}

export const listSolutionAdmin = async (param: QuerySolution, signal?: AbortSignal): Promise<PagedResponse<Solution>> => {
    const {data} = await service.get<PagedResponse<Solution>, AjaxResult<PagedResponse<Solution>>>(
        '/content-api/solution/admin/list', {params: {...param}, signal, localForbidden: true});
    return data;
}

type SolutionMutation = 'add' | 'edit'

const mutateSolution = async (
    form: SolutionForm,
    mutation: SolutionMutation,
    admin = false
): Promise<boolean> => {
    if (mutation === 'add' && form.solutionId !== undefined) {
        throw new ApiError('新增题解不能携带已有题解 ID', 400);
    }
    const url = `/content-api/solution${admin ? '/admin' : ''}`;
    const response = admin
        ? mutation === 'add'
            ? await service.post<SolutionForm, AjaxResult<boolean>>(url, {...form}, {localForbidden: true})
            : await service.put<SolutionForm, AjaxResult<boolean>>(url, {...form}, {localForbidden: true})
        : mutation === 'add'
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
    const targets = Array.isArray(ids) ? ids : [ids];
    if (!targets.length) throw new ApiError('请选择题解', 400);
    const {data} = await service.delete<boolean, AjaxResult<boolean>>(
        `/content-api/solution/${targets.map(encodeURIComponent).join(',')}`, {localForbidden: true});
    if (data !== true) throw new ApiError('题解未删除，请刷新后重试', 409);
}

export const deleteSolutionAdmin = async (id: IdType, reason: string, signal?: AbortSignal): Promise<void> => {
    const {data} = await service.post<{reason: string}, AjaxResult<{deleted: boolean}>>(
        `/content-api/solution/admin/${encodeURIComponent(id)}/delete`, {reason}, {signal, localForbidden: true});
    if (data?.deleted !== true) throw new ApiError('题解未删除，请刷新后重试', 409);
}

export const moderateSolution = async (id: IdType, action: 'AUTHOR_ONLY' | 'RESTORE', reason: string,
                                      signal?: AbortSignal): Promise<void> => {
    const {data} = await service.post<{action: string; reason: string}, AjaxResult<{moderationState: string}>>(
        `/content-api/solution/admin/${encodeURIComponent(id)}/moderation`, {action, reason},
        {signal, localForbidden: true});
    if (data?.moderationState !== (action === 'RESTORE' ? 'NORMAL' : 'AUTHOR_ONLY')) {
        throw new ApiError('题解状态未更新，请刷新后重试', 409);
    }
}

export const getSolutionModerationHistory = async (id: IdType, currentPage = 1,
                                                 signal?: AbortSignal): Promise<PagedResponse<ModerationAction>> => {
    const {data} = await service.get<PagedResponse<ModerationAction>, AjaxResult<PagedResponse<ModerationAction>>>(
        `/content-api/solution/admin/${encodeURIComponent(id)}/moderation-history`,
        {params: {currentPage, pageSize: 20}, signal, localForbidden: true});
    return data;
}

export const topUp = async (id: IdType): Promise<boolean> => {
    const {data} = await service.put<undefined, AjaxResult<boolean>>(
        `/content-api/solution/admin/topUp/${encodeURIComponent(id)}`, undefined, {localForbidden: true});
    return data;
}

export const lowDown = async (id: IdType): Promise<boolean> => {
    const {data} = await service.put<undefined, AjaxResult<boolean>>(
        `/content-api/solution/admin/lowDown/${encodeURIComponent(id)}`, undefined, {localForbidden: true});
    return data;
}

export const recentSolution = async () => {
    const {data} = await get<Solution[]>("/content-api/solution/recent");
    return data;
}

export const likeSolution = async (id: IdType, signal?: AbortSignal): Promise<LikeResult> => {
    const {data} = await service.put<LikeResult, AjaxResult<LikeResult>>(
        `/content-api/solution/${encodeURIComponent(id)}/like`, undefined,
        {signal, localForbidden: true}
    );
    return data;
}

export const unlikeSolution = async (id: IdType, signal?: AbortSignal): Promise<LikeResult> => {
    const {data} = await service.delete<LikeResult, AjaxResult<LikeResult>>(
        `/content-api/solution/${encodeURIComponent(id)}/like`,
        {signal, localForbidden: true}
    );
    return data;
}

export const setSolutionCommentsState = async (id: IdType, open: boolean): Promise<{commentsOpen: boolean}> => {
    const {data} = await service.put<{commentsOpen: boolean}, AjaxResult<{commentsOpen: boolean}>>(
        `/content-api/solution/${encodeURIComponent(id)}/comments-state`, {open},
        {localForbidden: true}
    );
    return data;
}

export const getSolutionModeration = async (id: IdType, signal?: AbortSignal): Promise<ModerationAction[]> => {
    const {data} = await service.get<ModerationAction[], AjaxResult<ModerationAction[]>>(
        `/content-api/solution/${encodeURIComponent(id)}/moderation`, {signal, localForbidden: true}
    );
    return Array.isArray(data) ? data : [];
}
