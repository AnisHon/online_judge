import {describe, expect, it} from 'vitest'
import workbenchSource from './OjWorkbench.vue?raw'
import activitySource from '@/views/contest/ContestProblems/ContestProblems.vue?raw'

// Both scoped styles participate in this cascade. Browser geometry checks supplement these guards.
describe('activity workbench fullscreen height isolation', () => {
  it('limits the component parent-fill rule to non-fullscreen workbenches', () => {
    expect(workbenchSource).toMatch(/\.oj-workbench--activity:not\(\.oj-workbench--fullscreen\)\s*\{\s*height:\s*100%;\s*max-height:\s*100%;/)
    expect(workbenchSource).not.toMatch(/\.oj-workbench--activity\s*\{/)
  })

  it('also excludes fullscreen from the activity page deep parent-fill rule', () => {
    expect(activitySource).toContain('.problem-detail-shell :deep(> .oj-workbench:not(.oj-workbench--fullscreen))')
    expect(activitySource).not.toContain('.problem-detail-shell :deep(> .oj-workbench)')
  })
})
