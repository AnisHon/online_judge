export type JudgeStatusTone = 'pending' | 'primary' | 'success' | 'warning' | 'danger' | 'info';

export interface JudgeStatusMeta {
  label: string;
  code: string;
  tone: JudgeStatusTone;
}

export type CanonicalJudgeStatus = 'QUEUE' | 'COMPILING' | 'RUNNING' | 'AC' | 'RE' | 'WA' | 'TLE' | 'MLE' | 'CE' | 'JUDGE_ERROR';

const STATUS_META: Record<string, JudgeStatusMeta> = {
  QUEUE: {label: '排队中', code: 'WAIT', tone: 'pending'},
  COMPILING: {label: '编译中', code: 'BUILD', tone: 'primary'},
  RUNNING: {label: '判题中', code: 'RUN', tone: 'pending'},
  AC: {label: '通过', code: 'AC', tone: 'success'},
  RE: {label: '运行错误', code: 'RE', tone: 'warning'},
  WA: {label: '答案错误', code: 'WA', tone: 'danger'},
  TLE: {label: '时间超限', code: 'TLE', tone: 'info'},
  MLE: {label: '内存超限', code: 'MLE', tone: 'info'},
  CE: {label: '编译错误', code: 'CE', tone: 'warning'},
  JUDGE_ERROR: {label: '判题异常', code: 'JUDGE_ERROR', tone: 'danger'},
};

const STATUS_ALIASES: Record<string, string> = {
  QUEUE: 'QUEUE',
  COMPILING: 'COMPILING',
  RUNNING: 'RUNNING',
  ACCEPT: 'AC',
  ACCEPTED: 'AC',
  AC: 'AC',
  RUNTIME_ERROR: 'RE',
  RUNTIMEERROR: 'RE',
  RE: 'RE',
  WRONG_ANSWER: 'WA',
  WRONGANSWER: 'WA',
  WA: 'WA',
  TIME_LIMIT_EXCEEDED: 'TLE',
  TIMELIMITEXCEEDED: 'TLE',
  TLE: 'TLE',
  MEMORY_LIMIT_EXCEEDED: 'MLE',
  MEMORYLIMITEXCEEDED: 'MLE',
  MLE: 'MLE',
  COMPILE_ERROR: 'CE',
  COMPILEERROR: 'CE',
  CE: 'CE',
  JUDGE_ERROR: 'JUDGE_ERROR',
  JUDGEERROR: 'JUDGE_ERROR',
};

export function normalizeJudgeStatus(status?: string): CanonicalJudgeStatus | '' {
  const raw = String(status || '').trim().toUpperCase();
  return (STATUS_ALIASES[raw] || raw) as CanonicalJudgeStatus | '';
}

export function getJudgeStatusMeta(status?: string): JudgeStatusMeta {
  const raw = String(status || '').trim();
  const key = normalizeJudgeStatus(raw);
  return STATUS_META[key] || {label: raw || '未知状态', code: raw || 'UNKNOWN', tone: 'danger'};
}

export function isPendingJudgeStatus(status?: string) {
  const key = normalizeJudgeStatus(status);
  return key === 'QUEUE' || key === 'COMPILING' || key === 'RUNNING';
}

export function isTerminalJudgeStatus(status?: string) {
  return !!normalizeJudgeStatus(status) && !isPendingJudgeStatus(status);
}
