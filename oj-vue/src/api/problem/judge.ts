import {post} from "@/utils/http";

interface Answer {
    index: number;
    answer: string ;
}


interface JudgeForm {
    contestId?: number;
    problemId: number;
    languageId?: number;
    answers: Answer[];

}

interface JudgeResponse {
    answers: Answer[],
    correct: boolean,
    totalScore: string
}

async function judge(judgeForm: JudgeForm): Promise<JudgeResponse> {
    const {data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm);
    return data
}

export {
    type Answer,
    type JudgeForm,
}


