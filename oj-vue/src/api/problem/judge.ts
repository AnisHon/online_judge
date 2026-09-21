import {
    type AjaxResult,
    type failCallback,
    get,
    getWithParams,
    post,
    type successCallback
} from "@/utils/http";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";
import {ElMessage} from "element-plus";

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

}

interface JudgeResponse {
    answers?: Answer[],
    correct: boolean;
    judgeResult: OJResult;
    submitId?: IdType;
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

export interface LogSubmit {
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
    contestId?: IdType;
}

export interface SubmitCaseResult {
    caseIndex?: number;
    status: OJResult;
    score?: number | string;
    time?: number;
    memory?: number;
}

interface TestForm {
    languageId?: IdType;
    stdin?: string;
    code?: string;
    uuid?: string;
}

interface TestResult {
    userId?: IdType
    uuid: string
    judgeResult: OJResult,
    stderr?: string,
    stdout?: string,
}

interface TestStatusRequest {
    uuid: string;
}

async function sendTest(testForm: TestForm): Promise<AjaxResult<void | AjaxResult<void>>> {
    return await post<TestForm, void | AjaxResult<void>>("/problem-api/judge/test", testForm);
}

async function testStatus(uuid: string) {
    const {data} = await getWithParams<TestResult | null, TestStatusRequest>("/problem-api/judge/test-status", {uuid});
    return data;
}

async function getSubmissionCases(id: IdType): Promise<SubmitCaseResult[]> {
    const {data} = await get<SubmitCaseResult[]>(`/problem-api/log/submissions/${encodeURIComponent(String(id))}/cases`);
    return data || [];
}

async function fetchLog(id: IdType, success: successCallback<LogSubmit>) {
    const {data} = await get<LogSubmit, IdType>("/problem-api/log/submissions", id);
    success(data);
    const intervalId = setInterval(async () => {
        const {data} = await get<LogSubmit, IdType>("/problem-api/log/submissions", id);
        success(data);
        if (data.status !== OJResult.COMPILING && data.status !== OJResult.QUEUE && data.status !== OJResult.RUNNING) {
            clearInterval(intervalId);
        }
    }, 1000);
}

/**
 * 提交状态轮询。默认每秒查询一次，返回停止函数供页面卸载时清理。
 */
async function pollSubmission(
    id: IdType,
    onUpdate: (log: LogSubmit) => void,
    interval = 1000
): Promise<() => void> {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const stop = () => {
        stopped = true;
        if (timer) clearTimeout(timer);
    };
    const poll = async () => {
        if (stopped) return;
        try {
            const {data} = await get<LogSubmit, IdType>("/problem-api/log/submissions", id);
            if (data) {
                onUpdate(data);
                if (![OJResult.QUEUE, OJResult.COMPILING, OJResult.RUNNING].includes(data.status)) {
                    stop();
                    return;
                }
            }
            if (!stopped) timer = setTimeout(poll, interval);
        } catch {
            // 网络瞬断时保留轮询，避免用户必须重新提交；错误提示由 HTTP 层统一处理。
            if (!stopped) timer = setTimeout(poll, Math.min(interval * 2, 5000));
        }
    };
    void poll();
    return stop;
}

/** 代码测试结果轮询。uuid 用于隔离同一用户的连续测试，避免读到旧结果。 */
async function pollTestResult(
    uuid: string,
    onUpdate: (result: TestResult) => void,
    interval = 1000
): Promise<() => void> {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const stop = () => {
        stopped = true;
        if (timer) clearTimeout(timer);
    };
    const poll = async () => {
        if (stopped) return;
        try {
            const data = await testStatus(uuid);
            if (data && data.uuid === uuid) {
                onUpdate(data);
                if (data.judgeResult && ![OJResult.QUEUE, OJResult.COMPILING, OJResult.RUNNING].includes(data.judgeResult)) {
                    stop();
                    return;
                }
            }
            if (!stopped) timer = setTimeout(poll, interval);
        } catch {
            if (!stopped) timer = setTimeout(poll, Math.min(interval * 2, 5000));
        }
    };
    void poll();
    return stop;
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
    const {data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge/test", judgeForm, fail);

    return data
}


async function judge(judgeForm: JudgeForm, fail: failCallback): Promise<JudgeResponse> {
    const {data} =
        await post<JudgeForm, JudgeResponse>("/problem-api/judge", judgeForm, fail);

    return data
}

export type {
    Answer,
    JudgeForm,
    JudgeResponse,
    TestResult,
    TestForm,
    JudgeMessage,
    TestStatusRequest
}

export {
    judge,
    getUserAnswer,
    saveUserAnswer,
    debouncedSave,
    fetchLog,
    pollSubmission,
    sendTest,
    testStatus,
    pollTestResult,
    getSubmissionCases,
    OJResult
}
