import {type AjaxResult, type failCallback, type finallyCallback, get, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";

enum OJResult {
    QUEUE = "QUEUE",
    COMPILING = "compiling",
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

// 更新判题状态的时候发送这个judgeMessage
interface JudgeMessage {
    state: OJResult;
    stderr?: string;
    stdout?: string;
}



interface JudgeForm {
    contestId?: number;
    problemId: number;
    languageId?: number;
    answers: Answer[];
    code: string;
    uuid?: string;

}

interface JudgeResponse {
    answers?: Answer[],
    correct: boolean;
    judgeResult: OJResult;
    submitId?: number;
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

interface LogSubmit {
    submitId: number;
    userId: number;
    problemId: number;
    language: string;
    status: OJResult;
    time?: number;
    memory?: number;
    submitTime?: string;
    stderr?: string;
}

interface TestForm {
    languageId?: number;
    stdin?: string;
    code?: string;
    uuid?: string;
}

interface TestResult {
    userId: 0
    judgeResult: OJResult,
    stderr?: string,
    stdout?: string,
}

async function sendTest(testForm: TestForm): Promise<AjaxResult<void | AjaxResult<void>>> {
    return await post<TestForm, void | AjaxResult<void>>("/problem-api/judge/test", testForm);
}

async function testStatus() {
    const {data} = await get<TestResult, void>("/problem-api/judge/test-status");
    return data;
}

async function fetchLog(id: number, success: successCallback<LogSubmit>) {
    const {data} = await get<LogSubmit, number>("/problem-api/log/get", id);
    success(data);
    const intervalId = setInterval(async () => {
        const {data} = await get<LogSubmit, number>("/problem-api/log/get", id);
        success(data);
        if (data.status !== OJResult.COMPILING && data.status !== OJResult.QUEUE) {
            clearInterval(intervalId);
        }
    }, 1000);
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

async function test(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {code, data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge/test", judgeForm, (msg) => {
            ElMessage.warning(msg);
        });

    return data
}


async function judge(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {code, data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm);

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

export type {
    Answer,
    JudgeForm,
    JudgeResponse,
    LogSubmit,
    TestResult,
    TestForm,
    JudgeMessage
}

export {
    judge,
    getDebouncedJudge,
    getUserAnswer,
    saveUserAnswer,
    debouncedSave,
    fetchLog,
    sendTest,
    testStatus,
    OJResult
}


