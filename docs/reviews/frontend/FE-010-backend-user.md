# FE-010 用户模块 Review

## 范围

- `oj-vue/src/views/backend/user-module/user-manage/UserManage.vue`
- `oj-vue/src/views/backend/user-module/class-manage/ClassManage.vue`
- `oj-vue/src/views/backend/user-module/class-manage/user/StudentManage.vue`
- `oj-vue/src/api/user/index.ts`、`oj-vue/src/api/class/index.ts`

## 结论

用户模块的主要风险集中在“延迟操作读取可变表单”“班级成员全量加载”“选择状态与表格视觉状态脱节”和重复 CSS/请求模板。普通 CRUD 路径基本清楚，但数据量增大或用户快速操作时容易出现错写、旧列表覆盖新列表、看起来已经取消但请求仍执行等问题。

## 问题清单

### FE-010-U-01 [高] debounce 持有 reactive 表单引用，关闭/切换对象后仍可能提交新状态

- 位置：`UserManage.vue:367-436`、`api/user/index.ts:131-154`；同样存在于 `ClassManage.vue` 与 `api/class/index.ts:38-62`。
- `debouncedAddUser(editorForm, ...)`、`debouncedUpdateUser(updateForm, ...)` 在初始化时保存表单对象，真正执行发生在 1 秒后。`cancel/resetEditor/finishDialog` 会在这 1 秒内清空或复用对象。
- 影响：用户点击保存后马上关闭弹窗、再次新建或编辑另一个用户时，延迟请求可能提交空表单、后一条记录或错误的 `userId`。loading 也可能对应不上实际请求。
- 建议：提交时先校验，再创建 `structuredClone`/显式 DTO 快照；debounce 只接收快照。弹窗关闭时取消 pending timer，API 层不要吞掉取消和失败。

### FE-010-U-02 [高] 用户状态查询把所有未知值静默映射成 NORMAL

- 位置：`oj-vue/src/api/user/index.ts:157-164`。
- 只要传入的 status 不是 `BANNED`，就被转换成 `NORMAL`。未来新增枚举、后端返回数字格式异常或 URL 参数被污染时，查询结果会悄悄变成“正常用户”，而不是报参数错误。
- 建议：显式映射 `NORMAL/BANNED`，未知值返回 `undefined` 或抛出可识别的参数错误；组件层用枚举值，不要让 API 层用“除封禁之外都正常”的兜底。

### FE-010-U-03 [中] 角色下拉固定拉前 200 条，且新用户默认角色硬编码为 `1`

- 位置：`UserManage.vue:374,496-508`。
- 角色选择依赖一次 `pageSize: 200`，超过 200 个角色时无法选择；`resetEditor` 把默认角色写死为字符串 `'1'`，不保证该角色存在、可用或属于当前管理员可分配范围。
- 建议：使用带搜索/分页的角色选择器；默认角色由后端返回的系统策略决定，或从可分配角色中选择第一个并显式提示。

### FE-010-U-04 [中] 查询完成只清空 `selectedIds`，没有清理 `el-table` 的视觉/保留选择

- 位置：`UserManage.vue:339-344`、`ClassManage.vue` 列表回调、`RoleAuth.vue:105`。
- 表格选择是 Element Plus 内部状态，组件只清空自己的 ID 数组。筛选、分页、刷新后，复选框可能仍显示选中，但批量按钮使用的是新的空数组；如果启用 `reserve-selection`，跨页选择还可能保留旧记录。
- 建议：给表格 ref，在数据切换后 `clearSelection()`；若需要跨页选择，单独实现 selection store，明确“当前页选择”和“全局选择”两种语义，不要依赖默认行为。

### FE-010-U-05 [高] 班级学生页捕获一次 `classId`，路由复用时会继续请求旧班级

- 位置：`StudentManage.vue:67-88`。
- `const classId = route.params.classId` 只执行一次，没有监听 `route.params.classId`。当 Vue Router 复用同一个组件实例切换班级时，标题/路由可能已经是新班级，但列表、添加和移除仍操作旧班级。
- 建议：使用 `computed(() => route.params.classId)`，监听参数变化并取消旧请求；所有 action 从当前 computed 快照读取 ID。

### FE-010-U-06 [中] 班级成员接口全量返回，页面没有分页、上限或增量策略

- 位置：`api/class/index.ts:78-90`、`StudentManage.vue:84-94`。
- `getUserByClass` 返回整个班级成员数组，学生页直接渲染；班级规模变大后，首屏响应、Vue diff、表格布局和选择操作都会线性变慢。竞赛参与人页也有同样模式，应统一治理。
- 建议：后端提供分页/关键词查询，前端使用 `PagedResponse`；添加用户时按搜索结果提交，禁止把全部成员一次性塞进弹窗。

### FE-010-U-07 [中] 学生批量移除没有独立 loading，操作期间仍可重复提交

- 位置：`StudentManage.vue:99-112`。
- 只有单行移除设置 `removingId`；批量移除没有全局状态，批量按钮只绑定 `isLoading`（列表 loading）。请求尚未结束时可以再次点击批量移除或打开添加弹窗。
- 建议：区分 `listLoading/actionLoading/submitLoading`，所有 destructive action 用统一状态锁；批量请求完成后再清理 selection。

### FE-010-U-08 [中] 班级/用户/角色页面重复实现同一套 panel/filter/table CSS

- 位置：`UserManage.vue:511+`、`ClassManage.vue`、`StudentManage.vue`、`RoleAuth.vue:156+`。
- `.panel-heading`、`.filter-panel`、`.list-toolbar`、`.selection-status`、`.user-cell`、`.status-pill`、固定行高和移动端隐藏固定列被多份复制，颜色和间距已经出现不同版本。
- 影响：修复一个页面的响应式或暗色问题不会同步其它页面，且页面组合时容易出现内边距、高度和操作列不一致。
- 建议：抽取管理页面基础组件和 CSS token；页面只提供列配置、权限和业务 slot。

### FE-010-U-09 [中][CSS] 小屏直接隐藏固定操作列，用户失去编辑/移除入口

- 位置：`UserManage.vue`、`ClassManage.vue`、`StudentManage.vue`、`RoleAuth.vue` 的 `@media` 规则。
- 在约 440/480/760px 以下通过 `display:none` 隐藏 `.el-table__fixed-right`。这不是降级，而是把核心功能从页面上删掉；同时表格主体仍可能横向溢出。
- 建议：小屏改为行点击打开详情抽屉、把操作收进 `更多` 菜单，或允许表格横向滚动；不要隐藏唯一操作入口。

### FE-010-U-10 [低] 日期格式化和昵称首字逻辑散落且缺少非法值处理

- 位置：`UserManage.vue:331-337`、`StudentManage.vue:78-81`、`RoleAuth.vue:108-111`。
- 多个页面重复 `new Date(...).toLocaleDateString`，非法日期会显示 `Invalid Date`；头像/首字逻辑也各写一份。
- 建议：统一 `formatAdminDate`、`formatId`、`getUserDisplayName/getInitial`，对空值和非法日期给出稳定占位符。

## 建议验收用例

1. 保存用户后立即关闭、重新打开新建弹窗，确认不存在延迟错写。
2. 快速从班级 A 切到班级 B，确认 A 的慢响应不会覆盖 B。
3. 筛选、翻页、刷新后，复选框和批量按钮状态一致。
4. 201 个以上角色可搜索选择；不存在 role `1` 时新建用户仍可正常工作。
5. 360px 宽度下仍能执行编辑、封禁、删除和学生移除。
