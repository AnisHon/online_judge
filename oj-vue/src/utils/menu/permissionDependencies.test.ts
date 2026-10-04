import {describe, expect, it} from 'vitest'
import type {TreedMenu} from '@/api/auth/menu'
import {findMissingPermissionParent} from './permissionDependencies'

const tree = [{id: '20', menu: {menuId: '20', menuName: '用户管理'}, children: [
  {id: '206', menu: {menuId: '206', menuName: '重置头像', perms: 'user:user:reset-avatar'}},
  {id: '1202', menu: {menuId: '1202', menuName: '禁止头像', perms: 'policy:avatar:deny'}},
]}] as TreedMenu[]

describe('role permission dependencies', () => {
  it('allows a deny permission without any parent route even for legacy nesting', () => {
    expect(findMissingPermissionParent(tree, ['1202'])).toBe('')
  })
  it('retains parent requirements for positive management permissions', () => {
    expect(findMissingPermissionParent(tree, ['206'])).toContain('用户管理')
    expect(findMissingPermissionParent(tree, ['20', '206', '1202'])).toBe('')
  })
})
