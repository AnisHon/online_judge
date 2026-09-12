import {
    type AjaxResult,
    type failCallback,
    type finallyCallback,
    get,
    getWithParams,
    post,
    type successCallback
} from "@/utils/http";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";

enum OJResult {
    QUEUE = "QUEUE",
    COMPILING = "compiling",
    RUNNING = "running",
    ACCEPT = "AC",
    RUNTIME_ERROR = "RE",
    WRONG_ANSWER = "WA",
    TIME_LIMIT_EXCEEDED = "TLE",
    MEMORY_LIMIT_EXCEEDED = "MLE",
    COMPILE_ERROR = "CE",
    JUDGE_ERROR = "JUDGE_ERROR",
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
    contestId?: IdType;
    problemId: IdType;
    languageId?: IdType;
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

export interface UserAnswer {
    answers?: Answer[];
    code?: string;
    languageId?: IdType;
}

interface UserAnswerRequest {
    contestId?: IdType;
    problemId?: IdType;
}

interface LogSubmit {
    submitId: IdType;
    userId: IdType;
    problemId: IdType;
    language: string;
    status: OJResult;
    time?: number;
    memory?: number;
    submitTime?: string;
    stderr?: string;
    code?: string;
    totalCount?: number;
    passCount?: number;
}

interface TestForm {
    languageId?: IdType;
    stdin?: string;
    code?: string;
    uuid?: string;
}

interface TestResult {
    userId: IdType
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
    const {data} = await get<LogSubmit, number>("/problem-api/log/submissions", id);
    success(data);
    const intervalId = setInterval(async () => {
        const {data} = await get<LogSubmit, number>("/problem-api/log/submissions", id);
        success(data);
        if (data.status !== OJResult.COMPILING && data.status !== OJResult.QUEUE && data.status !== OJResult.RUNNING) {
            clearInterval(intervalId);
        }
    }, 1000);
}

/**
 * 不依赖 SSE 的提交状态轮询。默认每秒查询一次，返回停止函数供页面卸载时清理。
 */
async function pollSubmission(
    id: IdType,
    onUpdate: (log: LogSubmit) => void,
    interval = 1000
): Promise<() => void> {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const poll = async () => {
        if (stopped) return;
        const {data} = await get<LogSubmit, IdType>("/problem-api/log/submissions", id);
        if (data) onUpdate(data);
        if (!stopped && data && [OJResult.QUEUE, OJResult.COMPILING, OJResult.RUNNING].includes(data.status)) {
            timer = setTimeout(poll, interval);
        }
    };
    await poll();
    return () => {
        stopped = true;
        if (timer) clearTimeout(timer);
    };
}

async function getUserAnswer(req: UserAnswerRequest): Promise<UserAnswer> {
    const {data} = await getWithParams<UserAnswer, UserAnswerRequest>("/problem-api/record", req);
    return data;
}

async function saveUserAnswer(judgeForm: JudgeForm): Promise<boolean> {
    const {data} = await post<JudgeForm, boolean>("/problem-api/record", judgeForm);
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

const defaultFail = (msg: string = "提交出错") => {
    ElMessage.error(msg)

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
    pollSubmission,
    sendTest,
    testStatus,
    OJResult
}

