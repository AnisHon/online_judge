import {type failCallback, type finallyCallback, post, type successCallback} from "@/utils/http";
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
    answers?: Answer[],
    correct: boolean,
    totalScore: string
}

async function judge(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {code, data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm, fail);

    if (code === 400) {
        return {
            answers: [],
            correct: false,
            totalScore: "0"
        }
    }
    return data
}

const defaultFail = (msg: string) => {
    ElMessage.error("提交出错")

}

const getDebouncedJudge = (
    judgeForm: JudgeForm,
    success: successCallback<JudgeResponse>,
    fail: failCallback = defaultFail,
    final: finallyCallback = () => {}
) => {
    return debounce(() => {

        judge(judgeForm, fail).then(success).finally(final);
        console.log()
    }, 1000);

}


export {
    type Answer,
    type JudgeForm,
    type JudgeResponse,
    judge,
    getDebouncedJudge
}


