import {describe, expect, it} from 'vitest'
import {sanitizeMarkdownHtml} from '@/utils/markdown/sanitize'

describe('sanitizeMarkdownHtml', () => {
  it('keeps md-editor code, Mermaid and KaTeX markers', () => {
    const html = sanitizeMarkdownHtml(`
      <details class="md-editor-code" data-closed="false">
        <summary class="md-editor-code-head">JavaScript</summary>
        <pre><code class="language-js"><span class="md-editor-code-block">const answer = 42</span></code></pre>
      </details>
      <p class="md-editor-mermaid" data-processed>
        <svg viewBox="0 0 10 10"><path d="M0 0L10 10" /></svg>
      </p>
      <span class="md-editor-katex-inline" data-processed>
        <span class="katex"><math><semantics><mrow><mi>x</mi></mrow></semantics></math></span>
      </span>
    `)

    expect(html).toContain('class="md-editor-code"')
    expect(html).toContain('class="md-editor-code-block"')
    expect(html).toContain('class="md-editor-mermaid"')
    expect(html).toContain('data-processed')
    expect(html).toContain('<svg')
    expect(html).toContain('<path')
    expect(html).toContain('class="md-editor-katex-inline"')
    expect(html).toContain('<math>')
  })

  it('still strips executable HTML and unsafe URLs', () => {
    const html = sanitizeMarkdownHtml(`
      <script>alert('xss')</script>
      <img src="javascript:alert('xss')" onerror="alert('xss')">
      <span onclick="alert('xss')">safe text</span>
    `)

    expect(html).not.toContain('<script')
    expect(html).not.toContain('javascript:')
    expect(html).not.toContain('onerror')
    expect(html).not.toContain('onclick')
    expect(html).toContain('safe text')
  })
})
