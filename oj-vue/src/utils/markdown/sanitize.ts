import xss from 'xss'

const ALLOWED_URL_PROTOCOLS = new Set(['http:', 'https:', 'mailto:'])

const isSafeUrl = (value: string): boolean => {
  const raw = value.trim()
  if (!raw || raw.startsWith('#') || raw.startsWith('/') || raw.startsWith('./') || raw.startsWith('../')) {
    return true
  }

  try {
    return ALLOWED_URL_PROTOCOLS.has(new URL(raw, window.location.origin).protocol)
  } catch {
    return false
  }
}

/**
 * Markdown 内容是用户可编辑数据，预览前统一走 md-editor-v3 同版本的 xss 清洗器。
 * 只允许常见 Markdown 标签，并额外收紧链接和图片地址协议，禁止任意嵌入内容。
 */
export const sanitizeMarkdownHtml = (html: string): string => xss(html, {
  stripIgnoreTag: true,
  stripIgnoreTagBody: ['script', 'style', 'iframe', 'object', 'embed'],
  onTagAttr: (tag, name, value, isWhiteAttr) => {
    if ((tag === 'a' && name === 'href') || (tag === 'img' && name === 'src')) {
      return isSafeUrl(value) && isWhiteAttr ? undefined : ''
    }
    return undefined
  }
})
