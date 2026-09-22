# FE-008 题库、题单、标签、目录 Review

## 1. Review 范围

- 公共题库：`oj-vue/src/views/problem/ProblemSet.vue`、`oj-vue/src/components/problemset/ProblemList.vue`、`ProblemListForm.vue`
- 公共题单：`oj-vue/src/views/list/List.vue`
- 后台题目管理与编辑：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue`、`ProblemEditView.vue`、`AnswerEditor.vue`、`CaseEditor.vue`
- 后台题单管理：`oj-vue/src/views/backend/problem-module/list-edit/ListEdit.vue`、`ListProblem.vue`、`list-problem-view/*`
- 后台目录管理：`oj-vue/src/views/backend/problem-module/folder-edit/FolderEdit.vue`
- 标签管理与标签选择：`oj-vue/src/views/backend/problem-module/tag-edit/TagEdit.vue`、题目标签弹窗、题目/题单/目录选择器
- 复用组件：`oj-vue/src/components/ProblemPicker/ProblemPicker.vue`、`oj-vue/src/components/ListView/ListView.vue`、`ProblemModuleShell.vue`
- API/工具：`oj-vue/src/api/problem/index.ts`、`api/problem/label.ts`、`api/list/*`、`api/tag/index.ts`、`api/folder/index.ts`、`utils/simpleCRUD.ts`
- 本次覆盖：CRUD 正确性、批量操作、请求竞态、表单校验、权限过滤、Long ID/类型契约、拖拽排序、标签/题单/目录选择、空态/错误态、CSS 复用、响应式、暗色模式和可访问性。
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

这组模块已经有较明确的页面壳、题单选择器、题目选择器、目录树和批量排序入口，视觉上也开始使用统一的卡片、信息头和主题变量。但目前 CRUD 和选择器的状态边界仍然比较松散，多个关键操作依赖共享 reactive 对象和 debounce 定时器，失败时又没有回滚或页面级错误态。

最高优先级是修复题目管理删除/上传、题目标签保存、题单题目删除和拖拽排序的实际数据一致性；其次要统一题型/标签/Long ID 契约以及选择器的请求生命周期。CSS 方面，题库、题单、目录和选择器仍有多套重复的表单/表格布局，固定高度和移动端断点没有形成统一规则，后续每次改后台布局都容易牵连其它模块。

## 3. 问题清单

### FE-008-01 [高] 题目管理的批量删除实际只删除第一条题目

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:334-343`
- 问题：顶部删除按钮在有多选时可用，但 `handleDelete()` 只取 `ids.value[0]`，确认文案也只显示一个 ID，随后调用单个 `removeProblems(id)`。用户选择多条题目后点击批量删除，剩余选中项不会被删除。
- 影响：按钮语义、确认文案和实际后端请求不一致；管理员可能误以为整批已经删除，随后又对残留数据重复操作。
- 建议：根据 `ids` 长度分别走单条/批量接口，确认文案列出数量和可截断 ID；成功后清空 selection、刷新列表并把当前页归一化。删除按钮和处理函数的参数类型也应区分 row action 与 batch action，不要依赖数组第一项。

### FE-008-02 [高] 搜索后强行修改 single/multiple，导致无选中时也能修改/删除

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:282-286,345-350`
- 问题：`handleSelectionChange` 本来根据真实选择数量维护 `single/multiple`，但 `handleQuery` 查询后无条件设置 `single.value = false`、`multiple.value = false`。这会使顶部修改/删除按钮在没有选中行时被解禁；`handleUpdate` 可能把 undefined 作为题目 ID，删除则取 `ids[0]`。
- 影响：筛选或刷新后，用户可以点击一个本不应该可用的操作；错误请求可能带空 ID，页面表现为无响应、错误通知或误删第一条逻辑之外的数据。
- 建议：查询、刷新、删除后统一清空 selection 并恢复 `single=true/multiple=true`；按钮禁用状态只由 selection 派生，不要在查询 handler 中手动篡改。单行操作和批量操作使用独立 handler。

### FE-008-03 [高] 题目上传绕过统一 HTTP 层，没有携带项目自定义 token 头

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:173-195`
- 问题：`el-upload` 直接使用 `action="/api/problem-api/problem/upload"`，没有配置 `headers`；项目的 axios 请求拦截器才会注入自定义 `token` 头。`withCredentials` 只能带 cookie，不能替代该自定义 header。
- 影响：如果生产上传接口依赖双 key/`token` 请求头，上传会得到 401/403；普通 axios API 正常而上传异常，容易被误判成文件格式或网关问题。
- 建议：上传统一复用带认证的 request，或在上传组件明确从 token store 注入当前 access token，并处理 token 刷新失败；不要在页面里散落 `/api` 直连地址。上传成功/失败响应也应走统一 AjaxResult 解包。

### FE-008-04 [高] 上传 loading、debounce 和文件校验不完整，可能永久 loading 或提交空文件

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:173-195,288-331`
- 问题：上传按钮复用了题目列表的 `isLoading`；点击导入时直接把它置为 true，再通过 1 秒 debounce 调 `uploadRef.submit()`。没有检查是否选择文件、数量、大小和 JSON 类型；无文件时可能没有 success/error 回调，loading 不会复位。取消上传也没有将 `isLoading` 置 false。`multiple` 与文案“单文件不超过 500KB”相矛盾，`uploadError` 还直接 `JSON.parse(evt.message)`，遇到普通文本错误会在错误处理器中再次抛异常。
- 影响：用户可看到“开始导入”一直转圈；多个文件被一次提交但后端可能只按单文件处理；真实上传错误会被错误解析异常覆盖。
- 建议：上传使用独立 `uploading` 状态和明确的文件队列校验；无文件立即提示，超过大小/类型拒绝；取消、成功、失败、超时都走同一个终态。错误解析使用安全 JSON fallback，不要假设浏览器返回一定是 JSON。

### FE-008-05 [高] 题目标签弹窗用对象引用比较差异，取消后重新选回会被误判为新增/删除

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:376-415`
- 问题：保存差异使用 lodash `difference(currentCards.value, originCards.value)`，比较的是对象引用而不是 `tagId`。初次数据来自 `fetchTagByProblemId`，重新勾选时使用的是 `allCards` 中的另一个对象，即使 tagId 相同，仍会被算成新增和删除。
- 影响：用户把原标签取消后重新选回，保存时可能同时发送错误的 add/del；后端若有唯一键约束会报重复，若没有约束则可能产生重复关系或不可预测结果。
- 建议：所有标签集合差异按 `String(tagId)` 建立 Set 计算；表单内部只保存 ID，显示信息从标签 registry 派生。提交前去重，后端接口也应保证关系幂等。

### FE-008-06 [高] 标签新增和删除并发执行，任一请求完成都会关闭弹窗并结束 loading

- 位置：`oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:394-417`
- 问题：`submit` 同时启动 `tagAdd()` 和 `delTagForProblem()`；两边共享同一个 `addIsLoading/finish`，删除完成时直接关闭弹窗，新增完成时回调也会关闭。没有 `Promise.all`、部分成功状态或错误回滚。
- 影响：新增请求尚未结束时弹窗已经关闭，用户看不到新增失败；loading 可能被第一个请求结束提前关闭。保存后重新打开标签时，页面可能呈现半更新状态。
- 建议：将 add/del 组合成一次可观察的保存事务，至少用 `Promise.allSettled` 等待双方结束，再根据结果提示“全部成功/部分失败”；只有全部成功才关闭并刷新。更理想的是后端提供题目标签全量替换接口，前端一次提交最终 ID 集合。

### FE-008-07 [中] 标签管理的请求失败没有页面错误态，删除取消和服务错误都静默/未处理

- 位置：`oj-vue/src/views/backend/problem-module/tag-edit/TagEdit.vue:86-95`、`oj-vue/src/api/tag/index.ts:29-67`
- 问题：`getList` 只有 try/finally，没有 error state；失败时表格继续显示旧数据或空表。`handleDelete` 没有 catch，用户取消确认和后端删除失败都会以 rejected Promise 结束；debounced CRUD wrapper 也没有统一 catch/cancel。
- 影响：标签接口异常时页面看起来像没有标签；删除失败没有明确反馈，控制台可能出现 unhandled rejection。刷新、删除、保存的 loading 和数据状态也可能互相覆盖。
- 建议：页面维护 loading/data/error 三态，保留旧数据时显示刷新失败；确认取消单独吞掉，服务错误必须提示。API 层统一返回可识别错误并支持 abort/cancel，组件卸载时取消 debounce。

### FE-008-08 [中] 题单表单声明了 rules 却没有执行校验

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListEdit.vue:30-33,49-74`
- 问题：题单弹窗绑定了 `:rules="rules"`，但没有 `formRef`，`submitForm` 直接调用 add/update，既没有 `validate()` 也没有 trim/长度/描述归一化。
- 影响：空题单名称可能被发送到后端；校验规则看起来存在却不会生效，用户会在保存后才收到后端错误。新增和编辑页面的表单体验也不一致。
- 建议：为表单增加 FormInstance，保存前统一 validate；提交 payload 使用快照而不是直接传 reactive form，并在保存期间锁定取消/切换模式。

### FE-008-09 [高] 题单题目行删除忽略传入 row，可能删除了选中的其它题目或发送空数组

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListProblem.vue:119-128,280-303`
- 问题：`handleDelete(row)` 无论是否传入行，开头都根据 `ids.value` 生成 relations。点击行内“删除”时，如果没有选中行会发送空 relations；如果之前选中过其它行，则确认文案显示当前 row，实际删除请求却指向旧的 selection。
- 影响：行操作和批量操作的目标错位，可能误删错误题目或让用户看到“已确认但没有变化”。
- 建议：行操作直接构造 `[row.problemId]`，批量操作只使用 selection；两者使用不同确认文案和请求路径。删除成功后清理 selection，并保留后端失败信息。

### FE-008-10 [高] 题单添加请求持有可变 relations，取消弹窗可能把延迟请求清空

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListProblem.vue:420-450`、`oj-vue/src/api/list/index.ts:78-85`
- 问题：`debouncedAddProblemToList` 闭包持有 reactive `relations`；`submit` 先写入它，再调用 1 秒 debounce 的 `add()`。用户在请求真正发出前点击取消，`reset()` 会清空 `relations`，延迟请求收到的就可能是空数组。
- 影响：界面显示已选题目，但后端没有新增关系；因为弹窗已关闭，用户很难知道是取消触发了数据变异。
- 建议：提交时创建不可变 payload 快照，并立即进入 submitting 状态；取消只允许取消尚未提交的 debounce，已经确认的请求不能靠清空 reactive 引用改变 payload。API wrapper 应接受函数或快照，而不是保存外部可变对象。

### FE-008-11 [中] 题单分数更新先改本地值，失败时没有回滚或重试状态

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListProblem.vue:309-323`
- 问题：`handleScoreUpdate` 在调用 `updateProblemRelation` 前就把 `data.score = data.tempScore`，请求没有 loading、错误回滚或版本校验；用户连续编辑多个分数时，会发出多个独立请求，返回顺序也不受控制。
- 影响：保存失败后表格仍显示新分数，刷新才发现数据没变；并发编辑同一行可能后返回的旧请求覆盖新值。拖拽排序和分数保存也没有统一的未保存状态提示。
- 建议：行级维护 saving/error/lastSaved 值，请求成功后再提交本地基线；失败恢复旧值并提供重试。若允许批量编辑，应收集变更后一次提交并展示未保存数量。

### FE-008-12 [中] 题单拖拽只支持鼠标 HTML5 DnD，移动端和键盘无法调整顺序

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListProblem.vue:96-112,325-373`
- 问题：拖动依赖 `draggable`、dragstart/dragover/drop，只给了 handle 的 title 和 grab 光标；没有键盘移动、触摸 pointer 方案、`aria-grabbed` 或目标位置提示。drop 永远插到目标之后，视觉上没有“上方/下方”的明确标记。
- 影响：触屏设备无法排序，键盘用户无法完成同一任务；大量题目排序时用户不容易判断最终插入位置。浏览器/系统拖拽中断也没有全局清理 drag 状态。
- 建议：抽取可复用 reorder composable，支持 pointer/keyboard；提供上移、下移和移动到位置的无障碍操作，拖拽预览明确插入线。保存前展示顺序变更摘要，取消时回滚到快照。

### FE-008-13 [中] ListProblem 的 route/listId 只在 setup 时捕获，同一路由切换题单会显示旧题单

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/ListProblem.vue:180-193,219-232,453-459`
- 问题：`listId` 来自 route computed，但 `queryParams.listId`、`form.listId` 和 `debouncedGetProblem(listId.value, ...)` 都在 setup 时取一次；没有 watch route 参数，也没有重置 table、orderSnapshot、selection 和 add picker。
- 影响：在同一个 `list-problem` 路由实例内从题单 A 跳到 B 时，标题可能显示 B，但请求仍然获取 A；拖拽保存甚至可能把 A 的顺序提交到错误的上下文。
- 建议：把 listId 变化作为资源切换事件，重新创建/取消查询和保存状态；所有 API 调用实时读取快照 listId，并在响应落地前校验 listId。不要把 computed 在初始化时解引用后长期保存。

### FE-008-14 [中] 添加题目选择器分页时 selection 会丢失，且表格没有稳定 row-key

- 位置：`oj-vue/src/views/backend/problem-module/list-edit/list-problem-view/ListProblemView.vue:10-12,34-40,97-109`
- 问题：选择表格没有 `row-key`，`ids` 每次 selection-change 都被替换为当前页选中项；分页或重新查询会丢失上一页选择。`single/multiple` 只是维护了却没有提供清晰的跨页选择状态。
- 影响：用户在弹窗中跨页选择题目时，最终加入题单的数量小于界面之前选择的数量；表格重渲染后高亮/选中状态也不稳定。
- 建议：使用 `row-key="problemId"` 和跨页 selection 缓存 Set；切换筛选时明确保留或清空选择，并在底部显示“已选 X 道，当前页 Y 道”。

### FE-008-15 [高] 题型筛选在不同页面使用数字和字符串两套契约

- 位置：
  - `oj-vue/src/api/problem/index.ts:12-17,136-140`
  - `oj-vue/src/components/problemset/ProblemListForm.vue:30-35`
  - `oj-vue/src/views/backend/problem-module/problem-edit/ProblemEdit.vue:19-31`
  - `oj-vue/src/components/ProblemPicker/ProblemPicker.vue:21-24,93-116`
  - `oj-vue/src/views/backend/problem-module/list-edit/list-problem-view/ListProblemViewForm.vue:28-34`
- 问题：`ProblemType` 是数字 enum，但公共题库 radio 使用 `OJ/FILL/CHOICE/MULTI_CHOICE` 字符串，后台题目管理使用 `problemTypeStr` 字符串，ProblemPicker 再通过 `as ProblemType` 强制断言；只有 ListProblemViewForm 使用 1～4 数值。`ProblemParam/AdminQueryProblem` 的类型又声明为数字 enum。
- 影响：不同入口发给后端的筛选值不一致，某些接口能把字符串 enum 转换成功，另一些会返回空列表或类型转换错误。新增题型时还要同时维护多套映射。
- 建议：明确 HTTP 契约只传一种格式，推荐稳定的字符串 code 或稳定数字 code，UI label 统一由 registry 提供；删除 `as ProblemType` 这类掩盖错误的断言，并对接口返回做 normalize。

### FE-008-16 [高] ProblemPicker 用初始化时的权限快照决定 admin/public 接口

- 位置：`oj-vue/src/components/ProblemPicker/ProblemPicker.vue:90-116`
- 问题：`const isAdminPicker = hasPerm('problem:problem:list')` 只在组件 setup 执行一次。动态权限尚未加载、登录切换、权限刷新后，这个值不会更新；组件会持续调用 public 或 admin endpoint。
- 影响：管理员可能看到公开题目列表而不是完整题库，普通用户在权限状态异常时可能触发 admin 请求并不断收到 403。权限边界表现取决于组件挂载时序，而不是当前权限状态。
- 建议：权限加载完成后再渲染选择器，或把 permission store 的响应式状态作为 computed；后端仍必须做最终鉴权。选择器应在权限变化时清空旧结果并重新查询，不能继续展示上一个权限上下文的数据。

### FE-008-17 [中] ProblemPicker、ListView 和题单候选题列表失败时都清空数据，没有 error/retry 语义

- 位置：`oj-vue/src/components/ProblemPicker/ProblemPicker.vue:105-124`、`oj-vue/src/components/ListView/ListView.vue:109-140`、`oj-vue/src/views/backend/problem-module/list-edit/list-problem-view/ListProblemView.vue:82-95`
- 问题：这些选择器的 catch 大多将列表设为空或完全没有 catch；loading 结束后只展示“没有找到/没有数据”。请求没有 AbortController/requestId，快速搜索、分页或关闭弹窗后，旧响应仍可能覆盖新结果。
- 影响：权限错误、网络错误、真正的空结果和旧结果被混为一谈；目录编辑、题目编辑、题解/题单选择时，用户无法判断是没有可选项还是服务不可用。
- 建议：抽出统一的 `usePagedPicker`，管理 query snapshot、loading/empty/error、重试和取消；切换选择器上下文时清理旧响应。错误态应保留明确的权限/网络提示，不要只清空数组。

### FE-008-18 [中] 公共题单目录点击非文件节点或未开放题单时会保留旧题目内容

- 位置：`oj-vue/src/views/list/List.vue:61-81`
- 问题：`handleNodeClick` 只有在文件节点且有 listId 时才请求；点击目录/菜单或未开放文件时没有清空 `tableList`。它先更新 `selectedName`，随后直接 return 或等待请求，旧题目的表格仍可能显示在新节点名称下。`isLoading` 也从未被设为 true。
- 影响：用户看到的题单标题和题目列表不匹配；切换到未开放目录时会误以为目录仍包含旧题目。网络慢时没有加载反馈，失败时 Promise 未处理。
- 建议：点击任何节点先清理/标记当前资源，目录节点显示目录说明而不是复用旧表格；文件节点请求使用 loading/error/empty 状态和 nodeId 快照。为树加载和题单题目加载增加重试入口。

### FE-008-19 [中] 目录树排序优先按节点类型，覆盖了用户配置的 order

- 位置：`oj-vue/src/views/backend/problem-module/folder-edit/FolderEdit.vue:95-100`
- 问题：`decorateTree` 使用 `folderTypeRank` 作为第一排序条件：目录永远在菜单前，菜单永远在文件前，只有同类型才比较 `folder.order`。这会无条件改变后端保存的显示顺序。
- 影响：管理员在目录中设置顺序后，前台/后台看到的顺序可能与保存值不同；不同类型节点混排的场景无法实现，维护者也很难判断到底是后端顺序还是前端排序生效。
- 建议：如果 `order` 是唯一展示顺序，直接按 order 排序并用类型作为稳定的次级 tie-breaker；如果业务确实要求按类型分组，应明确展示为分组规则并同步到前台树，不能隐式覆盖用户输入。

### FE-008-20 [中] 目录刷新/过滤后 selection 没有统一清理，批量删除可能作用于不可见旧节点

- 位置：`oj-vue/src/views/backend/problem-module/folder-edit/FolderEdit.vue:109-119`
- 问题：`getList`、`handleQuery` 和 `resetQuery` 没有清空 `selectedIds` 或调用 table clearSelection；过滤条件变化后，旧 selection 仍可能留在状态里。批量删除直接使用 `selectedIds.value`，不校验这些节点是否仍在当前数据集。
- 影响：管理员筛选目录后看到“未选择节点”或当前结果之外的 selection 状态，点击批量删除可能删除上一轮筛选中的节点。树节点父子同时选中时还可能向后端发送重复 ID。
- 建议：查询/刷新/分页边界统一清理 selection，或者维护明确的跨筛选选择模型并在 UI 显示；删除前按 String ID 去重并重新校验节点仍存在、是否有权限和是否为当前上下文。

### FE-008-21 [中] 目录文件节点的“必须选择题单”只在提交 handler 校验，表单规则和状态没有同步

- 位置：`oj-vue/src/views/backend/problem-module/folder-edit/FolderEdit.vue:42-49,121-131`
- 问题：`listId` 没有放入 `rules`，文件节点必须关联题单的约束只写在 `submitForm` 的 if 中；切换节点类型、异步回填题单名和选择器关闭之间也没有统一字段状态。用户点击保存时才得到警告，表单的错误区域不会定位到题单字段。
- 影响：复杂弹窗中用户不知道哪个字段未完成；键盘提交、未来复用表单或新增自动保存入口都可能绕过这条局部校验。编辑已有异常数据时，非文件节点仍可能携带旧 listId。
- 建议：使用动态 FormRule：`folderType === FILE` 时 listId required；类型切换时同时清理字段和校验状态；选择器返回 `{id,name}` 快照，显示和提交分离。后端仍需保证节点类型与 listId 的一致性。

### FE-008-22 [中] 多个 CRUD debounce wrapper 保存外部 reactive form，取消/切换操作会改变已排队 payload

- 位置：`oj-vue/src/api/problem/index.ts:205-263`、`api/list/index.ts:78-101,154-192`、`api/tag/index.ts:29-67`、`api/folder/index.ts:72-111`
- 问题：add/update wrapper 在创建时把 `form` 引用闭包保存，调用时再读取；它们只返回 loading/isLoading，没有 cancel、请求序号或错误状态。页面 resetForm、切换编辑对象或关闭弹窗时，会改变同一个对象，延迟调用发送的内容可能已经不是用户点击保存时看到的内容。
- 影响：新增变编辑、取消后仍提交、旧请求覆盖新列表等问题会在所有题库 CRUD 页面重复出现；后续修一个页面容易漏掉其它 API wrapper。
- 建议：debounce 只负责输入合并，不负责业务提交；点击保存时创建深拷贝 payload，立即进入 action state。统一封装 `useAsyncAction`，提供 cancel/abort、requestId、error、finally 和幂等保护，组件卸载时释放定时器。

### FE-008-23 [低] 题单、标签和目录 API 类型没有表达真实 JSON 数据，Long ID/日期/空字段仍会漂移

- 位置：`oj-vue/src/api/list/index.ts:12-16`、`api/tag/index.ts:7-12`、`api/problem/label.ts:7-13`、`api/folder/index.ts:14-36`
- 问题：`ListView` 没有声明模板实际使用的 `createTime`；`TagView.createTime`、`ProblemView.createTime` 声明为 Date，但 HTTP JSON 通常是字符串；目录 `listId/parentId/order` 声明为必填，却对根节点/非文件节点使用 undefined/0；多个 ID 只依赖 `IdType=string`，展示又各自实现 shortId。
- 影响：类型检查无法保护页面对真实响应的使用，空题单、根目录、旧数据和日期格式异常会在深层模板才暴露；不同选择器对 Long ID 的截断长度也不一致。
- 建议：在 API 边界做 response normalizer，明确 `string | undefined`/日期字符串契约；补齐 ListView 实际字段。抽取 `ShortId`/`EntityReference` 展示组件，统一完整值 tooltip 和中间截断规则。

### FE-008-24 [低][CSS] 题库/题单/目录/选择器重复实现 filter、toolbar、table 和 dialog 样式

- 位置：`ProblemSet.vue`、`ProblemListForm.vue`、`List.vue`、`ListEdit.vue`、`TagEdit.vue`、`FolderEdit.vue`、`ListView.vue`、`ProblemPicker.vue`
- 问题：多个页面各自定义 `.filter-panel`、`.toolbar-actions`、`.dialog-intro`、`.picker-table`、`.module-toolbar` 和表格 header/row 高度；同名类在 scoped 与非 scoped style、不同组件中反复出现，断点也分别使用 440/460/480/520/600/620/640/650/700/720/760/800px。
- 影响：一个后台模块修正按钮间距、表格行高或移动端换行，不能自然同步其它模块；窄屏下页面之间的操作区高度和内边距不一致，暗色模式也容易出现某个组件仍使用固定浅色背景。
- 建议：抽取后台 `ModuleToolbar`、`FilterPanel`、`EntityPickerTable`、`DialogIntro` 和 table token；统一断点与 spacing token。页面只提供语义变体，不再复制整段布局 CSS。

### FE-008-25 [中][CSS] 目录、题单和选择器的固定高度/overflow 组合会产生裁剪和双滚动

- 位置：`FolderEdit.vue:137-145`、`ListView.vue:144-150`、`ProblemPicker.vue:145-164`、`ListProblem.vue:482-486`
- 问题：目录父级选择器使用 `max-height:250px`，题单添加弹窗使用 `min-height:420px; max-height:62vh`，选择器表格和 Element Dialog/Scrollbar 又各自管理 overflow。题库公共页的分页仍使用绝对定位，题单/目录管理表格固定列和移动端隐藏策略也不统一。
- 影响：小屏或浏览器字体放大时，弹窗会同时出现内外滚动条，按钮/选择摘要可能被挤出；目录树层级深、题目描述较长或错误提示出现时，内容容易被裁剪。固定高度还会使空态显得过度留白。
- 建议：每个弹窗只保留一个内容滚动容器，header/footer 固定；列表主体使用 `minmax(0,1fr)` 或按内容自适应高度，移动端切换为卡片/横向滚动而不是隐藏固定操作列。分页用 flex/grid，不要绝对定位。

### FE-008-26 [低][CSS/暗色/可访问性] 管理模块大量硬编码颜色和非按钮点击语义

- 位置：`ProblemModuleShell.vue:40-44`、`FolderEdit.vue:137-145`、`ProblemEdit.vue:454-487`、`ListProblem.vue:96-128`
- 问题：蓝/琥珀/紫/绿主题大量直接写 `#2563eb`、`#d97706`、`rgb(... / 12%)`，没有以 Element 主题变量或暗色 token 为主；拖动 handle 是普通 span，题目描述/删除等部分交互也依赖 link/普通区域，没有统一的 focus-visible、aria-label 和键盘操作。
- 影响：暗色主题下某些颜色对比度随背景变化不可控；键盘用户不能完成排序或识别图标操作，焦点状态也可能不可见。主题切换和品牌配色调整会需要搜索修改大量 CSS。
- 建议：为每个模块 tone 定义 CSS variables，并提供 light/dark token；可操作元素使用 button/真实表格操作语义，补 focus-visible、aria-label 和键盘行为。颜色只承担辅助信息，状态同时提供文字。

## 4. 做得较好的地方

- 后台题目、题单、标签和目录已经统一接入 `ProblemModuleShell`，标题、返回、操作区和内容容器有了明确边界，后续适合继续抽取模块级基础组件。
- 题单顺序已经采用“本地拖动—变更计数—批量保存—撤销”的交互方向，`orderSnapshot` 也说明代码已经考虑了未保存状态，而不是每次拖动都直接请求后端。
- 目录管理已经支持树形父级排除、ID 短显示、父级路径保留和文件节点题单选择，复杂关系的 UI 模型比简单平铺表格清晰。
- `ProblemPicker`、`ListView` 和题目标签弹窗都尝试显示名称与截断后的 Long ID，并保留完整值 tooltip，符合平台目前的 ID 展示需求。
- 标签管理使用表单校验、颜色归一化和预览，且颜色保存前会转换为十六进制，方向上已经避免了原先 `rgb(...)` 直接提交的问题。

## 5. 建议修复顺序

1. 先修 FE-008-01、FE-008-02、FE-008-03、FE-008-04、FE-008-05、FE-008-06、FE-008-09、FE-008-10：这些会直接导致错误删除、上传失败、标签错配或新增题目不生效。
2. 修 FE-008-07、FE-008-08、FE-008-11、FE-008-13、FE-008-14、FE-008-15、FE-008-16、FE-008-17：统一 CRUD/选择器状态、表单校验、路由上下文和题型契约。
3. 修 FE-008-18、FE-008-19、FE-008-20、FE-008-21：保证公共目录和后台目录的节点、顺序、选择与题单关联一致。
4. 修 FE-008-22、FE-008-23、FE-008-24、FE-008-25、FE-008-26：抽取请求/展示/CSS 基础能力，收敛响应式、暗色和可访问性。
5. 补回归测试：批量删除、无选中操作、上传无文件/401/非 JSON 错误、标签取消后重选、标签 add/del 部分失败、题单取消延迟提交、跨页选择、路由切换题单、目录筛选后删除、所有题型 filter code、320～1440px 和暗色模式。

## 6. 测试与验证

### 本次检查

- 逐行检查了公共题库/题单、后台题目/题单/标签/目录页面、题目/题单选择器、答案/测试点编辑器和相关 API wrapper。
- 检查了 CRUD 请求 payload、批量删除、题单顺序保存、分数更新、标签关系增删、目录父级选择、动态权限、Long ID、空态/错误态和 debounce 生命周期。
- 检查了固定高度、表格固定列、弹窗滚动、移动端断点、暗色颜色变量、拖拽和键盘语义。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 本次 review 没有替代真实浏览器回归；上传认证、快速连续操作、路由复用、拖拽中断、跨页选择、慢网络和暗色/移动端布局需要后续浏览器验证。
- 当前报告阶段尚未执行前端类型检查；后续会单独运行 `pnpm run type-check` 和 `git diff --check`，并确认只新增本报告。

## 7. 本次未修改内容

- 未修改题库、题单、标签、目录页面、选择器、拖拽逻辑、CRUD API 或后端代码。
- 未提交 Git。
- 仅新增本 review 报告。
