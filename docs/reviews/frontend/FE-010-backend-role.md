# FE-010 角色管理 Review

## 范围

- `oj-vue/src/views/backend/user-module/role-manage/RoleManage.vue`
- `oj-vue/src/views/backend/user-module/role-manage/RoleAuth.vue`
- `oj-vue/src/api/role/index.ts`、`oj-vue/src/api/auth/menu.ts`

## 结论

角色页的权限过滤方向基本正确，角色 CRUD、角色成员和角色资源授权也已经拆成不同入口。但角色编辑和授权仍共享大量可变状态，且“刷新角色缓存”“批量授权”“成员跨页选择”没有明确的一致性模型。这里的问题一旦出现，通常不是当前页面立刻报错，而是角色已经部分生效、前端仍显示旧状态。

## 问题清单

### FE-010-R-01 [高] 角色权限保存存在部分成功却提示整体成功的问题

- 位置：`RoleManage.vue:294-332`、`api/auth/menu.ts:65-86`。
- 新增权限和撤销权限各自使用一个 debounced API。两个请求的 `finally` 都进入 `finishPermissionRequest`，失败也会递减计数并显示成功、关闭弹窗、清空本地快照。
- 影响：角色权限可能只新增了一部分或只撤销了一部分，管理员无法从页面知道失败边界。
- 建议：优先提供后端事务型“设置角色权限快照”接口；若暂时保留双请求，使用 `Promise.allSettled`，失败时保留弹窗、展示失败方向并重新查询服务端权限。

### FE-010-R-02 [高] grant/revoke debounce 捕获数组引用，取消操作会修改正在排队的 payload

- 位置：`RoleManage.vue:271-302,320-332`。
- `debouncedGrant(addArray.value, ...)` 和 `debouncedRevoke(delArray.value, ...)` 保存的是数组引用；`cancelMenu()` 直接把数组清空。关闭弹窗或打开另一个角色时，尚未执行的请求可能提交空数组或新角色的关系。
- 建议：debounce 函数接受 `() => relationsSnapshot` 或每次调用传入 `relations.map(...)`，并提供 `.cancel()`；请求中不要复用可变数组。

### FE-010-R-03 [高] 角色权限弹窗的 roleId 是共享表单字段，快速切换角色可能写错角色

- 位置：`RoleManage.vue:304-317,320-330`。
- `handleMenu` 先写 `form.roleId`，再异步加载菜单和角色权限；如果旧请求晚于新请求完成，它会把旧角色的选中状态写入当前树。提交时又从共享 `form.roleId` 取目标角色。
- 建议：弹窗内部使用不可变 `permissionSession = {roleId, requestId}`；加载、勾选、保存都绑定 session，切换/关闭立即使旧 session 失效。

### FE-010-R-04 [中] 角色资源树只初始化一次，菜单资源更新后角色授权页面过期

- 位置：`RoleManage.vue:286-290`。
- 通过 `menuTree.length` 做永久缓存。权限管理新增、删除或重命名资源后，当前页面不刷新浏览器就看不到新资源。
- 建议：菜单资源树放到可失效的共享 query store，资源 CRUD 成功后清除版本；弹窗打开时至少校验资源版本。

### FE-010-R-05 [中] 角色缓存刷新失败也继续刷新列表，用户无法区分缓存刷新是否成功

- 位置：`api/role/index.ts:59-63`、`RoleManage.vue:221`。
- `refreshRoleCache` 在 API 层 catch 后只发 warning，不抛出失败；组件随后无条件 `getList()`。UI 看起来完成了“刷新缓存 + 刷新列表”，但缓存操作失败没有失败状态。
- 建议：API 层返回结构化结果或抛出错误；组件分别展示“缓存刷新失败”和“列表刷新成功”，不要用普通通知掩盖运维动作失败。

### FE-010-R-06 [中] RoleAuth 读取角色信息失败后仍允许管理成员

- 位置：`RoleAuth.vue:117-123`。
- `loadRole` catch 后保留 `角色 ${roleId}` 的 fallback，并继续执行 `getList()`。如果 roleId 不存在、无权读取或路由参数错误，用户仍可能看到一个可提交的成员管理页。
- 建议：角色详情加载失败时进入明确的 not-found/forbidden 状态，禁用 grant/revoke；不要用展示占位名掩盖上下文未确认。

### FE-010-R-07 [中] RoleAuth 的 `reserve-selection` 与分页/查询清理逻辑互相矛盾

- 位置：`RoleAuth.vue:48-60,105-115`。
- 表格启用 `reserve-selection`，回调却在每次列表加载后把 `selectedIds` 设为空，也没有调用 `clearSelection`。跨页保留选择与“刷新后清空”两种语义同时存在。
- 建议：明确批量撤销只针对当前页，去掉 `reserve-selection`；或者维护跨页 selection，并在查询条件变化时给用户一个可见的清空/保留策略。

### FE-010-R-08 [中] 角色成员 grant 也使用延迟可变数组，关闭弹窗可能仍提交

- 位置：`RoleAuth.vue:142-150`、`api/role/index.ts:117-125`。
- `addForm` 是 reactive 数组，`debouncedGrant` 保存其引用；`cancel()` 会清空 `grantSelectedIds`，成功回调和关闭时机又依赖 debounce。
- 建议：成员分配不需要人为 1 秒延迟，按钮 loading 已经足够防重复；若保留防抖，应在 submit 时复制 relation 数组并允许取消。

### FE-010-R-09 [中][权限] “更多”入口按 OR 显示，但实际子操作权限更细，容易出现空菜单或错误引导

- 位置：`RoleManage.vue:194-197` 及模板更多菜单。
- `canUseMore` 只要拥有角色授权、撤销或用户列表任一权限就显示更多入口；子项再分别判断。对于只具备用户列表权限的账号，可能打开一个几乎没有可操作项的菜单；只具备 grant/revoke 的账号也不应看到成员列表之外的无关入口。
- 建议：以可见子项数组驱动 dropdown，先按每项权限过滤，再在数组非空时渲染菜单；不要用宽泛的 OR 作为 UI 入口。

### FE-010-R-10 [中][CSS] 角色页、成员页和权限页复制同一套紫色主题与 panel 样式

- 位置：`RoleManage.vue:338+`、`RoleAuth.vue:156+`、`AuthManage.vue:497+`。
- 角色头像、状态 pill、filter panel、dialog intro、tree/permission panel 互相复制，且一部分用硬编码 `#8b5cf6/#d97706`，一部分用 Element Plus CSS 变量。
- 影响：暗色模式下对比度和 hover 行为不一致；调整管理后台整体气质需要修改多个大文件。
- 建议：抽取 `AdminRoleCell`、`AdminFilterPanel`、`AdminPermissionDialog` 和颜色 token，页面文件只保留业务数据和 slot。

## 建议验收用例

1. 打开角色 A 授权，立即切换角色 B，再快速保存，确认只写 B。
2. 模拟新增权限请求失败、撤销请求成功，确认 UI 不提示整体成功且服务端结果可重新加载。
3. 权限资源页新增资源后，不刷新浏览器打开角色授权，资源树能更新。
4. 角色详情接口失败时，页面不能提交成员关系。
5. 20 条分页成员跨页勾选、筛选、刷新后，批量撤销范围与界面提示一致。

## 本次修复记录（2026-09-22）

- `RoleManage` 的角色权限保存使用 `Promise.allSettled`；任一新增/撤销请求失败时保留授权弹窗、重新读取服务端权限快照，不再把部分成功提示为整体成功。
- 角色授权和成员授权的请求数据在提交时复制；角色 API 中保留的防抖方法也会复制表单/查询/关系快照，并提供 `cancel()`，避免关闭弹窗或修改响应式数组后污染排队请求。
- 角色权限弹窗通过 session 与 roleId 校验异步响应，切换角色或关闭弹窗后旧请求不能写入当前角色。
- 角色资源树在每次打开授权弹窗时重新读取，资源 CRUD 成功后继续失效菜单缓存；不再依赖页面首次加载时的永久树缓存。
- 刷新角色缓存改为由页面统一处理成功/失败状态：缓存刷新失败不会继续刷新角色列表，也不会出现未处理的异步异常；列表刷新失败会单独提示。
- `RoleAuth` 在角色信息加载成功前禁止查询、分配、撤销和刷新；角色不存在或详情请求失败时显示明确错误状态，成员查询也会等待角色上下文确认。
- 成员表不使用 `reserve-selection`，每次查询/分页刷新都会清空当前页选择；批量撤销在确认前复制用户 ID，操作范围与提示保持一致。
- “更多”菜单改为由实际可见权限项生成，没有可执行子项时不渲染入口。
- 角色、成员、资源页挂载共享 `admin-role-surface` token，紫色主题的关键状态色改为跟随 Element Plus 主色和暗色主题变量。

## 验证记录

- `pnpm run type-check`（`oj-vue`）通过。
- 待完成：使用 Java 11 对 `user-service` 执行 Maven 编译，并运行前端生产构建。
