import type {IdType} from "@/api/common.ts";
import {get, getWithParams} from "@/utils/http.ts";
import type {UserAnswer} from "@/api/problem/judge.ts";

export interface ProblemStatistic {
    problemId: number;
    title: string;
    average: number;
    score: number;
    wrongNum: number;
    absentNum: number;
    rightNum: number;
}

export interface UserScore {
    problemId: number;
    title: string;
    correct: boolean;
    score: number;

}

export interface ProblemScore {
    userId: number;
    nikeName: string;
    score: number;
    correct: boolean;
}

export interface UserStatistic {
    userId: number;
    nikeName: string;
    submitted: boolean;
    late: boolean;
    score: number;
    correctNum: number;
    wrongNum: number;
    absentNum: number;
    submitTime: string;

}

export type ScoreUserParam = {
    contestId: IdType;
    userId: IdType;
}

export type ScoreProblemParam = {
    contestId: IdType;
    problemId: IdType;
}

export type AnswerParam = {
    contestId: IdType;
    problemId: IdType;
    userId: IdType;
}

export const getProblemStatistic = async (id: IdType): Promise<ProblemStatistic[]> => {
    const {data} = await get<ProblemStatistic[]>("/problem-api/record/statistic/problem", id);
    return data;
}

export const getUserScores = async (id: ScoreUserParam): Promise<UserScore[]> => {
    const {data} = await getWithParams<UserScore[], ScoreUserParam>("/problem-api/record/score/user", id);
    return data;
}

export const getProblemScore = async (id: ScoreProblemParam): Promise<ProblemScore[]> => {
    const {data} = await getWithParams<ProblemScore[], ScoreProblemParam>("/problem-api/record/score/problem", id);
    return data;
}

export const getUserStatistic = async (id: IdType): Promise<UserStatistic[]> => {
    const {data} = await get<UserStatistic[]>("/problem-api/record/statistic/user", id);
    return data;
}

export const getUserAnswer = async (problemId: IdType, contestId: IdType, userId: IdType): Promise<UserAnswer> => {
    const {data} = await getWithParams<UserAnswer, AnswerParam>("/problem-api/record/admin/answer", {
        problemId,
        userId,
        contestId
    });
    return data;
}
