export const formatJudgeDate = (value?: string | Date) => value ? new Date(value).toLocaleString('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
}) : '刚刚';

/** 判题服务和公开日志接口统一以 MiB 返回内存。 */
export const formatJudgeMemory = (value?: number | null) => value == null ? '-' : `${value} MiB`;
export const formatJudgeTime = (value?: number | null) => value == null ? '-' : `${value} ms`;
