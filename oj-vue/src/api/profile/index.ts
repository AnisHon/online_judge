import type {IdType} from "@/api/common";
import {ContestType} from '@/api/contest'
import {ApiError, get} from "@/utils/http";
import {useToken} from '@/stores/useToken';

export interface UserProfile {
    userId: IdType;
    userName: string;
    nikeName?: string;
    signature?: string;
    points?: number | string;
    createTime?: string;
    lastLoginTime?: string;
    specialRoles?: string[];
}

export interface ProfileContest {
    contestId: IdType;
    title: string;
    type: ContestType | null;
    startTime?: string;
    endTime?: string;
}

export interface ProfileSolution {
    solutionId: IdType;
    title: string;
    problemId: IdType;
    problemTitle?: string;
    private_?: boolean;
    effectiveVisibility?: 'PUBLIC' | 'AUTHOR_ONLY';
    createTime?: string;
}

export interface ProfileDifficultyGroups {
    easy: IdType[];
    medium: IdType[];
    hard: IdType[];
    unknown: IdType[];
}

export interface ProfileActivity {
    userId: IdType;
    owner: boolean;
    solvedCount: number;
    attemptedCount: number;
    heatmap?: ProfileActivityHeatmap;
    solvedProblems: ProfileDifficultyGroups;
    contests: ProfileContest[];
    solutions: ProfileSolution[];
    solvedProblemsTruncated: boolean;
    contestsTruncated: boolean;
    solutionsTruncated: boolean;
    solutionsError?: string;
}

export interface ProfileActivityDay {
    date: string;
    count: number;
}

export interface ProfileActivityHeatmap {
    startDate: string;
    endDate: string;
    timeZone: 'Asia/Shanghai';
    days: ProfileActivityDay[];
}

const dateTimestamp = (value: unknown): number => {
    if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN
    const timestamp = Date.parse(`${value}T00:00:00Z`)
    return Number.isFinite(timestamp) && new Date(timestamp).toISOString().slice(0, 10) === value ? timestamp : NaN
}

// 兼容旧响应；只接收一年以内的日汇总，不按浏览器时区重新解释日期。
const normalizeHeatmap = (value: unknown): ProfileActivityHeatmap | undefined => {
    if (!value || typeof value !== 'object') return undefined
    const raw = value as Record<string, unknown>
    const start = dateTimestamp(raw.startDate)
    const end = dateTimestamp(raw.endDate)
    const dayCount = (end - start) / 86400000 + 1
    if (!Number.isInteger(dayCount) || dayCount < 365 || dayCount > 366 || raw.timeZone !== 'Asia/Shanghai') return undefined
    const counts = new Map<string, number>()
    if (Array.isArray(raw.days)) {
        for (const item of raw.days.slice(0, 366)) {
            if (!item || typeof item !== 'object') continue
            const day = item as Record<string, unknown>
            const timestamp = dateTimestamp(day.date)
            if (timestamp < start || timestamp > end || !Number.isFinite(timestamp)) continue
            if (typeof day.count === 'number' && Number.isSafeInteger(day.count) && day.count >= 0) {
                counts.set(String(day.date), day.count)
            }
        }
    }
    return {
        startDate: String(raw.startDate),
        endDate: String(raw.endDate),
        timeZone: 'Asia/Shanghai',
        days: Array.from({length: dayCount}, (_, index) => {
            const date = new Date(start + index * 86400000).toISOString().slice(0, 10)
            return {date, count: counts.get(date) ?? 0}
        }),
    }
}

export interface ProfileData {
    user: UserProfile;
    activity: ProfileActivity;
}

export const PROFILE_ACTIVITY_LIMITS = {
    solvedProblems: 300,
    contests: 50,
    solutions: 50,
} as const

const asId = (value: unknown): IdType => value === undefined || value === null ? '' : String(value)
const asText = (value: unknown, fallback = '') => typeof value === 'string' ? value : value == null ? fallback : String(value)
const asDate = (value: unknown): string | undefined => typeof value === 'string' && value ? value : undefined
const asBoolean = (value: unknown) => value === true || value === 1 || value === '1' || value === 'true'

const normalizeContestType = (value: unknown): ContestType | null => {
    const numberValue = typeof value === 'string' && value.trim() !== '' ? Number(value) : value
    if (numberValue === ContestType.CONTEST) return ContestType.CONTEST
    if (numberValue === ContestType.HOMEWORK) return ContestType.HOMEWORK
    if (value === 'CONTEST') return ContestType.CONTEST
    if (value === 'HOMEWORK') return ContestType.HOMEWORK
    return null
}

const normalizeUser = (value: unknown): UserProfile => {
    if (!value || typeof value !== 'object') throw new ApiError('个人资料接口返回数据不完整', 502)
    const raw = value as Record<string, unknown>
    const userId = asId(raw.userId)
    if (!userId || !asText(raw.userName)) throw new ApiError('个人资料接口返回数据不完整', 502)
    return {
        userId,
        userName: asText(raw.userName),
        nikeName: asText(raw.nikeName),
        signature: asText(raw.signature),
        points: typeof raw.points === 'number' || typeof raw.points === 'string' ? raw.points : 0,
        createTime: asDate(raw.createTime),
        lastLoginTime: asDate(raw.lastLoginTime),
        specialRoles: Array.isArray(raw.specialRoles) ? raw.specialRoles.map(item => asText(item)).filter(Boolean) : [],
    }
}

const normalizeActivity = (value: unknown): ProfileActivity => {
    if (!value || typeof value !== 'object') throw new ApiError('个人活动接口返回数据不完整', 502)
    const raw = value as Record<string, unknown>
    const userId = asId(raw.userId)
    if (!userId) throw new ApiError('个人活动接口返回数据不完整', 502)
    const rawGroups = raw.solvedProblems && typeof raw.solvedProblems === 'object'
        ? raw.solvedProblems as Record<string, unknown>
        : {}
    const groups = (key: string): IdType[] => Array.isArray(rawGroups[key])
        ? rawGroups[key].map(asId).filter(Boolean)
        : []
    const contests = Array.isArray(raw.contests)
        ? raw.contests.map(item => {
            const contest = item && typeof item === 'object' ? item as Record<string, unknown> : {}
            return {
                contestId: asId(contest.contestId),
                title: asText(contest.title, '未命名活动'),
                type: normalizeContestType(contest.type),
                startTime: asDate(contest.startTime),
                endTime: asDate(contest.endTime),
            }
        }).filter(item => Boolean(item.contestId))
        : []
    const solutions = Array.isArray(raw.solutions)
        ? raw.solutions.map(item => {
            const solution = item && typeof item === 'object' ? item as Record<string, unknown> : {}
            return {
                solutionId: asId(solution.solutionId),
                title: asText(solution.title, '未命名题解'),
                problemId: asId(solution.problemId),
                problemTitle: asText(solution.problemTitle, '题目'),
                private_: asBoolean(solution.private_ ?? solution.private),
                effectiveVisibility: solution.effectiveVisibility === 'AUTHOR_ONLY' ? 'AUTHOR_ONLY' as const
                    : solution.effectiveVisibility === 'PUBLIC' ? 'PUBLIC' as const : undefined,
                createTime: asDate(solution.createTime),
            }
        }).filter(item => Boolean(item.solutionId && item.problemId))
        : []

    return {
        userId,
        owner: asBoolean(raw.owner),
        solvedCount: Number.isFinite(Number(raw.solvedCount)) ? Number(raw.solvedCount) : 0,
        attemptedCount: Number.isFinite(Number(raw.attemptedCount)) ? Number(raw.attemptedCount) : 0,
        heatmap: normalizeHeatmap(raw.heatmap),
        solvedProblems: {easy: groups('easy'), medium: groups('medium'), hard: groups('hard'), unknown: groups('unknown')},
        contests,
        solutions,
        solvedProblemsTruncated: asBoolean(raw.solvedProblemsTruncated),
        contestsTruncated: asBoolean(raw.contestsTruncated),
        solutionsTruncated: asBoolean(raw.solutionsTruncated),
    }
}

export const getUserProfile = async (userId: IdType): Promise<UserProfile> => {
    const result = await get<UserProfile | null>(`/user-api/user/profile/${encodeURIComponent(String(userId))}`)
    return normalizeUser(result.data)
};

export const getProfileActivity = async (userId: IdType): Promise<ProfileActivity> => {
    const result = await get<ProfileActivity | null>(`/problem-api/profile/${encodeURIComponent(String(userId))}`)
    return normalizeActivity(result.data)
};

export class StaleProfileResponseError extends Error {}

export const getProfileSolutions = async (userId: IdType): Promise<Pick<ProfileActivity, 'solutions' | 'solutionsTruncated'>> => {
    const result = await get<Record<string, unknown> | null>(
        `/content-api/profile/${encodeURIComponent(String(userId))}/solutions`)
    if (!result.data || asId(result.data.userId) !== String(userId) || !Array.isArray(result.data.solutions))
        throw new ApiError('题解接口返回数据不完整', 502)
    const normalized = normalizeActivity(result.data)
    return {solutions: normalized.solutions, solutionsTruncated: normalized.solutionsTruncated}
}

// The caller owns request generation; sessionVersion additionally fences account changes.
export const getProfile = async (userId: IdType, isCurrent: () => boolean = () => true): Promise<ProfileData> => {
    const token = useToken()
    const sessionVersion = token.getSessionVersion()
    const [user, activity, solutions] = await Promise.all([
        getUserProfile(userId),
        getProfileActivity(userId),
        getProfileSolutions(userId).then(data => ({data, error: undefined})).catch(() => ({
            data: undefined, error: '题解暂时无法加载，请稍后重试'
        })),
    ]);
    if (!isCurrent() || sessionVersion !== token.getSessionVersion()) throw new StaleProfileResponseError()
    if (String(user.userId) !== String(userId) || String(activity.userId) !== String(userId))
        throw new ApiError('个人主页接口返回数据不匹配', 502)
    // Never fall back to solutions embedded in an old problem-service response.
    activity.solutions = solutions.data?.solutions ?? []
    activity.solutionsTruncated = solutions.data?.solutionsTruncated ?? false
    activity.solutionsError = solutions.error
    return {user, activity};
};
