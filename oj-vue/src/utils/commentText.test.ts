import {describe, expect, it} from 'vitest'
import {COMMENT_EMOJIS, COMMENT_MAX_CODE_POINTS, commentTextError, normalizeCommentText} from './commentText'

describe('plain comment text', () => {
  it('normalizes line endings, NFC, and Java-compatible edge whitespace', () => {
    expect(normalizeCommentText('\u3000e\u0301\r\nsecond\n\t')).toBe('é\nsecond')
    expect(normalizeCommentText('\uFEFFtext\uFEFF')).toBe('\uFEFFtext\uFEFF')
  })

  it('enforces Unicode code points and UTF-8 byte limits', () => {
    expect(commentTextError('🙂'.repeat(COMMENT_MAX_CODE_POINTS))).toBe('')
    expect(commentTextError('🙂'.repeat(COMMENT_MAX_CODE_POINTS + 1))).toContain('2000')
    expect(commentTextError('界'.repeat(2000))).toBe('')
    expect(commentTextError('界'.repeat(2001))).toContain('2000')
    expect(commentTextError('🙂'.repeat(2000))).toBe('')
    expect(commentTextError('é'.repeat(2000))).toBe('')
  })

  it('rejects empty text, invalid surrogate pairs, NUL, CR, and C1 controls', () => {
    expect(commentTextError(' \u3000\n')).toBe('请输入评论内容')
    expect(commentTextError('\uD800')).toBe('评论包含无效字符')
    expect(commentTextError('\uDC00')).toBe('评论包含无效字符')
    expect(commentTextError('a\0b')).toContain('无效字符')
    expect(commentTextError('a\rb')).toContain('控制字符')
    expect(commentTextError('a\u0085b')).toContain('控制字符')
  })

  it('keeps HTML, Markdown, and URL-looking strings as ordinary text and exposes only the fixed emoji set', () => {
    const literal = '<script>alert(1)</script> ![x](https://example.test/a.png) `code` https://example.test'
    expect(normalizeCommentText(literal)).toBe(literal)
    expect(COMMENT_EMOJIS).toEqual(['🙂', '😄', '😂', '😅', '😍', '🤔', '👍', '👎', '🎉', '❤️', '😢', '😡'])
  })
})
