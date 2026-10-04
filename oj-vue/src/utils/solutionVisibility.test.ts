import {describe, expect, it} from 'vitest'
import {solutionVisibilityLabel} from './solutionVisibility'

describe('solution effective visibility labels', () => {
  it('uses server authorization outcome, never the private preference', () => {
    const solution = {effectiveVisibility: 'AUTHOR_ONLY' as const, private_: false}
    expect(solutionVisibilityLabel(solution)).toBe('仅本人可见')
    expect(solutionVisibilityLabel({effectiveVisibility: 'PUBLIC'})).toBe('公开')
    expect(solutionVisibilityLabel({effectiveVisibility: 'DELETED'})).toBe('已删除')
  })
  it('does not guess public access for older or incomplete responses', () => {
    expect(solutionVisibilityLabel({})).toBe('未确认')
  })
})
