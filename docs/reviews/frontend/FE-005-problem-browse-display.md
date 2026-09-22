# FE-005 题目浏览与题目展示 Review

## 1. Review 范围

- 题库页面：`oj-vue/src/views/problem/Problem.vue`、`ProblemSet.vue`
- 题库筛选/列表：`oj-vue/src/components/problemset/ProblemListForm.vue`、`ProblemList.vue`
- 题目详情：`oj-vue/src/components/DetailProblem/DetailProblem.vue`
- OJ 工作台：`oj-vue/src/components/OjWorkbench/OjWorkbench.vue`
- 各题型：`OnlineJudgeProblem.vue`、`FillBlank.vue`、`ChoiceChoose.vue`、`ProblemRadio.vue`、`ProblemRadioGroup.vue`、`ProblemResult.vue`
- 直接依赖：题目 API、判题/答案 API、题目后端 Controller/Service、MarkdownPreview、题型/难度工具
- 本次覆盖：数据契约与空值、请求竞态、题型分支、答案状态、权限/提交边界、CSS 高度/滚动/弹层、暗色模式、响应式、无障碍和重复逻辑
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

题库列表和题目工作台已经有基础的卡片化视觉、暗色变量、题型组件拆分和 OJ 判题轮询，但目前仍有几个会直接影响正确性和维护成本的问题：

1. 筛选重置只重置了本地表单，没有向父页面发查询事件；标签数据放在普通 Map 中，也没有加载/失败状态，标签筛选可能显示空白或直接访问 undefined。
2. 题库列表和题目详情都采用“延迟请求 + 不取消旧请求 + 静默吞错”的模式；快速换页、换条件或切题时，旧响应可能覆盖新内容，失败时用户只看到旧数据。
3. 详情接口允许返回 `success(null)`，前端却把它强转成完整题目并继续渲染；题目、用户答案、提交记录都没有统一归一化。
4. 详情页同时保留旧的两列题目实现和新的 OjWorkbench，题面、提交记录、示例、状态和 CSS 逻辑已经分叉；后续修一边很容易另一边继续崩。
5. 选择题/填空题组件缺少真正的表单语义和键盘操作，答案数组直接被子组件修改，填空项没有稳定 key，MarkdownPreview 又被嵌套到选项和段落等不适合的结构中。
6. OJ 工作台在固定高度、全屏、窄屏单列和测试面板之间有多层高度所有权；题目预览、代码区和测试区分别管理滚动，760px 附近缺少连续的布局策略。

## 3. 问题清单

### FE-005-01 [高] 筛选“重置”不会真正刷新题目列表

- 位置：
  - `oj-vue/src/components/problemset/ProblemListForm.vue:123-141`
  - `oj-vue/src/views/problem/ProblemSet.vue:7-12,64-75`
- 问题：`onResetHandler` 只清空 `queryForm`，没有调用 `handleQuery` 或 emit 查询事件。父组件只监听 `@query`，所以列表仍然保留上一次搜索结果；用户需要再点一次“查询”才会刷新。
- 影响：UI 显示搜索条件已经清空，结果却仍是旧条件，用户会误以为后端筛选或缓存异常。当前页码也没有回到第一页。
- 建议：将筛选状态和查询动作统一成一个可复用的 `resetAndQuery` 流程：清空条件、页码归一为 1、立即 emit 快照。不要让“重置表单”和“刷新结果”分成两个隐式步骤。

### FE-005-02 [高] 标签 Map 不是响应式数据，标签异步加载期间可能是空弹窗或 undefined

- 位置：`oj-vue/src/components/problemset/ProblemListForm.vue:80,108-110,143-149`
- 问题：`tagsMap` 是普通 `new Map()`，异步请求完成后只调用 `set`，没有修改任何响应式 ref/reactive。模板中的 `v-for="...[key, value] of tagsMap"` 也不会因 Map 内部变化自动更新。
- 影响：用户在标签请求完成前打开弹窗时可能看到空列表；已选标签回填时 `getTag(id)` 可能返回 undefined，随后访问 `tagColor/tagName` 产生运行时错误。标签接口失败时没有任何错误状态和重试入口。
- 建议：用响应式数组或 `reactive(new Map())` 保存标签，并把加载/错误状态纳入组件状态机；`getTag` 对未知 ID 返回显式的占位对象，不能靠类型断言隐藏 undefined。

### FE-005-03 [高] 题库列表的 debounce 请求不能取消旧请求，错误被静默吞掉

- 位置：`oj-vue/src/components/problemset/ProblemList.vue:106-140`
- 问题：列表查询使用一秒 debounce，但只取消尚未发出的定时任务，已经发出的请求没有 AbortController 或请求序号校验。`catch(() => {})` 丢弃所有失败原因；`finally` 无条件把 loading 置为 false。组件卸载时也没有 cancel debounce。
- 影响：快速切换搜索条件/页码时，慢的旧请求可能晚于新请求返回并覆盖列表；旧请求的 finally 还可能提前结束新请求的 loading。网络失败时用户看不到错误和重试入口，旧结果会继续留在页面上，产生“查询没有生效”的错觉。
- 建议：以查询快照生成 requestId，响应落地前校验仍是当前请求；支持取消请求并在 unmount 时取消 debounce。页面至少区分 loading、empty、error 三种状态，错误不能只交给全局 HTTP 拦截器。

### FE-005-04 [高] 题目详情切换时存在旧题面、旧答案和旧提交记录回填

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:654-708,710-741`
  - `oj-vue/src/api/problem/index.ts:300-308`
- 问题：`getProblem` 调用 debounce 后的 `get(problemId)` 不等待，同时并行加载提交记录；路由/题目 ID 变化时，旧的详情、`getAnswer` 和 `getLogs` 请求没有取消，也没有校验响应对应的 problemId。`reset` 还没有清空当前题目对象。
- 影响：快速从题目 A 切到 B 时，A 的响应可能覆盖 B；提交记录和已保存答案也可能错挂到当前题目。页面可能在 loading 时继续显示上一道题的题干，用户提交后很难判断到底提交了哪道题。
- 建议：把题目加载封装成带请求序号/AbortController 的 loader，切题时先清空或标记旧题面；详情、答案、提交记录必须使用同一份题目快照，只有快照仍然有效时才能写入状态。不要让 debounce 代替请求竞态控制。

### FE-005-05 [高] 题目详情空数据没有明确状态，后端 success(null) 会变成前端深处异常

- 位置：
  - `oj-vue/src/api/problem/index.ts:281-288`
  - `problem-service/src/main/java/com/anishan/problem/controller/ProblemController.java:107-112`
  - `problem-service/src/main/java/com/anishan/problem/service/impl/ProblemServiceImpl.java:497-506`
- 问题：题目不存在或不允许公开时，后端可能返回 `R.success(null)`；前端只检查 HTTP/code，随后把 data 强转为 `ProblemDetailView`。模板和 computed 又直接访问 `problemVo`、`ojProblemVo`、`tagVo`。
- 影响：用户看到空白区域、骨架屏不结束或诸如“读取 problemVo 失败”的通用异常，而不是“题目不存在/无权访问”。题目 ID 输错、题目被删除、比赛题目从公开题库隐藏时表现不一致。
- 建议：API 层统一校验 data，空值转为 NotFound/Forbidden 类型错误；后端也应返回明确的 404/业务错误而不是成功 null。详情页增加 not-found/error/retry 状态，不要把完整 VO 的类型断言当作运行时校验。

### FE-005-06 [高] 全局 resize 事件处理会覆盖其他页面，卸载时还清理了错误属性

- 位置：`oj-vue/src/components/DetailProblem/DetailProblem.vue:747-761`
- 问题：组件通过 `window.onresize = ...` 注册全局事件，覆盖同一时间的其他页面/组件处理；卸载时写的是 `window.onreset = null`，没有移除 `onresize`。debounce 实例也没有 cancel。
- 影响：离开题目页面后，旧组件的 resize 回调仍可能访问已卸载的 ref；重新进入多次后可能产生多个 debounce/布局异常。其他使用 `window.onresize` 的组件还会互相覆盖。
- 建议：使用 `addEventListener` 并保存 handler，在 onUnmounted 对称 remove；组件卸载时调用 debounce.cancel。测量逻辑应绑定工作台容器的 ResizeObserver，而不是污染全局 window handler。

### FE-005-07 [中] 提交入口没有在业务函数层再次校验 disableSubmit

- 位置：`oj-vue/src/components/DetailProblem/DetailProblem.vue:555-607`
- 问题：按钮和编辑器收到 `isSubmitDisabled`，但 `onHandleSubmit` 自身没有先判断 `disableSubmit`；OJ 分支甚至会先调用 `lockOjSubmit`。任何未来新增的快捷键、子组件事件或调用方都可能绕过视觉禁用。
- 影响：比赛结束、未开始或权限状态变化后，UI 看起来不能提交，但事件仍可能进入判题请求；即使后端最终拒绝，也会制造多余请求和错误提示。
- 建议：在提交 service 和页面 handler 两层都做最终状态校验；把“活动是否允许提交”作为明确的状态，而不是只传给按钮的布尔值。锁定提交应发生在通过业务校验之后。

### FE-005-08 [中] 竞赛场景无条件隐藏判题结果，显示策略没有和活动配置绑定

- 位置：`oj-vue/src/components/DetailProblem/DetailProblem.vue:402-413,120-122`
- 问题：`isShowResult` 只要存在 `contestId` 就固定返回 false。它没有区分作业/比赛、活动是否结束、是否允许即时反馈或当前用户权限。
- 影响：非 OJ 题在活动中提交后，即使后端已经返回正确/错误答案，页面也永远不展示结果；后续如果只想在正式比赛隐藏结果、作业显示结果，只能继续加散落的条件。
- 建议：由活动接口返回明确的 `showResult/feedbackPolicy`，题目组件只根据策略渲染；把“提交成功”“判题完成”“允许展示结果”分成三个状态，避免用 contestId 代替业务规则。

### FE-005-09 [中] 已保存答案缺少统一归一化，undefined answers 会破坏题型组件

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:710-720`
  - `oj-vue/src/components/DetailProblem/ChoiceChoose.vue:23-39`
  - `oj-vue/src/components/ProblemRadio/ProblemRadio.vue:25-36`
- 问题：`getAnswer` 对 response 只做类型断言，直接把可能为 undefined 的 `answer.answers` 赋给必需的数组；题型组件随后使用 `findIndex`、`push`。填空题的 `blankIndex` 也直接强转 number，没有校验后端数据。
- 影响：旧数据缺少 answers、后端返回 null/部分字段或题型切换后，页面会在选择/填空时才抛异常。问题发生在用户交互深处，不容易从网络响应判断。
- 建议：在 API 层把答案统一归一成 `{answers: [], code: '', languageId: default}`；按题型验证选项 order/blankIndex 的完整性，非法数据进入错误状态而不是继续渲染。

### FE-005-10 [中] 题目类型/难度展示有多套映射和魔法值，未知值没有稳定兜底

- 位置：
  - `oj-vue/src/api/problem/index.ts:11-30,43-75`
  - `oj-vue/src/utils/problem/index.ts:3-6`
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:336-397`
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:286-299`
- 问题：题型既使用枚举，也在 OJ 工作台直接比较数字 `type === 1`；难度和题型文案在多个文件各自建立数组/对象。`problemTypeToString` 直接按数组下标返回，未知值会得到 undefined。
- 影响：后端新增题型、返回字符串枚举或数据异常时，列表标签、详情分支和工作台可能显示不一致，甚至不渲染文案。维护者必须同时改多个映射。
- 建议：抽一个带未知兜底的题型/难度 metadata registry，包含 label、icon、颜色、是否需要代码编辑器等能力；所有页面使用同一 registry，避免在模板中出现裸数字和重复条件。

### FE-005-11 [中][CSS] 题库分页使用绝对定位，页码、页大小和窄屏容易重叠

- 位置：`oj-vue/src/views/problem/ProblemSet.vue:14-35,92-103`
- 问题：footer 只有固定 48px 高度，页大小输入框和分页组件都使用 `position:absolute`；分页固定在 50% 居中，页大小固定在左侧。没有处理总页数变化、控件换行和 600px 以下的布局。
- 影响：窄窗口、浏览器字体放大或分页页码较多时，页大小控件可能覆盖页码/内容；footer 实际高度不足时会产生额外滚动或点击区域重叠。
- 建议：用 flex/grid 让页大小、分页和总数自然布局，窄屏改为上下排列；通过 Element Plus 的 current-page/page-size 双向绑定统一分页状态，不要手动用绝对定位模拟分页布局。

### FE-005-12 [中][CSS] 题目表格没有移动端策略，Long ID 和标签列会挤压主要内容

- 位置：`oj-vue/src/components/problemset/ProblemList.vue:35-73,144-151`
- 问题：表格使用 `table-layout="auto"`，同时展示状态、原始题目 ID、标题、类型、标签和来源；只有 600px 断点调整外层 padding，没有列隐藏/横向滚动/移动卡片策略。Long ID 也没有统一截断，只依赖列 tooltip。
- 影响：平板或窄窗口下标题可读宽度被 ID、标签和来源抢走；标签多时行高不可控，表格整体横向溢出。此前项目里已经多次出现“ID 太长”和“左右内边距不足”的问题，这里仍然是散落式处理。
- 建议：统一 ID 展示组件（短 ID + title 完整值），桌面表格和移动卡片使用不同呈现策略；列配置按优先级隐藏，标签限制数量并提供剩余计数，不要让 auto table layout 决定核心信息的宽度。

### FE-005-13 [中] 列表错误、空值和未知题型会被当成空白/旧数据

- 位置：
  - `oj-vue/src/components/problemset/ProblemList.vue:106-125`
  - `oj-vue/src/api/problem/index.ts:266-287`
- 问题：`data.forEach` 假设 data 永远是数组；API 没有对根 data、分页字段和题目字段归一化。列表 catch 为空，失败时既不清空旧数据也不显示错误；题型转换没有未知兜底。
- 影响：后端返回 null、缓存旧结构或部分字段时，列表在渲染阶段才报错；查询失败会继续显示上一组题目，用户无法区分“没有结果”和“请求失败”。
- 建议：API 层定义严格的 paged response normalizer，列表统一使用 `items=[]` 的安全结构；UI 明确呈现 empty/error，并保留重试。任何未知题型显示“未知类型”，不要渲染空标签。

### FE-005-14 [中][架构/CSS] DetailProblem 同时维护旧布局和 OjWorkbench 两套题面实现

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:1-246`
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:1-210`
- 问题：OJ 题进入 OjWorkbench，但 DetailProblem 仍保留包含题目描述、输入输出、提交记录、题解、代码编辑器和测试输入输出的旧布局；两套实现分别维护 tabs、Markdown section、提交记录和高度 CSS。
- 影响：同一个题目功能存在两套视觉和交互契约，修复题面内边距、结果展示、提交记录或响应式时很容易只修一条路径。旧分支中的 OJ 子组件现在大部分情况下不可达，却继续增加复杂度。
- 建议：把详情数据、答案/提交状态和题面 section 抽成无布局业务 composable/子组件，再由普通题型页和 OJ 工作台选择布局；删除已经不可达的重复 OJ 视图，避免继续用 deep CSS 给两套 DOM 打补丁。

### FE-005-15 [中][CSS] MarkdownPreview 被当作选项/段落内联内容使用，会产生嵌套卡片和非法布局

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:77-81,113-117`
  - `oj-vue/src/components/ProblemRadio/ProblemRadio.vue:3-11`
  - `oj-vue/src/components/ProblemResult/ProblemResult.vue:14-23`
- 问题：题目描述把 block 级的 MarkdownPreview 放进 `<p>`；选择项又把 `<div>` 预览包在 `<span>` 中。MarkdownPreview 当前默认带边框、较大 padding 和 article 风格，题型组件没有 compact 模式。
- 影响：浏览器会重新解析非法嵌套，导致段落 margin、选项垂直对齐和点击区域不稳定；每个选项/答案都像一个独立大卡片，题目页被大量空白和边框切碎，暗色模式下层级也不自然。
- 建议：MarkdownPreview 提供明确的 `article/inline/compact` variant；题目描述使用 section/div 容器，选项使用专门的 prose wrapper，不要把 block 组件放进 inline 元素。代码、公式和短文本的 spacing 应由题型容器控制。

### FE-005-16 [中][可访问性] 选择题选项和提交状态没有真正的键盘/语义控件

- 位置：
  - `oj-vue/src/components/ProblemRadio/ProblemRadio.vue:3-12,41-72`
  - `oj-vue/src/components/ProblemRadio/ProblemRadioGroup.vue:3-45`
  - `oj-vue/src/components/DetailProblem/FillBlank.vue:4-14`
- 问题：选择项是带 click 的普通 div，没有 `role="radio/checkbox"`、`tabindex`、`aria-checked` 或键盘事件；填空题表单没有 model/rules/稳定的 form-item key。`ProblemRadioGroup` 直接改变父级 answers 数组，选中状态只是视觉 class。
- 影响：键盘用户无法选择题目，读屏器也无法知道单选/多选状态；Vue 可能因为填空项没有 key 复用错误输入 DOM。后续加入校验、禁用或只读模式时，组件边界会继续混乱。
- 建议：使用真正的 radio/checkbox 语义或封装可访问的 option button；用不可变更新/明确事件传递答案；填空项使用稳定的 blankIndex key，并把校验、disabled、readonly 作为统一 props。

### FE-005-17 [中] 题型答案组件的状态更新方式重复且边界不清

- 位置：
  - `oj-vue/src/components/DetailProblem/FillBlank.vue:23-28`
  - `oj-vue/src/components/DetailProblem/ChoiceChoose.vue:27-39`
  - `oj-vue/src/components/ProblemRadio/ProblemRadioGroup.vue:28-45`
- 问题：填空直接 v-model 嵌套对象，单选通过清空并 push，多选通过 lodash.remove；三套组件没有共同的答案模型、空值策略或只读策略。`ChoiceChoose` 中的 `problemRadioGroupRef` 也没有使用。
- 影响：答案回写、切题保存、判题提交和题型切换都要适配不同的数组副作用；当后端返回顺序不同、选项被删除或用户快速点击时，结果排序和 dirty 比较可能不一致。
- 建议：建立按题型定义的 answer adapter：输入后端答案，输出规范化模型；更新时只向父组件 emit 新数组，排序/去重/校验集中处理。删除无效 ref 和每个题型自己的隐式副作用。

### FE-005-18 [低] 复制示例没有处理 Clipboard 失败，成功文案并不代表复制成功

- 位置：`oj-vue/src/components/DetailProblem/OnlineJudgeProblem.vue:34-42`、`CopyLink.vue:9-16`
- 问题：`navigator.clipboard.writeText` 返回 Promise，但调用方没有 await/catch；`CopyLink` 点击后立即显示“复制成功”，无论浏览器权限、非安全上下文或系统拒绝是否发生。
- 影响：HTTP 环境、权限限制或移动浏览器中，用户看到成功提示但剪贴板没有内容；失败 Promise 也可能出现在控制台。
- 建议：让复制工具返回 Promise<boolean>，只在 resolve 后展示成功状态，失败时给出手动复制提示；按钮组件统一处理恢复 timer、loading 和失败状态。

### FE-005-19 [低] 题目 API 和 debounce 包装重复，类型不能表达真实 JSON 契约

- 位置：
  - `oj-vue/src/api/problem/index.ts:32-41,78-127,194-308`
  - `oj-vue/src/api/problem/judge.ts:206-240`
- 问题：题目、答案、分页请求在多个 API 中重复 unwrap 和 debounce；很多函数声明日期为 `Date`，但 HTTP JSON 实际是字符串，Long ID 同时使用 string/number。多个 debounce API 只写 `then/finally`，没有统一 catch/cancel。
- 影响：题库、题目详情和判题页面对日期/ID/空 data 的处理不一致；调用方必须自己猜测接口失败时返回 null、抛错还是空数组。重复的 loading/error 处理也会继续制造按钮抖动和静默失败。
- 建议：按资源抽取 typed API client/normalizer，ID 统一字符串、日期统一 ISO 字符串；请求状态由 composable 管理，所有 debounce 都暴露 cancel 并收口 reject。不要让页面通过类型断言修补后端数据。

## 4. 做得较好的地方

- 题目类型已经拆成 OJ、填空、单选、多选组件，详情页和 OJ 工作台的基础职责方向是清楚的。
- 题库列表有分页、标签过滤、题目完成状态和 skeleton；Long ID 在题目选择器和活动侧栏中已经有截断意识。
- OJ 工作台已经使用有限高度、ResizeObserver、可折叠测试面板和独立状态提示，说明此前代码框无限延长问题已经有针对性治理基础。
- 题目公开列表后端明确排除了比赛题目，详情服务也对公开题目做了筛选；但空数据和错误响应仍需要在前后端契约上显式化。
- 多数新 CSS 使用 Element Plus 主题变量，基础暗色模式方向是正确的；主要问题是组件之间的密度和高度策略没有统一。

## 5. 建议修复顺序

1. 先修 FE-005-01、FE-005-02、FE-005-03、FE-005-04、FE-005-05、FE-005-06：修正筛选/标签状态、请求竞态、空题目状态和全局事件泄漏。
2. 修 FE-005-07、FE-005-08、FE-005-09、FE-005-10：收紧提交边界，统一答案与题型/难度 metadata，避免未知数据进入深层组件。
3. 修 FE-005-14、FE-005-15、FE-005-16、FE-005-17：收敛新旧详情布局，拆分 Markdown 密度，补题型组件语义和答案 adapter。
4. 修 FE-005-11、FE-005-12、FE-005-13、FE-005-18、FE-005-19：处理分页/表格响应式、列表错误态、复制反馈和 API 重复。
5. 补回归测试：重置筛选、标签接口慢/失败、快速切换两道题、题目 404/null、答案字段缺失、活动禁提、键盘选择题、窄屏 320～1000px、暗色 Markdown 公式、全屏/测试面板/Drawer 叠加。

## 6. 测试与验证

### 本次检查

- 逐行检查了 Problem、ProblemSet、ProblemList、ProblemListForm、DetailProblem、OjWorkbench 及各题型组件。
- 对照检查了题目/答案/判题 API、题目后端 Controller、Service、VO 和分页查询参数。
- 检查了 MarkdownPreview 在题面、选项、答案、公告和比赛工作台中的复用方式，以及相关 CSS 高度和滚动容器。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 本次 review 没有替代真实浏览器回归；请求竞态、标签异步渲染、题型键盘操作、全屏高度和窄屏溢出需要后续浏览器验证。
- 前端现有测试此前会在加载 Element Plus CSS 时因 `Unknown file extension ".css"` 失败，当前测试也没有覆盖题库筛选和题目展示流程。

## 7. 执行结果

已按本报告完成修复，主要包括：

- 题库筛选：重置会立即回到第一页并刷新；标签列表改为响应式数据，补充加载、失败、重试和未知标签占位状态。
- 题库列表：增加 loading/error/empty 三态；请求使用序号防止旧响应覆盖新查询；卸载时取消 debounce；Long ID、标签数量和窄屏表格布局统一处理。
- 题目详情：详情 API 对 `null`/缺失 `problemVo` 做明确错误归一化；后端题目不存在时返回 404；切题时清理旧状态，详情、答案、提交记录使用请求序号防止竞态回写。
- 题型组件：选择题改为按钮语义并支持键盘，答案采用不可变更新；填空题使用稳定 key、自动伸缩输入框和独立答案更新；结果展示增加安全空值处理。
- 题面与 OJ 工作台：Markdown 使用 compact/embedded 变体，避免 block 元素嵌套在段落或选项内；题型/难度使用统一 metadata 兜底；提交入口增加业务层禁用校验；复制样例处理 Clipboard 失败；resize 改为成对的 `addEventListener/removeEventListener`，不再污染全局属性。
- 后端：`GET /problem/{id}` 对不存在题目返回 `R.error404()`，避免 `success(null)` 继续流入前端。

验证结果：

- `pnpm run type-check` 通过。
- `pnpm run build` 通过；仅保留项目原有的动态导入和大 chunk 警告。
- `git diff --check` 通过。
- 使用 `/usr/lib/jvm/java-11-temurin-jdk/bin/java` 执行 `mvn -B -pl problem-service -am -DskipTests compile`，`commons`、`common-api`、`problem-service` 均 `BUILD SUCCESS`。

未提交 Git；工作区中的其他业务改动未清理、未回滚。
