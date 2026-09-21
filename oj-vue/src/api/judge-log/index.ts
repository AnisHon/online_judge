import {get, getWithParams} from '@/utils/http'
import type {IdType} from '@/api/common'
import type {PagedResponse, PagedType} from '@/api/pagedType'

export interface AdminJudgeQuery extends PagedType {
  submitId?: string
  userId?: string
  problemId?: string
  contestId?: string
  caseId?: string
  caseIndex?: number
  status?: string
  language?: string
}

export interface AdminSubmitLog {
  submitId: IdType
  userId: IdType
  problemId: IdType
  contestId?: IdType | null
  language?: string
  code?: string | null
  status?: string | null
  stderr?: string | null
  internalError?: string | null
  errorCode?: string | null
  time?: number | null
  memory?: number | null
  totalCount?: number | null
  passCount?: number | null
  submitTime?: string | Date
}

export interface AdminJudgeCaseLog {
  id: IdType
  submitId: IdType
  problemId: IdType
  caseId?: IdType | null
  caseIndex?: number | null
  status?: string | null
  score?: number | string | null
  time?: number | null
  memory?: number | null
  internalError?: string | null
  createTime?: string | Date
  updateTime?: string | Date
}

export const getAdminSubmitLogs = async (query: AdminJudgeQuery) => {
  const {data} = await getWithParams<PagedResponse<AdminSubmitLog>, AdminJudgeQuery>(
    '/problem-api/log/admin/submissions',
    query,
  )
  return data
}

export const getAdminSubmitLog = async (submitId: IdType) => {
  const {data} = await get<AdminSubmitLog, IdType>('/problem-api/log/admin', submitId)
  return data
}

export const getAdminCaseLogs = async (query: AdminJudgeQuery) => {
  const {data} = await getWithParams<PagedResponse<AdminJudgeCaseLog>, AdminJudgeQuery>(
    '/problem-api/log/admin/cases',
    query,
  )
  return data
}

export const getAdminCaseLogsBySubmit = async (submitId: IdType) => {
  const {data} = await get<AdminJudgeCaseLog[]>(
    `/problem-api/log/admin/${encodeURIComponent(String(submitId))}/cases`,
  )
  return data
}

export const judgeStatusOptions = [
  {label: '排队中', value: 'QUEUE', tone: 'info'},
  {label: '编译中', value: 'COMPILING', tone: 'info'},
  {label: '运行中', value: 'RUNNING', tone: 'warning'},
  {label: '通过', value: 'ACCEPT', tone: 'success'},
  {label: '答案错误', value: 'WRONG_ANSWER', tone: 'danger'},
  {label: '运行错误', value: 'RUNTIME_ERROR', tone: 'warning'},
  {label: '超时', value: 'TIME_LIMIT_EXCEEDED', tone: 'warning'},
  {label: '内存超限', value: 'MEMORY_LIMIT_EXCEEDED', tone: 'warning'},
  {label: '编译错误', value: 'COMPILE_ERROR', tone: 'danger'},
  {label: '判题异常', value: 'JUDGE_ERROR', tone: 'danger'},
] as const

const statusMap = new Map(judgeStatusOptions.map(item => [item.value, item]))

export const judgeStatusMeta = (status?: string | null) => {
  if (!status) return {label: '未知', value: '', tone: 'info'}
  const normalized = status.toUpperCase()
  return statusMap.get(normalized as typeof judgeStatusOptions[number]['value']) || {
    label: status,
    value: normalized,
    tone: 'info',
  }
}
