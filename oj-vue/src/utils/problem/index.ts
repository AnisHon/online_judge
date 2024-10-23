import type {ProblemType} from "@/api/problem";

const problemTypeToString = (type: ProblemType) => {
    const names = ['', 'OJ题目', '填空题', '选择题', '多选题'];
    return names[type]
}

export {
    problemTypeToString,
}