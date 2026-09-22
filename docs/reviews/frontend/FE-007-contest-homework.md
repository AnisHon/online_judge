# FE-007 竞赛/作业前台 Review

## 1. Review 范围

- 活动列表：`oj-vue/src/components/activity/ActivityList.vue`
- 比赛入口：`oj-vue/src/views/contest/Contest.vue`
- 作业入口：`oj-vue/src/views/homework/Homework.vue`
- 活动做题页：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue`
- 活动侧栏拖动：`oj-vue/src/components/ActivityResizablePanel/ActivityResizablePanel.vue`
- 活动 API/工具：`oj-vue/src/api/contest/index.ts`、`oj-vue/src/api/list/problem.ts`、`oj-vue/src/utils/contest/index.ts`
- 活动页复用的做题容器：`oj-vue/src/components/DetailProblem/DetailProblem.vue`
- 本次覆盖：加入/进入竞赛流程、请求竞态、活动状态、交卷边界、题目完成度、路由切换、时间显示、CSS 高度/滚动/响应式、暗色模式、可访问性和重复逻辑。
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

当前比赛和作业前台已经抽出了共用的 `ActivityList` 和 `ActivityResizablePanel`，视觉上也有活动卡片、活动概况、题目导航和暗色变量。但业务状态仍然分散在多个 debounce 回调和模板条件里，存在几处会直接导致“进入了错误活动、活动状态不更新、交卷错误被误报、题目加载失败看起来像没有题目”的问题。

最需要优先处理的是加入流程的共享可变状态、活动详情页的路由/请求生命周期，以及时间和失败状态。其次再收敛活动列表与活动做题页的 CSS 高度所有权，补齐移动端侧栏和拖动控件的键盘语义。

## 3. 问题清单

### FE-007-01 [高] 加入流程使用共享 currentActivity/form，快速点击时可能进入错误活动

- 位置：`oj-vue/src/components/activity/ActivityList.vue:86-115`、`oj-vue/src/api/contest/index.ts:85-119`
- 问题：`currentActivity` 和 `form` 是整个列表唯一的一份状态；`debouncedIsJoined` 的回调不接收本次请求对应的活动，而是从当前的 `currentActivity` 读取活动。若活动 A 的 `isJoined` 请求已经发出，用户又点击活动 B，A 的响应回来后会用 B 的信息执行 `enter` 或 `join`。`join` 还会读取已经被改成 B 的共享 `form.contestId`。
- 影响：用户可能被带入错误活动，或者向 B 发送原本针对 A 的加入操作。这类问题只在快速操作或网络延迟时出现，难以通过正常点击复现。
- 建议：把活动 ID绑定在一次完整的进入意图中，回调使用闭包快照或返回 `{activity, joined}`，不要从全局 currentActivity 推断响应归属。加入请求直接接收不可变 request，不要让 API wrapper 持有可变 reactive form。

### FE-007-02 [高] 加入/密码确认没有可靠的进行中锁，存在重复请求和状态覆盖

- 位置：`oj-vue/src/components/activity/ActivityList.vue:15,45-55,90-116`
- 问题：列表 loading 只包含 `isLoading || isJoinedLoading`，没有包含 `debouncedJoin` 返回的 `isLoading`；密码确认按钮没有 `loading` 或 `disabled`，`submit` 也没有校验当前请求状态。用户连点确认、按 Enter 或重复触发加入时，可以创建多个请求；加入期间还可能继续操作其它活动。
- 影响：重复加入、通知重复弹出、`passwordDialog` 被旧响应关闭，甚至与 FE-007-01 的共享状态竞态叠加，形成错误跳转。
- 建议：每次点击创建带 activityId 的 action state，只锁定当前活动；加入按钮在请求期间固定宽度并禁用，统一处理 Enter 提交。对同一 activityId 合并 in-flight Promise，完成后再清理。

### FE-007-03 [高] 活动列表请求失败没有页面级错误态，失败和空列表被混淆

- 位置：`oj-vue/src/components/activity/ActivityList.vue:15-40,90-123`、`oj-vue/src/api/contest/index.ts:157-170`
- 问题：列表只有 `isLoading`、数组和 `total`，没有 `error` 状态、重试入口或请求快照。请求失败时全局 HTTP 层可能弹通知，但组件自身既不清空旧数据，也不显示“本次加载失败”；首次失败会直接落到 `list.length === 0` 的“还没有任何比赛/作业”。debounce 包装还没有暴露 cancel 或请求序号。
- 影响：网络错误、403、后端暂时不可用都会被用户理解为“没有活动”；旧分页数据也可能与当前筛选/页码同时存在。排查时只能依赖浏览器控制台或全局通知。
- 建议：活动列表建立 `idle/loading/success/empty/error` 状态，错误态提供重试；按页码和 kind 生成请求快照，响应落地前校验快照仍有效，并在卸载时取消 debounce。全局通知不能替代页面自己的错误反馈。

### FE-007-04 [高] 题目列表 API 把所有失败转换为空数组，活动页无法区分故障和无题

- 位置：`oj-vue/src/api/list/problem.ts:13-21`、`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:40-55,153-155`
- 问题：`debouncedGetProblems` 在 catch 中调用 `success([])`，活动页随后正常替换 `problemList`，模板只会显示“暂无题目”。接口鉴权失败、网络断开、服务错误和真正的空题单全部走同一条路径。
- 影响：用户无法知道是活动没有配置题目，还是题目服务挂了；更严重的是，空数组会把原有题目清掉，用户可能误以为活动内容被删除。
- 建议：API 层保留失败语义，组件增加题目列表 error 状态和重试；只有后端明确返回空数组时才显示空状态。若活动已进入做题状态，刷新失败时应保留旧列表并标注“刷新失败”。

### FE-007-05 [高] 活动详情把三个独立请求用 Promise.all 绑定，状态接口失败会吞掉活动信息

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:174-186`
- 问题：活动详情、活动可答状态和题目列表是三个独立数据源，但 `fetchContestById` 与 `getContestStatus` 被放在同一个 `Promise.all`。只要状态接口失败，catch 就把 `contest` 设为 undefined；题目列表又是另一条未等待的 debounce 请求，页面会同时出现活动加载失败和题目列表状态不确定。
- 影响：状态接口短暂超时不会影响活动基本信息的展示；反过来，活动详情成功但状态失败时用户看不到活动说明，也没有明确知道是“不能答题”还是“信息加载失败”。
- 建议：拆成独立的资源状态：活动信息成功即可展示概况，答题权限失败时只禁用提交并显示状态错误，题目列表独立显示 loading/error。不要用 Promise.all 把可降级的数据绑定成全失败。

### FE-007-06 [高] 同一活动 ID通过路由变化时不会可靠重载页面

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:142-217`
- 问题：`contestId` 是 route params 的 computed，但只在 setup 末尾调用一次 `loadPage()`；没有 watch `route.params.id`，也没有在活动 ID变化时清空 `contest`、`problemList`、`currentRow`、侧栏状态并重新加载。Vue Router 在同一命名路由之间切换参数时通常会复用组件实例。
- 影响：从活动 A 直接跳到活动 B 时，页面可能继续显示 A 的标题、题目和作答状态；用户点击题目后提交的 contestId 可能已经是 B，视觉内容却仍然是 A，属于高风险的数据错配。
- 建议：以 route param 为唯一资源 key，watch `contestId` 并执行可取消的 reload；切换开始先清理旧资源和活动状态，所有异步响应带活动 ID 校验。若业务允许，路由上使用 `:key="route.params.id"` 也只能作为兜底，不能代替请求竞态保护。

### FE-007-07 [高] 交卷异常会被统一显示成“已取消提交”，真实失败原因被隐藏

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:203-215`、`oj-vue/src/api/contest/index.ts:199-202`
- 问题：交卷 Promise 的 `.catch()` 无条件执行 `ElMessage.info('已取消提交')`，用户点击取消、接口失败、权限失效、网络断开都会显示同一文案。`handInPaper` 还通过 `resultNotify` 处理业务结果，调用方没有明确区分成功、用户取消和服务异常。
- 影响：交卷失败会被用户误解为自己取消，无法判断试卷是否已经提交；如果服务端已经提交但后续刷新失败，页面也没有可靠的状态确认。重试可能造成重复操作或更大困惑。
- 建议：只捕获 `ElMessageBox` 的取消分支并显示取消提示；接口异常沿用统一错误处理或显示“交卷失败，请重试”。交卷接口应返回明确的幂等结果，成功后刷新活动状态和题目完成度，失败时保留当前题面。

### FE-007-08 [中] 交卷按钮没有独立提交状态，也没有在空题目/加载状态下阻止操作

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:59-64,174-186,203-215`
- 问题：提交试卷按钮只依赖 `isAnswering`，没有 `isHandingIn`、题目加载完成或题目数量条件。确认框关闭后，在请求完成前按钮仍然可点击；`refreshActivityState` 失败时，代码仍然在 then 链中无条件将 `isAnswering` 改为 false。
- 影响：慢网络下可以重复打开交卷确认框；题目列表尚未加载或加载失败时也可能提交空试卷。状态接口刷新失败会让页面进入不可提交状态，但没有说明服务端究竟是否已经完成交卷。
- 建议：增加交卷状态机和幂等锁：确认后立即锁定，成功/明确失败才解锁；提交前要求活动信息和题目列表处于可用状态。刷新失败时不要用猜测覆盖状态，展示“提交已发送，活动状态待确认”并提供刷新。

### FE-007-09 [中] 活动时间只在渲染时计算，没有响应式时钟，页面停留期间状态会过期

- 位置：`oj-vue/src/components/activity/ActivityList.vue:34-35,117-118`、`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:161-167`、`oj-vue/src/utils/contest/index.ts:28-55`
- 问题：`isNotStart`、`isContestOver` 和 `activityStatus` 读取 `dayjs()`/当前时间，但没有任何 reactive tick。一个活动打开时处于“未开始”，即使到开始时间，按钮和标签也不会自动变为“进入活动”；同样，进行中的活动到结束时间也不会自动变为结束态。
- 影响：用户必须刷新页面或触发其它响应式更新才能进入活动；活动结束后前端仍可能显示可作答，虽然后端可能拒绝。比赛倒计时/状态在前台和详情页之间也会不一致。
- 建议：抽取统一的活动时间状态 composable，按分钟或按最近边界设置 timer，并在页面卸载时清理；提交前仍必须由后端做最终校验。展示时间和权限状态不要由多个组件各自调用 dayjs 推断。

### FE-007-10 [中] 时间格式和非法日期没有统一兜底，活动列表可能展示 Invalid Date

- 位置：`oj-vue/src/utils/contest/index.ts:28-55`、`oj-vue/src/components/activity/ActivityList.vue:26-30`、`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:169-173`
- 问题：`formatDate` 直接格式化输入，没有校验有效性；`differ` 对非法时间返回“不足1小时”，`isContestOver/isNotStart` 对 invalid dayjs 比较得到的 NaN 也不会进入明确的错误状态。列表没有区分后端时间缺失、格式错误和真实的短活动。
- 影响：后端数据异常时，用户可能看到 `Invalid Date`、错误的“未开始”或“不足1小时”，并能继续点击进入；运维无法从 UI 识别时间字段问题。
- 建议：统一返回 `{valid, text, start, end}` 的时间 view model，非法时间进入活动卡片错误态并禁用进入；明确时区契约，所有页面共用同一 formatter 和边界测试。

### FE-007-11 [中] 活动列表、详情和作业入口通过 kind 条件堆叠逻辑，职责边界正在分叉

- 位置：`oj-vue/src/components/activity/ActivityList.vue:3-39,82-118`、`oj-vue/src/views/contest/Contest.vue:1-5`、`oj-vue/src/views/homework/Homework.vue:1-5`
- 问题：比赛和作业共用一个组件是正确方向，但模板和脚本里大量重复 `kind === 'CONTEST' ? ... : ...`，文案、图标、空态、通知、按钮提示和头部主题各自判断。随着作业和比赛的权限/提交规则出现差异，条件会继续散落，当前入口组件又只是薄包装，无法承载清晰配置。
- 影响：改比赛视觉或行为时容易漏改作业；后续加入活动类型、状态策略、反馈策略时，单个列表组件会变成条件分支中心。当前“复用”并没有形成稳定的活动 metadata 模型。
- 建议：定义 `ActivityKindConfig`/metadata registry，集中提供标题、图标、主题、文案、加入策略和状态策略；`ActivityList` 只消费配置。比赛/作业差异较大的交互应通过策略或子组件注入，而不是继续增加模板三元表达式。

### FE-007-12 [中][CSS] ContestProblems 的样式存在重复规则和死选择器，后续修复容易出现覆盖顺序问题

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:306-351,552-597`
- 问题：`.summary-actions`、`.activity-identity`、`.back-button`、`.hide-button`、`.summary-heading h3` 等规则重复出现；模板中对应的是 `h2`，并没有 `summary-heading h3`。同一组规则后置重复会让维护者误以为有不同断点或状态覆盖。
- 影响：调整侧栏间距、按钮字号或活动标题时，可能只修改了第一组规则，实际生效的值来自第二组；死选择器和重复 CSS 也会掩盖真正的布局问题，增加暗色/窄屏回归成本。
- 建议：删除重复块和死选择器，按“基础结构—状态—响应式”组织样式；把活动摘要、题目卡片、提交栏抽成可复用的局部样式 token，避免继续在大文件末尾追加覆盖。

### FE-007-13 [中][CSS] 活动做题页在窄屏仍保持 20%～36% 侧栏，无法形成真正的移动端布局

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:3-4,796-816`、`oj-vue/src/components/ActivityResizablePanel/ActivityResizablePanel.vue:108-177`
- 问题：`ContestProblems` 在 760px 以下只调整外层高度和概况 padding，没有调整 `ActivityResizablePanel` 的横向结构、最小侧栏比例或侧栏抽屉行为。组件仍按百分比给侧栏分配 20%～36%，同时主区域还要容纳题面 header 和工作台。
- 影响：手机/窄窗口下题目侧栏和正文同时变窄，题目标题、提交按钮和代码编辑器容易被裁剪；用户隐藏侧栏后恢复按钮又占用正文左侧固定 40px，可能遮挡题面内容。此前项目出现的“面板水平/高度崩坏”问题在这个复用组件上仍有复现条件。
- 建议：在窄屏切换为“正文全宽 + 侧栏抽屉/底部面板”，桌面拖动与移动端抽屉使用不同 layout mode；恢复按钮作为不遮挡内容的浮层，并依据安全区和焦点管理定位。不要只改 workspace 高度。

### FE-007-14 [中][CSS] 活动详情存在多层高度所有者，题目工作台容易被裁剪或产生双滚动

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:221-229,599-671`、`oj-vue/src/components/ActivityResizablePanel/ActivityResizablePanel.vue:108-168`、`oj-vue/src/components/DetailProblem/DetailProblem.vue:27-31,182-201`
- 问题：外层使用 `var(--in-main-content-height)`，面板和主区继续设置 `height:100%/max-height:100%`，DetailProblem 又给活动场景的 scrollbar 和 editor column 设 100%/固定内容高度。概况、题面、代码区、测试区分别拥有自己的 overflow 语义。
- 影响：浏览器高度变化、导航栏高度变化、测试区展开或全屏切换时，最内层内容可能被截断；题面、工作台和页面各出现一个滚动条，滚轮焦点不明确。用户已经反馈过比赛/作业面板高度和滚动条方向崩坏，这套高度链仍然比较脆弱。
- 建议：活动页只负责可用 viewport 高度，ActivityResizablePanel 负责横向分配，DetailProblem/OjWorkbench 负责内部纵向滚动，明确只保留一个纵向滚动容器。所有 flex 中间层补 `min-height:0/min-width:0`，用 ResizeObserver 或尺寸 token 传递真实空间，而不是多层 100% 互相反馈。

### FE-007-15 [中][CSS] 顶部题目栏在长标题和窄宽度下会挤出“活动概览”按钮

- 位置：`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:68-80,610-638`
- 问题：`.problem-stage__bar` 使用左右 `justify-content: space-between`，`.stage-heading` 没有 `min-width:0`，标题只设置 `max-width` 和 ellipsis，没有给整个标题组设置可收缩区域。活动概览按钮也没有固定的 `flex:0 0 auto` 或 aria label。
- 影响：长活动/题目标题、ID 较长或侧栏收缩后，按钮可能被挤压、换行或超出可视区域；CSS 的 text-overflow 不会在 flex item 的默认最小宽度下自动解决溢出。
- 建议：标题组设置 `min-width:0; flex:1`，标题和 ID 分别允许截断，操作区固定尺寸；窄屏隐藏非必要 ID 时仍保留完整 title tooltip。为“活动概览”提供明确 aria-label 和键盘焦点样式。

### FE-007-16 [中][CSS/可访问性] 拖动分隔条只有鼠标交互，没有键盘和状态语义

- 位置：`oj-vue/src/components/ActivityResizablePanel/ActivityResizablePanel.vue:12-22,73-100`
- 问题：分隔条虽有 `role="separator"` 和方向，但没有 `tabindex`、`aria-valuenow/min/max`，也没有 Arrow/Home/End 键调节。pointer listener 绑定在 handle 本身，只有 pointerup/pointercancel 清理，没有 `lostpointercapture` 和卸载清理。
- 影响：键盘用户无法调整侧栏；指针被系统手势打断或组件卸载时，dragging 可能保持异常，下一次进入活动时仍有拖动样式或残留 listener。
- 建议：把拖动逻辑抽成带清理的 composable，监听 pointer capture 丢失和 onUnmounted；分隔条实现可访问的数值语义和键盘步进，并把 collapsed/restore 状态通过 `aria-expanded` 暴露。

### FE-007-17 [低] 活动列表卡片的移动端内容没有统一的收缩/换行策略

- 位置：`oj-vue/src/components/activity/ActivityList.vue:16-37,194-250,283-329`
- 问题：桌面卡片使用 icon、主内容和 action 三列，标题 `white-space: nowrap`；移动端只让 body `flex-wrap`，但主内容、badge、日期范围、权限 tag 和按钮没有明确的 min-width/overflow 策略。日期范围与时长仍可能在同一行争夺空间。
- 影响：中等宽度或浏览器字体放大时，标题/加入人数徽标、权限标签和进入按钮会互相挤压；长标题可能把卡片高度推高，操作区视觉位置不稳定。暗色模式虽然变量基本可用，但 hover transform 与边框变化在低对比场景下不够明确。
- 建议：卡片使用明确的 grid areas，主标题 `min-width:0`，badge 有可读 label；时间区拆成可换行的 start/end 与 duration，移动端将 action 独占一行并固定按钮尺寸。补充 320/375/768px 和系统字体放大测试。

### FE-007-18 [低] 活动卡片和侧栏存在不完整的可访问性语义

- 位置：`oj-vue/src/components/activity/ActivityList.vue:16-39`、`oj-vue/src/views/contest/ContestProblems/ContestProblems.vue:41-53`
- 问题：活动卡片本身不是可操作元素，只有内部按钮可点击；活动标题/时间/权限没有关联 label。题目导航按钮缺少当前题目的 `aria-current`，完成/错误状态只有图标没有文本，隐藏侧栏按钮只依赖 `title`。
- 影响：读屏器用户很难理解活动卡片的层级和题目完成状态；图标加载失败或颜色不足时，AC/错误状态信息丢失。键盘用户虽然可以到达题目按钮，但得到的状态上下文不完整。
- 建议：使用清晰的 heading/description 关联，题目按钮补 `aria-current`、`aria-label` 和状态文本；完成状态除颜色/图标外提供隐藏文本。所有图标按钮使用 `aria-label`，title 只作为补充。

### FE-007-19 [低] 活动工具函数和导入存在重复，展示规则难以全局演进

- 位置：`oj-vue/src/components/activity/ActivityList.vue:73-74`、`oj-vue/src/api/contest/index.ts:157-180`、`oj-vue/src/utils/contest/index.ts:3-55`
- 问题：`ActivityList` 从同一个模块分两次导入工具函数；比赛/作业列表和详情页各自组合活动状态、日期和文案。API 中加入/是否加入/列表请求分别封装成多个相似 debounce wrapper，但没有统一的错误、取消和请求状态契约。
- 影响：修复活动时间、错误通知、loading 或重复提交时需要改多个 wrapper；导入和条件重复本身不一定立刻报错，却说明共享边界没有稳定下来，后续模块会继续复制相同逻辑。
- 建议：统一 activity API client 和 async state composable，提供 `loadList/join/checkAccess/submit` 的标准状态；工具函数按领域一次导入，活动文案和状态 metadata 集中管理。

### FE-007-20 [低] 活动列表空态在加载初期会闪现，且没有保留/刷新策略

- 位置：`oj-vue/src/components/activity/ActivityList.vue:15-40`
- 问题：`el-empty` 只判断 `list.length === 0`，没有同时判断 `!isLoading`。首次请求尚未返回时，空态会与 loading 遮罩一起渲染；翻页/刷新时如果先清空数据，也会闪出“还没有任何比赛/作业”。
- 影响：用户会看到内容在“暂无活动”和真实列表之间闪烁，尤其是网络较慢时；刷新过程中清空旧列表也会造成页面跳动。
- 建议：区分初次加载、刷新加载、成功空数据和错误；初次加载使用 skeleton，刷新时保留旧列表并显示轻量 loading，只有成功返回空数组才显示空态。

## 4. 做得较好的地方

- 比赛与作业入口已经复用 `ActivityList`，活动做题页也抽出了 `ActivityResizablePanel`，避免了完全复制两套页面。
- 活动题目侧栏把当前得分、完成数、题目顺序和题目标题放在同一信息层级，`finish/correct` 也有图标和颜色反馈，信息架构方向是合理的。
- 活动概况、题目导航和题面区域在暗色模式下主要使用 Element Plus 主题变量，概况 Markdown 也尝试移除重复边框，整体主题适配基础不错。
- 活动时间长度已经按年、月、日、小时拆分，且题目 ID 做了短显示；这为后续统一 ID/时间展示组件提供了基础。
- 交卷后会刷新题目列表和活动状态，并且 `DetailProblem` 通过 `submitted` 事件通知外层，这个事件边界比父组件直接操作题目内部状态更容易维护。

## 5. 建议修复顺序

1. 先修 FE-007-01、FE-007-02、FE-007-03、FE-007-04、FE-007-05、FE-007-06：消除加入/加载竞态，保留错误语义，补齐路由参数变化和资源级状态。
2. 修 FE-007-07、FE-007-08、FE-007-09、FE-007-10：重构交卷状态机，区分取消/失败/成功，统一时间状态和非法数据处理。
3. 修 FE-007-13、FE-007-14、FE-007-15、FE-007-16：收敛活动页高度所有权，重做窄屏侧栏/抽屉和拖动控件生命周期。
4. 修 FE-007-11、FE-007-12、FE-007-17、FE-007-18、FE-007-19、FE-007-20：抽取活动 metadata、清理重复 CSS，补充移动端、键盘和空态体验。
5. 补浏览器回归：快速连续点击两个活动、慢网络/403/接口失败、同路由切换不同活动 ID、活动开始/结束边界、交卷重复点击、320～1440px、暗色模式、键盘拖动和屏幕阅读器。

## 6. 测试与验证

### 本次检查

- 逐行检查了 `ActivityList`、`ContestProblems`、`ActivityResizablePanel`、比赛/作业入口、活动 API、题单题目 API 和活动时间工具。
- 检查了活动列表加入、私有密码、白名单、题目导航、活动概况、交卷刷新和 DetailProblem 的活动复用路径。
- 覆盖了 CSS 重复、固定高度、滚动容器、窄屏布局、暗色变量、键盘/读屏语义以及 debounce 请求生命周期。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 本次 review 没有替代真实浏览器回归；竞态、慢网络、路由复用、时间跨越、窄屏侧栏和拖动中断需要后续浏览器验证。
- 当前报告阶段尚未执行前端类型检查；后续会单独运行 `pnpm run type-check` 和 `git diff --check`，并确认只新增本报告。

## 7. 本次未修改内容

- 未修改比赛/作业列表、活动做题页、拖动面板、活动 API、题目 API 或后端代码。
- 未提交 Git。
- 仅新增本 review 报告。
