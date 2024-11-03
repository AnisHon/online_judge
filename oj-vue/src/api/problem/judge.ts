import {type failCallback, type finallyCallback, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";

enum OJResult {
    ACCEPT = "AC",
    RUNTIME_ERROR = "RE",
    WRONG_ANSWER = "WA",
    TIME_LIMIT_EXCEEDED = "TLE",
    MEMORY_LIMIT_EXCEEDED = "MLE",
    COMPILE_ERROR = "CE",
}

interface Answer {
    index: number;
    answer: string;
}


interface JudgeForm {
    contestId?: number;
    problemId: number;
    languageId?: number;
    answers: Answer[];
    code: string;

}

interface JudgeResponse {
    answers?: Answer[],
    correct: boolean;
    judgeResult: OJResult;
    errorMessage: string;
    totalScore: string;
    fullMark: string;
}

interface UserAnswer {
    answers?: Answer[];
    code?: string;
    languageId?: number;
}

interface UserAnswerRequest {
    contestId?: number;
    problemId?: number;
}

async function getUserAnswer(req: UserAnswerRequest): Promise<UserAnswer> {
    const {data} = await post<UserAnswerRequest, UserAnswer>("/problem-api/record/get", req);
    return data;
}

async function saveUserAnswer(judgeForm: JudgeForm): Promise<boolean> {
    const {data} = await post<JudgeForm, boolean>("/problem-api/record/save", judgeForm);
    return data;
}
const debouncedSave = () => {
    return debounce((x) => {
        saveUserAnswer(x)
            .then((data) => {
                if (data) {
                    ElMessage.success("保存成功")
                } else {
                    ElMessage.success("保存失败")
                }
            })
    }, 500)
}


async function judge(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {code, data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm, fail);
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
    getDebouncedJudge,
    getUserAnswer,
    saveUserAnswer,
    debouncedSave
}


