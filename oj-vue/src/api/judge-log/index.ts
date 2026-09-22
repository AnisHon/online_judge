import {get, getWithParams} from '@/utils/http'
import type {IdType} from '@/api/common'
import type {PagedResponse, PagedType} from '@/api/pagedType'
import {getJudgeStatusMeta, normalizeJudgeStatus} from '@/utils/problem/judgeStatus'

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

const judgeStatusValues = [
  'QUEUE', 'COMPILING', 'RUNNING', 'ACCEPT', 'WRONG_ANSWER',
  'RUNTIME_ERROR', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'COMPILE_ERROR', 'JUDGE_ERROR',
] as const

export const judgeStatusOptions = judgeStatusValues.map(value => ({
  label: getJudgeStatusMeta(value).label,
  value,
  tone: getJudgeStatusMeta(value).tone,
}))

export const judgeStatusMeta = (status?: string | null) => {
  if (!status) return {label: '未知', value: '', tone: 'info'}
  const normalized = normalizeJudgeStatus(status)
  const item = judgeStatusOptions.find(option => normalizeJudgeStatus(option.value) === normalized)
  return item || {
    label: status,
    value: normalized || status,
    tone: 'info',
  }
}
