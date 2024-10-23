import {type failCallback, post, type successCallback} from "@/utils/http";
import {debounce} from "@/utils/debounce";

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

async function judge(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm);
    return data
}

const defaultFail = (msg: string) => {
    ElMessage.error(msg)
}

const getDebouncedJudge = (judgeForm: JudgeForm, success: successCallback<JudgeResponse>, fail: failCallback = defaultFail) => {
    return debounce(() => {
        judge(judgeForm, fail).then(success)
    }, 1000);
}


export {
    type Answer,
    type JudgeForm,
    type JudgeResponse,
    judge,
    getDebouncedJudge
}


