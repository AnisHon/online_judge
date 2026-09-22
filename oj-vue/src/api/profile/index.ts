import type {IdType} from "@/api/common";
import {ContestType} from '@/api/contest'
import {ApiError, get} from "@/utils/http";

export interface UserProfile {
    userId: IdType;
    userName: string;
    nikeName?: string;
    signature?: string;
    points?: number | string;
    createTime?: string;
    lastLoginTime?: string;
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
    solvedProblems: ProfileDifficultyGroups;
    contests: ProfileContest[];
    solutions: ProfileSolution[];
    solvedProblemsTruncated: boolean;
    contestsTruncated: boolean;
    solutionsTruncated: boolean;
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
                createTime: asDate(solution.createTime),
            }
        }).filter(item => Boolean(item.solutionId && item.problemId))
        : []

    return {
        userId,
        owner: asBoolean(raw.owner),
        solvedCount: Number.isFinite(Number(raw.solvedCount)) ? Number(raw.solvedCount) : 0,
        attemptedCount: Number.isFinite(Number(raw.attemptedCount)) ? Number(raw.attemptedCount) : 0,
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

export const getProfile = async (userId: IdType): Promise<ProfileData> => {
    const [user, activity] = await Promise.all([
        getUserProfile(userId),
        getProfileActivity(userId),
    ]);
    return {user, activity};
};
