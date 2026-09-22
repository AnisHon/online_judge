import xss, {getDefaultWhiteList} from 'xss'

const ALLOWED_URL_PROTOCOLS = new Set(['http:', 'https:', 'mailto:'])

/**
 * md-editor-v3 会在 markdown-it 的结果上继续做一轮增强：
 *
 * - 代码块依赖 `md-editor-*` class 和 data 属性；
 * - KaTeX 依赖 class、style、MathML 标签；
 * - Mermaid 依赖 `.md-editor-mermaid[data-processed]` 容器，之后再把 SVG 插入其中。
 *
 * xss 的默认白名单没有这些属性/标签。之前直接使用默认白名单，等于把
 * md-editor 自己生成的 DOM 清掉了，最终表现就是“语法还在，但功能和样式都没了”。
 */
const createMarkdownWhiteList = () => {
  const whiteList = getDefaultWhiteList()

  const appendAttributes = (tag: string, attributes: string[]) => {
    whiteList[tag] = [...new Set([...(whiteList[tag] || []), ...attributes])]
  }

  // 主题、代码折叠、任务列表和公式都需要 class；style 由 xss 的 CSS 过滤器继续检查。
  Object.keys(whiteList).forEach(tag => appendAttributes(tag, ['class', 'id']))
  ;['div', 'p', 'span', 'code', 'pre', 'details', 'summary', 'label', 'input'].forEach(tag => {
    appendAttributes(tag, ['style'])
  })
  appendAttributes('label', ['for'])
  appendAttributes('input', ['type', 'name', 'value', 'checked', 'disabled'])

  // md-editor-v3 运行时使用的属性。只添加明确的属性名，不放开任意 on* 事件属性。
  ;['div', 'p', 'span', 'code', 'pre', 'details', 'summary', 'input'].forEach(tag => {
    appendAttributes(tag, [
      'data-processed',
      'data-line',
      'data-closed',
      'data-grab',
      'data-is-icon',
      'data-tips',
      'aria-hidden',
      'aria-label',
      'role'
    ])
  })

  // Mermaid 输出的 SVG 可能在 sanitizeMermaid 之后才插入，也可能作为已有 HTML 返回。
  // 这些属性都是几何/绘制属性，不包含脚本执行能力。
  const svgTags = [
    'svg', 'g', 'defs', 'marker', 'path', 'line', 'polyline', 'polygon', 'circle', 'ellipse',
    'rect', 'text', 'tspan', 'title', 'desc', 'clippath', 'mask', 'pattern', 'lineargradient',
    'radialgradient', 'stop', 'foreignobject'
  ]
  const svgAttributes = [
    'class', 'id', 'style', 'xmlns', 'viewBox', 'width', 'height', 'x', 'y', 'x1', 'x2', 'y1', 'y2',
    'cx', 'cy', 'r', 'rx', 'ry', 'd', 'points', 'fill', 'fill-opacity', 'stroke', 'stroke-width',
    'stroke-opacity', 'stroke-linecap', 'stroke-linejoin', 'stroke-dasharray', 'stroke-dashoffset',
    'transform', 'opacity', 'preserveAspectRatio', 'focusable', 'marker-end', 'marker-start',
    'marker-mid', 'clip-path', 'font-family', 'font-size', 'text-anchor', 'dominant-baseline',
    'offset', 'stop-color', 'stop-opacity', 'refX', 'refY', 'orient', 'markerWidth', 'markerHeight',
    'aria-hidden', 'role'
  ]
  svgTags.forEach(tag => appendAttributes(tag, svgAttributes))

  // KaTeX 输出的是 HTML + MathML 混合结构，不能只放开 math 一个标签。
  const mathTags = [
    'math', 'semantics', 'mrow', 'mi', 'mo', 'mn', 'ms', 'mtext', 'annotation', 'annotation-xml',
    'mfrac', 'msqrt', 'mroot', 'mstyle', 'mspace', 'mtable', 'mtr', 'mtd', 'munderover', 'munder',
    'mover', 'mpadded', 'mphantom', 'mprescripts', 'none'
  ]
  const mathAttributes = [
    'class', 'id', 'style', 'xmlns', 'encoding', 'mathvariant', 'displaystyle', 'scriptlevel', 'display',
    'columnalign', 'rowalign', 'columnspan', 'rowspan', 'width', 'height', 'depth', 'lspace', 'rspace',
    'accent', 'stretchy', 'fence', 'separator', 'symmetric', 'movablelimits', 'largeop', 'linebreak',
    'form', 'aria-hidden', 'role'
  ]
  mathTags.forEach(tag => appendAttributes(tag, mathAttributes))

  return whiteList
}

const markdownWhiteList = createMarkdownWhiteList()

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
  whiteList: markdownWhiteList,
  stripIgnoreTag: true,
  stripIgnoreTagBody: ['script', 'style', 'iframe', 'object', 'embed'],
  onTagAttr: (tag, name, value, isWhiteAttr) => {
    if ((tag === 'a' && name === 'href') || (tag === 'img' && name === 'src')) {
      return isSafeUrl(value) && isWhiteAttr ? undefined : ''
    }
    return undefined
  }
})
