import type {TreedMenu} from '@/api/auth/menu'
import type {IdType} from '@/api/common'

export const isDenyPermission = (permission?: string) =>
  Boolean(permission?.startsWith('policy:') && permission.endsWith(':deny'))

export function findMissingPermissionParent(tree: TreedMenu[], ids: Array<IdType | number | string>): string {
  const selected = new Set(ids.map(String))
  const nodes = new Map<string, {label: string; perms?: string; parentId?: string}>()
  const collect = (items: TreedMenu[], parentId?: string) => {
    items.forEach(item => {
      const id = String(item.id ?? item.menu.menuId)
      nodes.set(id, {label: item.menu.menuName, perms: item.menu.perms, parentId})
      collect(item.children || [], id)
    })
  }
  collect(tree)
  for (const id of selected) {
    const child = nodes.get(id)
    if (!child || isDenyPermission(child.perms)) continue
    let parentId = child.parentId
    const visited = new Set<string>()
    while (parentId && !visited.has(parentId)) {
      visited.add(parentId)
      if (!selected.has(parentId)) {
        return `“${child.label}”权限依赖于“${nodes.get(parentId)?.label || parentId}”权限，请检查父级权限。`
      }
      parentId = nodes.get(parentId)?.parentId
    }
  }
  return ''
}
