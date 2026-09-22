# FE-010 题目模块 Review

## 范围

- `oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue`
- `ProblemEditView.vue`、`AnswerEditor.vue`、`CaseEditor.vue`
- `list-edit/ListEdit.vue`、`ListProblem.vue`、`FolderEdit.vue`、`TagEdit.vue`、`SolutionEdit.vue`
- `ProblemModuleShell.vue` 及题目相关 API

## 结论

题目模块业务复杂度最高，已经出现多个“表单 + 选择器 + Markdown/测试用例 + 批量关系”的复合页面。当前最大风险是题单题目操作的选择范围、拖动排序和分数编辑不具备清晰事务边界；题目编辑器的动态题型状态也容易保留旧字段。CSS 方面，题目编辑页、测试用例、答案编辑器和题单页存在多层滚动与重复卡片样式，正是此前“编辑器超出盒子/页面滚动条很长”的高发来源。

## 问题清单

### FE-010-Q-01 [高] 题单删除使用了不明确的 selection 范围，行删除可能带上旧的批量选择

- 位置：`ListProblem.vue:280-303`。
- 行删除与批量删除共用 `selectedIds/relations`，删除关系在校验前构造；当直接点击一行删除而表格仍有旧选择时，可能把不相关题目一起带入请求；没有当前行时又可能发送空关系。
- 建议：行操作永远从当前 row 构造单条 DTO，批量操作只从经过明确清理的 selection snapshot 构造；二者使用不同 handler 和确认文案。

### FE-010-Q-02 [高] 题单排序更新缺少版本/事务语义，批量重排容易覆盖并发修改

- 位置：`ListProblem.vue:325-373`。
- 拖动后按当前数组把所有题目重新编号并批量提交，没有版本号、最后更新时间或失败回滚；管理员 A/B 同时调整时，后提交者会静默覆盖前者。
- 建议：后端批量排序接口接收 `listVersion`/updatedAt 并做乐观锁；前端保存前显示待提交数量，失败恢复服务端顺序并保留草稿，不要把本地顺序当成已保存状态。

### FE-010-Q-03 [中] HTML5 拖动排序缺少键盘/触摸支持，且拖动反馈不表达插入位置

- 位置：`ListProblem.vue:325-373`。
- 目前依赖鼠标 drag/drop，未提供键盘调整、触摸端替代、拖动目标前后位置和取消操作。移动端或无障碍用户无法管理顺序。
- 建议：抽取可复用 reorder composable/组件，提供 move up/down、键盘操作、拖动占位线和取消/恢复草稿。

### FE-010-Q-04 [高] 题目/题单/标签等 debounce API 读取外部可变数组或表单，关闭后仍可能提交

- 位置：`ProblemEdit.vue:298-331,395-430`、`ListProblem.vue:428-430`、对应 `api/problem/list/tag`。
- 上传、题目关系添加、标签差异提交都将 reactive object/array 交给 debounce；关闭弹窗、reset、切换题目后会清空或复用同一个引用。
- 影响：出现“弹窗已经取消但后台仍变更”“新增标签后列表状态与服务器不一致”等问题。
- 建议：所有 mutation 在点击提交时生成不可变 payload，统一使用可取消的 action composable；成功后再刷新，不要让 `finish` 既表示成功又表示请求结束。

### FE-010-Q-05 [高] 题型切换直接清空答案/测试用例，没有未保存确认和可恢复草稿

- 位置：`ProblemEditView.vue:142-154`。
- `handleTypeChange` 在切换题型时直接清空 `choices/cases` 等复杂字段，用户只要误点类型就会丢失输入；页面没有 dirty 状态或确认提示。
- 建议：题型数据按 type 分区保存，切换时给出“保留/清空”确认；离开页面、切路由和关闭弹窗复用同一 dirty guard。

### FE-010-Q-06 [中] 编辑页面依赖一次性 route 参数，路由复用时不会重新加载题目

- 位置：`ProblemEditView.vue:115,156-157`、`ListProblem.vue` 的 `listId` 快照。
- 题目/题单 ID 在 setup 时读取，组件被同一条动态路由复用时，URL 变化不会触发新的详情请求。
- 建议：把 route params/query 转成 computed 并 watch；watch 回调带 request id/AbortController，旧详情不能写入新题目。

### FE-010-Q-07 [中] 分数编辑按 blur 逐条提交，没有 saving/error/rollback 状态

- 位置：`ListProblem.vue:309-323`。
- 组件先修改 `data.score`，再发请求；没有禁用重复 blur、没有保存指示、失败也不回滚旧分数。多个输入快速修改时响应顺序可能反写旧值。
- 建议：分数采用显式编辑态/批量保存，或每行维护 `saving/error/lastCommittedScore`；请求带版本或以最后一次编辑序列号为准。

### FE-010-Q-08 [中] 标签差异用对象比较而不是稳定 ID，重载/代理对象后会误判增删

- 位置：`ProblemEdit.vue:367-447`。
- `originCards/currentCards` 之间的差异依赖对象引用或对象整体比较，而不是 `tagId` 集合；API 返回对象重建、字段顺序变化或响应式代理都会导致错误差异。
- 建议：统一转换为 `Set<String(tagId)>` 计算 added/removed；提交成功后重新获取题目标签，失败保留编辑态。

### FE-010-Q-09 [中][CSS] 题目编辑器存在多层固定高度/滚动容器，容易出现双滚动和内容被裁切

- 位置：`ProblemEditView.vue:162-168`、`ProblemEdit.vue` 弹窗 Markdown/上传区域、`AnswerEditor.vue`、`CaseEditor.vue`。
- 编辑页用 `calc(100vh - ...)` 固定滚动，弹窗又限制 `max-height`，Markdown 编辑器、答案卡片、测试用例 textarea 自己还拥有滚动；页面嵌套时高度基准并不一致。
- 影响：编辑器超出父盒、全屏/窄屏时滚动条无限增长、弹窗 footer 不可见。
- 建议：明确一个滚动归属；布局容器使用 `min-height:0`/`overflow:hidden`，内容区由一个 `overflow:auto` 承担；编辑器高度用 ResizeObserver 基于实际容器，而不是全局 viewport 计算。

### FE-010-Q-10 [中][CSS/重复] AnswerEditor、CaseEditor 和题目弹窗复制卡片、toolbar、按钮和移动端断点

- 位置：`AnswerEditor.vue:80+`、`CaseEditor.vue`、`ProblemEdit.vue`/`ContestManagement.vue`。
- 相同的 header、section、card、empty、按钮间距和 `@media 760/480` 被多次实现；颜色也混用硬编码和主题变量。
- 建议：抽出 `EditorSection`、`EditorCardList`、`AutosizeCodeField` 和统一 editor token；题目、竞赛、公告共用 Markdown 外壳时只保留一份尺寸协议。

### FE-010-Q-11 [中] 上传错误解析假设响应一定是 JSON，且上传操作复用了列表 loading

- 位置：`ProblemEdit.vue:298-331`。
- `JSON.parse(evt.message).message` 对 HTML、纯文本、空响应会再次抛错；上传 loading 与列表 loading 共享，取消/失败路径可能让整个题目列表一直显示加载。
- 建议：安全解析响应并保留原始 request id；上传使用独立状态，限制文件类型/大小，支持取消和错误恢复。

### FE-010-Q-12 [低] `ProblemEditView.back` 使用的路由名与动态路由命名不一致

- 位置：`ProblemEditView.vue:144`，动态路由定义中的题目编辑路由名需同步核对。
- 页面返回直接 push `problem-edit`，而当前动态路由使用的名字存在 `edit-problem` 的版本，可能导致返回时报 route not found 或落入错误页面。
- 建议：抽取模块级显式返回目标，不要依赖字符串散落；为每个动态管理页加路由集成测试。

## 建议验收用例

1. 题单行删除、批量删除分别操作，确认不会混入旧 selection。
2. 两个浏览器同时排序同一题单，后提交者能收到版本冲突而不是静默覆盖。
3. 题型切换、关闭、返回和刷新时，未保存答案有确认且不会无提示丢失。
4. 题目编辑页在 360px、两列、全屏和深色模式下只有一个主要滚动容器。
5. 题目标签增删、上传失败、网络超时后，loading 和本地编辑数据都能恢复。
