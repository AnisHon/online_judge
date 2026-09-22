import {
    type AjaxResult,
    type failCallback,
    get,
    getWithParams,
    post
} from "@/utils/http";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";
import {ElMessage} from "element-plus";
import {isPendingJudgeStatus, normalizeJudgeStatus} from "@/utils/problem/judgeStatus";
import {service} from "@/utils/http";

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
    judgeResult: string;
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
    status: string;
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
    status: string;
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
    judgeResult: string,
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
    if (!Array.isArray(data)) throw new Error("测试点结果暂不可用");
    return data;
}

async function fetchLog(id: IdType, onUpdate: (log: LogSubmit) => void): Promise<() => void> {
    return pollSubmission(id, onUpdate);
}

/**
 * 提交状态轮询。默认每秒查询一次，返回停止函数供页面卸载时清理。
 */
async function pollSubmission(
    id: IdType,
    onUpdate: (log: LogSubmit) => void,
    interval = 1000,
    options: {maxDuration?: number; maxFailures?: number} = {},
): Promise<() => void> {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let failures = 0;
    const startedAt = Date.now();
    const controller = new AbortController();
    const maxDuration = options.maxDuration ?? 90_000;
    const maxFailures = options.maxFailures ?? 8;
    const stop = () => {
        stopped = true;
        if (timer) clearTimeout(timer);
        controller.abort();
    };
    const reportTimeout = () => onUpdate({
        submitId: id,
        userId: '',
        problemId: '',
        language: '',
        status: OJResult.JUDGE_ERROR,
        stderr: undefined,
    });
    const poll = async () => {
        if (stopped) return;
        try {
            const result = await service.get<LogSubmit, AjaxResult<LogSubmit>>(`/problem-api/log/submissions/${encodeURIComponent(String(id))}`, {signal: controller.signal});
            if (stopped) return;
            const data = (result as unknown as AjaxResult<LogSubmit>).data;
            failures = 0;
            if (data) {
                const normalized = {...data, status: normalizeJudgeStatus(data.status) || data.status};
                onUpdate(normalized);
                if (!isPendingJudgeStatus(normalized.status)) {
                    stop();
                    return;
                }
            }
            if (Date.now() - startedAt >= maxDuration) {
                reportTimeout();
                stop();
                return;
            }
            if (!stopped) timer = setTimeout(poll, interval);
        } catch (error) {
            if (stopped || (error instanceof DOMException && error.name === 'AbortError')) return;
            failures++;
            if (failures >= maxFailures || Date.now() - startedAt >= maxDuration) {
                reportTimeout();
                stop();
                return;
            }
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
    interval = 1000,
    options: {maxDuration?: number; maxFailures?: number} = {},
): Promise<() => void> {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let failures = 0;
    const startedAt = Date.now();
    const controller = new AbortController();
    const maxDuration = options.maxDuration ?? 45_000;
    const maxFailures = options.maxFailures ?? 8;
    const stop = () => {
        stopped = true;
        if (timer) clearTimeout(timer);
        controller.abort();
    };
    const reportTimeout = () => onUpdate({uuid, judgeResult: OJResult.JUDGE_ERROR, stderr: '测试结果暂时无法获取'});
    const poll = async () => {
        if (stopped) return;
        try {
            const result = await service.get<TestResult | null, AjaxResult<TestResult | null>>('/problem-api/judge/test-status', {params: {uuid}, signal: controller.signal});
            if (stopped) return;
            const data = (result as unknown as AjaxResult<TestResult | null>).data;
            if (data && data.uuid === uuid) {
                const normalized = {...data, judgeResult: normalizeJudgeStatus(data.judgeResult) || data.judgeResult};
                failures = 0;
                onUpdate(normalized);
                if (!isPendingJudgeStatus(normalized.judgeResult)) {
                    stop();
                    return;
                }
            }
            if (Date.now() - startedAt >= maxDuration) {
                reportTimeout();
                stop();
                return;
            }
            if (!stopped) timer = setTimeout(poll, interval);
        } catch (error) {
            if (stopped || (error instanceof DOMException && error.name === 'AbortError')) return;
            failures++;
            if (failures >= maxFailures || Date.now() - startedAt >= maxDuration) {
                reportTimeout();
                stop();
                return;
            }
            if (!stopped) timer = setTimeout(poll, Math.min(interval * 2, 5000));
        }
    };
    void poll();
    return stop;
}

async function getUserAnswer(req: UserAnswerRequest): Promise<UserAnswer> {
    const {data} = await getWithParams<UserAnswer, UserAnswerRequest>("/problem-api/record", req);
    return {
        answers: Array.isArray(data?.answers) ? data.answers.filter(item => item && Number.isFinite(Number(item.index))) : [],
        code: typeof data?.code === 'string' ? data.code : '',
        languageId: data?.languageId || '1',
    };
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
