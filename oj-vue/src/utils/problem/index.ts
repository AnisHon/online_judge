import {Difficulty, ProblemType} from "@/api/problem";

export interface ProblemTypeMeta {
    label: string;
    shortLabel: string;
    tone: 'primary' | 'success' | 'warning' | 'info' | 'danger';
    isCode: boolean;
}

const PROBLEM_TYPE_META: Record<number, ProblemTypeMeta> = {
    [ProblemType.OJ]: {label: '编程题', shortLabel: 'OJ', tone: 'primary', isCode: true},
    [ProblemType.FILL]: {label: '填空题', shortLabel: '填空', tone: 'warning', isCode: false},
    [ProblemType.CHOICE]: {label: '单选题', shortLabel: '单选', tone: 'success', isCode: false},
    [ProblemType.MULTI_CHOICE]: {label: '多选题', shortLabel: '多选', tone: 'info', isCode: false},
};

const DIFFICULTY_META: Record<number, {label: string; tone: ProblemTypeMeta['tone']}> = {
    [Difficulty.UNKNOWN]: {label: '未分类', tone: 'info'},
    [Difficulty.SIMPLE]: {label: '简单', tone: 'success'},
    [Difficulty.MEDIUM]: {label: '中等', tone: 'warning'},
    [Difficulty.DIFFICULT]: {label: '困难', tone: 'danger'},
};

export const getProblemTypeMeta = (type?: ProblemType | number): ProblemTypeMeta =>
    PROBLEM_TYPE_META[Number(type)] || {label: '未知题型', shortLabel: '未知', tone: 'info', isCode: false};

export const getDifficultyMeta = (difficulty?: Difficulty | number) =>
    DIFFICULTY_META[Number(difficulty)] || DIFFICULTY_META[Difficulty.UNKNOWN];

const problemTypeToString = (type?: ProblemType | number) => getProblemTypeMeta(type).label;

export {problemTypeToString};
