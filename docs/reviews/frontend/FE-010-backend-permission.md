# FE-010 权限管理 Review

## 范围

- `oj-vue/src/views/backend/user-module/auth-manage/AuthManage.vue`
- `oj-vue/src/api/menu/index.ts`、`oj-vue/src/api/auth/menu.ts`
- `IconPicker`、权限资源树及共享后台壳

## 结论

权限页已经使用树形表格、类型排序、权限字符串和独立图标选择器，方向正确。首轮修复后，本轮继续收口了服务端树完整性、权限读取边界、角色授权代际和展开状态；现有 `user:menu:list` 继续作为兼容的 read scope，`add/edit/remove/grant/revoke` 仍分别控制管理动作。

## 问题清单

### FE-010-P-01 [高][数据完整性] 编辑资源时可以把节点挂到自己的后代，形成父子循环

- 位置：`AuthManage.vue:304-310`。
- `toParentOptions` 只排除了当前节点 `currentId`，没有排除当前节点的整个 descendants。编辑父节点时可以选择自己的子节点作为父级。
- 影响：后端可能拒绝，也可能保存出循环/无法递归的树；之后 `flattenTree/decorateTree/filterTree/expandRows` 可能递归异常、页面卡死或动态路由构建失败。
- 建议：先计算当前节点及全部后代 ID，再从父级候选中排除；后端也必须做环检测，前端校验不能代替服务端约束。

### FE-010-P-02 [高] 资源类型切换后仍提交旧字段，表单规则也没有绑定到条件字段

- 位置：`AuthManage.vue:161-207,441-453`。
- `form` 同时保存 icon/router/component/perms；从按钮切换到菜单或反向切换时，隐藏字段不会自动清空。`rules` 只校验名称、父级、顺序，路由和权限依赖 submit 时的 toast/return。
- 影响：隐藏字段可能被后端保存，资源类型与路由/权限不一致；用户只能看到一条通用提示，无法定位具体字段。
- 建议：类型切换时按 schema 清理不适用字段；建立 `MENU/MENU_ITEM/BUTTON` 三套条件校验，错误挂在对应 `el-form-item`；提交 DTO 时只发送当前类型允许的字段。

### FE-010-P-03 [高] 权限树异步加载和保存没有操作代际，旧响应能覆盖当前角色/当前弹窗

- 位置：`RoleManage.vue:266-332`（权限授权弹窗，属于权限域交互）；`AuthManage.vue:366-400`。
- 角色权限弹窗使用共享的 `form.roleId`、`originalPermissionIds`、`addArray/delArray`。切换角色、关闭后重开或在 1 秒 debounce 窗口内操作时，旧的 `listRoleMenu`/grant/revoke 响应仍可能写回新弹窗状态。
- 建议：每次打开弹窗生成 operation id，所有异步回调先校验 id 和 roleId；保存请求使用不可变 relation 快照，并在关闭时 cancel debounce，不要清空正在发送的数组引用。

### FE-010-P-04 [高] 权限保存的两个请求被当成“全部成功”，部分失败也会关闭弹窗并提示成功

- 位置：`RoleManage.vue:294-330`、`api/auth/menu.ts:65-86`。
- 新增和撤销分别发请求，`finishPermissionRequest` 在每个请求的 `finally` 中递减并显示成功；只要请求失败进入 finally，仍会算作完成并调用 `cancelMenu`。没有记录成功/失败集合，也没有重新读取最终权限。
- 影响：管理员看到“角色权限已更新”，实际只更新了一半；再次打开才发现权限漂移。
- 建议：用 `Promise.allSettled` 或后端一个事务批量接口，只有全部成功才关闭；部分成功时显示失败项、保留弹窗并重新拉取服务端快照。

### FE-010-P-05 [中] `menuTree.length` 作为缓存命中条件，资源变更后授权树永不刷新

- 位置：`RoleManage.vue:286-290`。
- `loadMenuTree` 只要已有一棵树就直接 return。权限资源页新增/编辑/删除后，角色授权弹窗仍使用旧资源，不存在显式 invalidation。
- 建议：权限资源变更后广播 `menuTreeVersion`/调用共享 query cache invalidate；按版本缓存而不是只判断数组长度。

### FE-010-P-06 [中][性能] 全量树、递归过滤和逐行 `toggleRowExpansion` 会在资源多时放大渲染成本

- 位置：`AuthManage.vue:296-400`。
- 每次筛选都递归复制树；展开时对每个节点逐行调用 `toggleRowExpansion`，并在递归层级中 `await nextTick()`。搜索还用 `setTimeout` 延迟展开，未保存 timer，也没有在组件卸载时取消。
- 影响：资源数量变大时搜索卡顿、展开动画堆叠，切页/离开页面后仍可能访问已卸载的 table ref。
- 建议：使用可控的 tree state/虚拟树；仅展开匹配路径，不要全量逐节点 nextTick；用 `onBeforeUnmount` 清理定时器和 pending 操作。

### FE-010-P-07 [中][交互] 筛选后 `expandedAll` 永远设为 true，清除筛选不恢复用户原来的展开状态

- 位置：`AuthManage.vue:380-389`。
- 搜索强制全部展开，清除筛选只清字段并再次调用 `handleQuery`，仍保持 true。用户无法回到“默认折叠”的状态，且筛选项和展开状态耦合。
- 建议：保存用户展开 key 集合；筛选只自动展开匹配路径，清除后恢复原集合；“展开全部”才改变全局展开模式。

### FE-010-P-08 [中][CSS] 树缩进由多个层级机制同时控制，父节点和叶节点对齐规则不一致

- 位置：`AuthManage.vue:358-364,685-719`。
- `row-style` 写入 `--tree-offset`，展开图标又用 `margin-left`，叶节点再由 `resourceCellStyle` 添加 padding；父节点、叶节点和没有图标的节点不是同一套缩进计算。
- 影响：深层资源树容易出现箭头与文字错位、叶节点比父节点多/少缩进、窄屏时资源名称被挤掉。
- 建议：只保留一套 indent 计算，建议把层级包装在资源单元格内部，箭头与文字放在同一 flex 行；用统一的 `--indent-size` token。

### FE-010-P-09 [中][重复/死代码] 权限页保留了旧版本地图标选择器 CSS，同时又使用独立 `IconPicker`

- 位置：`AuthManage.vue:943-1060`、模板 `AuthManage.vue:183-185`。
- 页面已经导入 `IconPicker`，但仍保留 `.icon-picker`、`.icon-grid`、`.icon-tile`、`.icon-preview` 等本地样式，真实 DOM 不使用这些类。
- 影响：维护者会误以为这里还有一套选择器；后续样式重构容易误改共享组件，增加 CSS 体积和认知负担。
- 建议：删除死 CSS，所有图标选择行为和样式只保留在共享 `IconPicker`；权限页只负责表单布局。

### FE-010-P-10 [中][权限模型] 页面、资源读取和资源删除只做按钮过滤，没有显式区分 read/manage scope

- 位置：`AuthManage.vue:11-13,70-72,123-131`、`RoleManage.vue` 权限弹窗。
- 页面是否进入主要依赖动态菜单，读取树/角色权限的请求没有页面级可见状态；当用户能看到页面但没有 `user:menu:list` 或权限读取权限时，失败表现依赖全局 HTTP 错误。
- 建议：为 `user:menu:list/read/add/edit/remove` 明确建模，页面入口和每个 action 使用同一 permission predicate；后端仍是最终鉴权，前端只负责稳定的可用性和错误提示。

## 建议验收用例

1. 编辑根菜单，父级候选不能出现自己和任何后代。
2. 在按钮/菜单项/菜单栏之间来回切换，检查提交 DTO 不包含不适用字段。
3. 同时新增和撤销权限，模拟其中一个请求失败，弹窗不能提示整体成功。
4. 资源页新增菜单后，不刷新浏览器直接打开角色授权，能看到新资源。
5. 5 层以上树在暗色和 360px 宽度下箭头、文字、图标保持同一基线。

## 本轮已执行

- `MenuController.treeMenus` 增加 `user:menu:list` 服务端鉴权，权限页在无 read scope 时不请求资源树并展示明确空状态。
- `SysMenuServiceImpl` 在新增/编辑菜单时统一规范根节点 `parentId=0`，校验父级存在、禁止按钮作为父级，并沿祖先链阻止自挂载和循环关系。
- `AuthManage` 使用按资源类型变化的表单规则和 DTO 白名单，只提交当前类型允许的字段；菜单栏不再被强制要求组件路径，菜单项/按钮分别校验组件和权限标识。
- 权限资源 CRUD 成功后使 `useMenuStore` 的菜单快照失效，但保留当前已注册菜单，避免后台导航瞬间消失。
- 资源树只展开搜索命中的父级路径，保留用户手动展开集合；清除筛选后恢复原状态，并用操作代际和卸载保护阻止旧异步展开写回。
- 统一资源树缩进，移除旧版图标选择器死 CSS；角色授权改用 `user:menu:list/grant/revoke` 判断，不再误用 `user:role:grant/revoke`，部分保存失败时保留弹窗并重新读取服务端快照。
- 删除权限 API 中不再使用的 debounce 包装，避免把可变表单对象延迟提交。

## 本轮验证

- `oj-vue`: `pnpm run type-check` 通过。
- `user-service`: 使用 `/usr/lib/jvm/java-11-temurin-jdk/bin/java` 执行 `mvn -pl user-service -am -DskipTests test-compile` 通过。
- `git diff --check` 通过。

本轮没有新增数据库字段或改变既有权限字符串，因此不需要额外数据库迁移；线上已有角色继续沿用现有菜单权限数据。
