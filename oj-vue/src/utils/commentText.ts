export const COMMENT_EMOJIS = ['🙂', '😄', '😂', '😅', '😍', '🤔', '👍', '👎', '🎉', '❤️', '😢', '😡'] as const
export const COMMENT_MAX_CODE_POINTS = 2000
export const COMMENT_MAX_UTF8_BYTES = 8192

const isJavaWhitespace = (codePoint: number): boolean =>
  (codePoint >= 0x0009 && codePoint <= 0x000D)
  || (codePoint >= 0x001C && codePoint <= 0x0020)
  || codePoint === 0x00A0 || codePoint === 0x1680
  || (codePoint >= 0x2000 && codePoint <= 0x200A)
  || codePoint === 0x2028 || codePoint === 0x2029 || codePoint === 0x202F
  || codePoint === 0x205F || codePoint === 0x3000

const trimUnicodeWhitespace = (value: string): string => {
  let start = 0
  let end = value.length
  while (start < end) {
    const codePoint = value.codePointAt(start)!
    if (!isJavaWhitespace(codePoint)) break
    start += codePoint > 0xFFFF ? 2 : 1
  }
  while (start < end) {
    const codePoint = value.codePointAt(end - 1)!
    const width = codePoint >= 0xDC00 && codePoint <= 0xDFFF ? 2 : 1
    const actual = width === 2 ? value.codePointAt(end - 2)! : codePoint
    if (!isJavaWhitespace(actual)) break
    end -= width
  }
  return value.slice(start, end)
}

export const normalizeCommentText = (value: string): string =>
  trimUnicodeWhitespace(value.replace(/\r\n/g, '\n').normalize('NFC'))

const hasInvalidSurrogate = (value: string): boolean => {
  for (let index = 0; index < value.length; index++) {
    const code = value.charCodeAt(index)
    if (code >= 0xD800 && code <= 0xDBFF) {
      const next = value.charCodeAt(index + 1)
      if (!(next >= 0xDC00 && next <= 0xDFFF)) return true
      index++
    } else if (code >= 0xDC00 && code <= 0xDFFF) {
      return true
    }
  }
  return false
}

export const commentTextError = (raw: string): string => {
  const value = normalizeCommentText(raw)
  if (!value) return '请输入评论内容'
  if (hasInvalidSurrogate(value)) return '评论包含无效字符'
  if (value.includes('\0')) return '评论包含无效字符'
  if (Array.from(value).length > COMMENT_MAX_CODE_POINTS) return '评论不能超过 2000 个字符'
  if (new TextEncoder().encode(value).length > COMMENT_MAX_UTF8_BYTES) return '评论内容过长，请精简后重试'
  if (/[\u0000-\u0008\u000B-\u001F\u007F-\u009F]/.test(value)) return '评论包含不支持的控制字符'
  return ''
}

export const createCommentRequestId = (): string => {
  if (typeof globalThis.crypto?.randomUUID === 'function') return globalThis.crypto.randomUUID()
  const bytes = new Uint8Array(16)
  if (typeof globalThis.crypto?.getRandomValues === 'function') globalThis.crypto.getRandomValues(bytes)
  else for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export interface CommentRequestIdentity {
  begin(content: string): string
  edit(content: string): void
  complete(): void
  current(): string
}

export const createCommentRequestIdentity = (newId = createCommentRequestId): CommentRequestIdentity => {
  let id = newId()
  let attemptedContent: string | null = null
  return {
    begin(content) {
      if (attemptedContent !== null && attemptedContent !== content) {
        id = newId()
        attemptedContent = null
      }
      attemptedContent = content
      return id
    },
    edit(content) {
      if (attemptedContent !== null && attemptedContent !== content) {
        id = newId()
        attemptedContent = null
      }
    },
    complete() {
      id = newId()
      attemptedContent = null
    },
    current: () => id,
  }
}

export const commentUiError = (error: unknown): string => {
  const candidate = error as {code?: unknown; status?: unknown}
  const code = typeof candidate?.code === 'number' ? candidate.code
    : typeof candidate?.status === 'number' ? candidate.status : 0
  if (code === 400) return '评论内容不符合要求，请检查后重试'
  if (code === 401) return '登录状态已过期，请重新登录'
  if (code === 403) return '当前账号不能执行此操作'
  if (code === 404) return '内容已不可访问，请刷新后重试'
  if (code === 409) return '内容状态已变化或评论区已关闭，请刷新后重试'
  if (code === 429) return '操作过于频繁，请稍后再试'
  return '操作暂时未完成，请稍后重试'
}
