# FE-006 提交代码与判题结果 Review

## 1. Review 范围

- 工作台：`oj-vue/src/components/OjWorkbench/OjWorkbench.vue`
- 代码编辑器：`oj-vue/src/components/EnhancedCodeEdior/index.vue`、`CodeEditor/CodeEditor.vue`
- 提交详情：`oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue`
- 判题状态：`oj-vue/src/components/JudgeStatusBadge/JudgeStatusBadge.vue`、`utils/problem/judgeStatus.ts`
- 可拖动分栏：`oj-vue/src/components/ResizablePanel/ResizablePanel.vue`
- 直接依赖：`api/problem/judge.ts`、`api/judge-log/index.ts`、`DetailProblem.vue` 里的提交/轮询状态
- 本次覆盖：提交快照与防重复、测试运行、HTTP 短轮询、状态机、结果/错误展示、代码编辑器配置、抽屉与分栏 CSS、暗色模式、响应式、可访问性、重复逻辑和 API 契约
- 本文先记录问题，再记录本次已执行的修复结果；未提交 Git，未清理工作区中的其他改动。

## 2. 执行结论

本次已完成 FE-006 的主要修复。提交代码、用户自定义测试和提交详情现在使用独立状态；判题状态先标准化再参与 UI 分支；轮询可以中止且有 deadline/失败上限；提交详情能够区分测试点加载失败、空结果和判题错误。剩余需要真实浏览器验证的内容主要是不同屏幕尺寸下的视觉细节，以及后端实际返回数据与历史记录的兼容性。

修复覆盖了原清单中的 FE-006-01～20：状态拆分、测试防重、轮询生命周期、状态/单位/格式化收敛、语言加载失败态、stdout/stderr 分离、详情请求竞态、响应式布局、拖动清理和键盘交互均已落地。前端锁的固定时间现在只作为网络/服务异常兜底，正常判题会在终态立即解锁；后端 Redis 锁仍是跨标签页/跨实例的最终兜底。

## 3. 问题清单

### FE-006-01 [高] 测试运行和正式提交共用 loading，异步结束会互相覆盖

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:328,531-607`
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:158-172,176-205`
- 问题：题目页只有一个 `useLoading` 实例。自定义测试和正式提交都调用 `loading()/finish()`，而工作台把同一个 `loading` 同时传给代码编辑器的“运行测试”和“提交代码”按钮。
- 影响：测试运行时提交按钮的 loading 状态和提交状态混在一起；测试请求结束可能把正式提交的 loading 提前结束，或正式提交结束后测试按钮停止 loading。两个操作的并发边界不清，用户无法判断当前到底是哪一个请求在运行。
- 建议：拆成 `testState` 和 `submitState`，分别管理按钮禁用、轮询、错误和终态；如果产品要求测试与正式提交互斥，在 handler 层统一拒绝另一类操作，不要只依赖按钮 loading。

### FE-006-02 [高] 自定义测试没有独立请求锁，重复点击会产生多次沙箱运行

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:525-553`
  - `oj-vue/src/components/EnhancedCodeEdior/index.vue:35-43`
- 问题：`submitTest` 每次点击都创建新 uuid、停止旧轮询并立即发起新请求，但没有检查测试请求是否已经发送/执行。按钮是否显示 loading 不等于 handler 层有锁；旧的 sendTest 请求即使还在网络中，也无法被取消。
- 影响：用户连续点击“运行测试”会同时创建多条沙箱任务，旧任务结果可能浪费判题机资源；当前页面只保留最后一个 uuid，旧任务完成后的资源和错误对用户不可见。
- 建议：测试操作使用独立的 requestId + AbortController/锁；新测试前明确取消旧任务，服务端也按 userId/uuid 做并发限制。测试的失败、取消、超时都要落到同一个可观察终态。

### FE-006-03 [高] 轮询没有最大期限，结果丢失时会永久请求

- 位置：`oj-vue/src/api/problem/judge.ts:142-204`
- 问题：`pollSubmission` 和 `pollTestResult` 只要没有 terminal status 就继续 setTimeout；网络异常还会无限退避到 5 秒，没有最大尝试次数、总等待时间、AbortSignal 或“结果不可用”的终态。
- 影响：提交记录丢失、Redis 结果过期、后端一直返回 null 或服务恢复但没有记录时，页面会长期显示判题中并持续发请求。用户切到后台后仍可能产生轮询流量，直到组件卸载。
- 建议：为每次轮询设置 deadline 和最大失败次数；超时后停止请求，展示“暂时无法获取结果”，并提供手动刷新。轮询 API 接受 AbortSignal，页面隐藏/卸载时立即取消在途请求，不能只停止下一次 timer。

### FE-006-04 [高] 组件卸载后，在途轮询请求返回仍可能更新已卸载状态

- 位置：
  - `oj-vue/src/api/problem/judge.ts:153-170,186-203`
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:756-761`
- 问题：`stop` 只设置布尔值并清理 timer，已经进入 await 的 HTTP 请求无法取消；请求 resolve 后，代码在再次检查 `stopped` 之前就执行 `onUpdate(data)`。组件卸载时因此仍有机会写入 `submitLogs/activeSubmission/testResult`。
- 影响：快速离开题目、切换比赛题目或关闭页面时，可能出现 Vue 卸载组件更新、旧结果污染新题目或浏览器控制台警告。
- 建议：轮询函数使用 AbortController 并在 response 落地前再次确认 stopped/requestId；所有 onUpdate 回调都带 submissionId/uuid，页面只接受当前活动请求的结果。

### FE-006-05 [高] 12 秒提交锁按时间解锁，不保证慢判题期间不重复提交

- 位置：
  - `oj-vue/src/components/DetailProblem/DetailProblem.vue:342-363,508-523,555-583`
  - 后端 Redis 提交锁配置及判题提交接口
- 问题：前端在 12 秒后无条件 `unlockOjSubmit`；后台 Redis 锁也有固定 TTL。即使这次提交仍为 QUEUE/RUNNING，前端和后端都可能重新允许下一次提交。
- 影响：判题机慢、MQ 堵塞或测试用例较多时，用户可以创建多个仍在运行的提交；提交记录和积分状态会变得难以解释。固定 TTL 虽能避免永久死锁，但不能同时表达“当前仍有活动提交”。
- 建议：把“活动提交 ID”作为服务端权威状态，只有 terminal/明确超时后才允许下一次；TTL 只做崩溃兜底。前端 12 秒到期时应显示可刷新/联系服务的状态，而不是静默解除业务锁。

### FE-006-06 [高] 判题状态存在多套映射，直接比较原始字符串会产生分支不一致

- 位置：
  - `oj-vue/src/api/problem/judge.ts:13-24,153-160,189-193`
  - `oj-vue/src/utils/problem/judgeStatus.ts:9-57`
  - `oj-vue/src/api/judge-log/index.ts:77-100`
  - `ojue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:152-178`
- 问题：`OJResult`、`STATUS_META`、judge-log 的 `judgeStatusOptions/statusMap` 都维护状态名称；轮询使用 exact includes，详情面板又直接比较 `OJResult.COMPILE_ERROR`，而 Badge 使用了能处理别名的统一工具。后端值既可能是 `AC`、`compiling`，也可能是 enum name。
- 影响：同一个结果在 toast、提交列表、详情抽屉和管理页面可能出现不同 label/颜色；新增状态或后端返回 enum name 时，Badge 能识别但轮询不终止、错误输出不显示。
- 建议：只保留一份标准化状态 registry，同时提供 `normalizeStatus/isPending/isTerminal/getMeta`；业务分支不要再直接比较原始字符串。用状态 fixture 覆盖后端 value、enum name、大小写和未知状态。

### FE-006-07 [高] 内存单位跨层不一致，用户看到的内存值很可能错误

- 位置：
  - `judge-server/src/main/java/com/anishan/judge/judge/impl/GradeSubmissionImpl.java:41-49`
  - `common-api/src/main/java/com/anishan/api/client/judgeserver/domain/JudgeScore.java:24-27,53-56`
  - `common-api/src/main/java/com/anishan/api/client/problem/domain/vo/SubmitLogVo.java:50-58`
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:316-322`
  - `oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:180-183`
- 问题：判题机结果转换为 `runResult.memory / (1024 * 1024)`，JudgeScore/前端数据却分别注释或处理成 KB/KiB/MiB；前端格式化逻辑把大于 1024 的值除以 1024，否则标记为 KiB。公开 VO 也把 memory 注释为 KB。
- 影响：例如后端已经保存为 MiB 的 256，前端会显示成 256 KiB；超过 1024 时又按 MiB 重新除一次。提交列表、toast、详情和测试点结果会出现单位误导。
- 建议：跨服务契约明确唯一单位，推荐 API 直接返回数值和 unit，或统一存/传 KiB；前端只在一个 formatter 中换算。补充 0、1、1024、1025、256MiB 和异常 null 的边界测试。

### FE-006-08 [高] CodeMirror 语言模式覆盖不完整，语言名称和 modeMap 绑定过紧

- 位置：`oj-vue/src/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue:58-81`、`index.vue:89-110`
- 问题：编辑器通过后端返回的显示名称查找 mode；modeMap 只覆盖少数精确名称，C++ 靠 `includes` 特判，其他新增语言会得到 undefined mode。`theme` 还从父组件传入但 CodeEditor 没有声明/使用，编辑器内部自己读取暗色模式。
- 影响：语言列表新增 C++ 20、Rust、JavaScript 或别名时，提交接口可能支持但语法高亮不工作；用户只看到代码颜色消失，不知道是编辑器配置还是判题语言问题。
- 建议：语言 API 返回稳定的 editorMode/editorKey，而不是前端猜显示名称；建立语言 registry，提交 languageId 与编辑器 mode 解耦。未知语言明确回退到纯文本并显示状态，不要让 undefined 静默进入 CodeMirror。

### FE-006-09 [中] 自动提示在每个 keypress 触发，性能和交互都不稳定

- 位置：`oj-vue/src/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue:119-126`
- 问题：`onReady` 给 CodeMirror 的每次 `keypress` 都调用 `showHint()`；没有按语言过滤，也没有 debounce、字符阈值或只在用户输入标识符时触发。所有 hint 插件都被导入，SQL hint 还配置了与 OJ 代码无关的示例表。
- 影响：长代码输入时反复创建提示菜单，可能造成卡顿、光标跳动和移动端遮挡；Java/C++/Go 用户会看到不相关的提示。自动提示失败也没有错误边界。
- 建议：改为明确的快捷键/可选自动提示策略，使用 inputRead + debounce，并按 editorMode 注册对应 hint；删除 SQL 示例配置和不使用的插件，避免基础代码编辑器承载所有语言的实验配置。

### FE-006-10 [中] CodeMirror 配置和事件存在死代码、重复 import 及错误键位定义

- 位置：`oj-vue/src/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue:20-48,89-116,129-142`、`EnhancedCodeEdior/index.vue:63,76-82`
- 问题：`clike.js`、`python.js` 被重复导入；`console.log(mode.value)` 留在生产代码；`extraKeys: {'Ctrl': 'autocomplete'}` 不是明确的 CodeMirror 组合键。父组件的 `codeEditorRef`、`theme` prop 和 `fullScreen` emit 没有形成有效使用链，父级监听的是 `full-screen`。
- 影响：构建体积和维护噪声增加；自动补全快捷键在不同浏览器/键盘下可能不生效；全屏或编辑器能力扩展时，事件命名不一致会造成“按钮点击无反应”。
- 建议：清理重复 import/console 和未使用 ref；定义稳定的事件名并由一层负责全屏；将 CodeMirror 配置拆成按语言的工厂，使用明确的 `Ctrl-Space`/`Cmd-Space` 等键位。

### FE-006-11 [中] 语言加载失败没有错误态，且多个编辑器实例可能重复请求

- 位置：
  - `oj-vue/src/components/EnhancedCodeEdior/index.vue:107-119`
  - `oj-vue/src/stores/useLanguage.ts:7-20`
- 问题：`initLanguages` 只处理 then，没有 catch/loading/error；store 用“数组为空”判断是否需要请求，在多个组件同时挂载时可能并发发送相同请求。
- 影响：语言接口失败时用户只能看到“选择语言/暂无可用语言”，无法重试；默认 languageId 仍可能被提交，直到后端提示不支持语言。
- 建议：语言 store 增加请求状态和 in-flight Promise 复用；编辑器提供重试/禁用提交策略，并在没有有效 languageId 时阻止 OJ 提交。

### FE-006-12 [中] 测试输出把 stdout 和 stderr 合并成一个字段，错误信息可能被吞掉

- 位置：`oj-vue/src/components/OjWorkbench/OjWorkbench.vue:192-204`、`DetailProblem.vue:539-549`
- 问题：工作台使用 `stdout || testResult?.stderr` 只显示一个流；当程序同时产生 stdout 和 stderr 时，stderr 被隐藏。旧布局也把 stderr 写入同一个结果区域，没有区分程序输出、运行错误和编译输出。
- 影响：用户可能看到“程序输出正常”却看不到关键警告/运行错误；调试结果和判题结果无法对应，尤其是 Java 编译/运行环境问题。
- 建议：测试结果模型分开展示 stdout、stderr、status 和 exit code；有 stderr 时显示独立错误区，并按终态区分编译、运行、超时和判题服务异常。不要用逻辑或运算符决定哪个诊断流可见。

### FE-006-13 [中] 提交详情测试点请求没有竞态保护，错误和空结果被混淆

- 位置：`oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:146-210`、`oj-vue/src/api/problem/judge.ts:122-125`
- 问题：抽屉打开/切换提交时直接调用 `loadCases`，没有请求序号或取消机制；A 提交的 cases 可能在 B 提交打开后覆盖 B。catch 只留下空数组，UI 将网络失败、无权限、暂无测试点都显示成同一段“暂无测试点明细”。
- 影响：用户查看提交记录时可能看到另一条提交的测试点；判题尚未写入日志或接口临时失败时，用户无法判断是结果还没生成还是加载失败。
- 建议：以 submitId 作为请求 token，落地前确认仍是当前 log；增加 cases 的 loading/empty/error/forbidden 状态和重试按钮。API 不要用 `data || []` 抹掉错误语义。

### FE-006-14 [中] 详情面板的错误输出展示策略不完整且依赖原始状态比较

- 位置：`oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:60-71,152-178`
- 问题：只有 CE 和 RE 会渲染 `stderr`；TLE、MLE 或其他可公开诊断的终态没有统一策略。判断使用 `props.log.status === OJResult...`，没有使用已经存在的规范化状态。
- 影响：用户看到 TLE/MLE 只有状态标签，没有任何可以帮助定位的公开输出；后端一旦返回 enum name 而非 value，连 CE 的编译输出也可能不显示。
- 建议：由状态 registry 定义每个终态的用户可见内容策略；判题机内部错误继续隐藏，但编译/运行错误的公开 stderr 可按权限和状态显示。所有判断先 normalize。

### FE-006-15 [中][CSS] 工作台存在多层固定高度和滚动容器，窄屏/全屏容易产生双滚动或裁剪

- 位置：
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:369-401,663-718`
  - `oj-vue/src/components/ResizablePanel/ResizablePanel.vue:102-120`
  - `oj-vue/src/components/EnhancedCodeEdior/CodeEditor/CodeEditor.vue:146-168`
- 问题：工作台固定 `height/max-height: var(--in-main-content-height)`，全屏改为 fixed，活动场景又改为 height:100%；编辑器 host、ResizablePanel、CodeMirror 各自设置 height:100%、flex、min/max-height 和 overflow。760px 以下再把外层改为 auto，但内部面板仍按百分比计算。
- 影响：浏览器高度变化、顶部导航高度变化、测试面板展开或拖动分隔线时，代码区可能被裁剪、出现两层滚动条或继续撑高页面；全屏时页面主体和代码编辑器滚动容器也可能抢滚轮。
- 建议：明确唯一高度所有者：工作台负责可用视口高度，ResizablePanel 负责分配比例，CodeMirror 只填充分配到的区域；中间 flex 层统一 `min-height:0`，只保留一个内容滚动容器。窄屏单列应使用独立的 panel height token，而不是外层 auto + 内层百分比反馈。

### FE-006-16 [中][CSS] 编辑器 toolbar 在中间宽度突然换行，按钮与语言选择缺少稳定布局

- 位置：`oj-vue/src/components/EnhancedCodeEdior/index.vue:126-150`
- 问题：只在 900px 处把 toolbar 整体改为纵向；在 901px 附近语言选择、三个操作按钮和右侧 padding 可能互相挤压，而在 900px 以下又让按钮全部靠右。语言 popover 固定 360px，未根据编辑器可用宽度设置最大宽度。
- 影响：不同侧栏宽度/浏览器缩放下，按钮会换行或抖动；移动端语言弹层可能超出可读宽度。操作按钮的 loading icon 变化也可能改变文字与按钮宽度。
- 建议：以 toolbar 容器宽度而非 viewport 设置断点，给 actions 使用固定按钮 token 和 flex-wrap；popover 使用 `min(360px, calc(100vw - 24px))`，并为 loading 状态预留 icon 宽度。

### FE-006-17 [中][CSS/可访问性] 状态 toast 和移动端操作按钮缺少完整交互语义

- 位置：
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:113-153`
  - `oj-vue/src/components/ResizablePanel/ResizablePanel.vue:11-25`
- 问题：判题 toast 是带 click 的普通 div，只有 `role="status"`，终态可以点击打开详情却没有 button 语义、tabindex 或键盘事件；移动端 CSS 隐藏操作按钮文字，但这些按钮没有 aria-label。ResizablePanel 的 separator 也没有 tabindex、aria-valuenow/min/max 和键盘调节。
- 影响：键盘用户无法打开提交详情、判断焦点位置或调整面板；屏幕阅读器读不到“全屏/记录/打开测试”等隐藏文字后的操作含义。
- 建议：终态 toast 使用按钮或补齐 role/button/键盘行为；所有只显示图标的按钮补 aria-label；分隔条实现 Arrow/Home/End 键调整并同步 aria 值，拖动只是鼠标增强。

### FE-006-18 [中] ResizablePanel 的 pointer 事件生命周期和不同用途仍耦合

- 位置：`oj-vue/src/components/ResizablePanel/ResizablePanel.vue:66-95,102-129`
- 问题：拖动监听器绑定在 handle 自身，清理依赖 pointerup/pointercancel；组件没有统一的 lostpointercapture/unmount 清理。组件同时支持横向活动侧栏和纵向编辑器测试面板，只靠 variant CSS 隔离，尺寸语义却都用百分比。
- 影响：指针离开、系统手势中断或快速卸载时，可能残留 dragging 状态；不同页面对 min/max、恢复按钮和滚动容器的要求不一致，继续修改一个 variant 可能影响另一个。
- 建议：抽取纯拖动 composable，监听 pointer capture/lostpointercapture，并在卸载时清理；面板组件提供明确的 axis/size unit/restore placement 配置，视觉 CSS 与交互逻辑分离。

### FE-006-19 [低] 提交详情抽屉在移动端和长内容下缺少更细的布局保护

- 位置：`oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:213-269`
- 问题：只在 640px 以下把指标和测试点改成单列；长题目名、长语言名、Long submitId 和错误输出没有统一的 min-width/截断策略，header heading 也没有显式允许收缩。代码只限制了 max-height，没有最大请求体/复制反馈之外的内容策略。
- 影响：640～980px 的抽屉中指标卡和测试点卡片可能过窄；长 ID/语言会把关闭按钮挤出；错误日志和源代码长行会产生横向滚动，用户需要在多个滚动容器间切换。
- 建议：提交详情按内容宽度设置断点，header 使用 minmax(0,1fr) 并允许标题截断；代码/错误输出统一提供横向滚动和复制入口，测试点在小屏改为紧凑列表。

### FE-006-20 [低] 状态、内存和日期格式化逻辑在多个组件重复

- 位置：
  - `oj-vue/src/components/OjWorkbench/OjWorkbench.vue:314-322`
  - `oj-vue/src/components/SubmissionDetailPanel/SubmissionDetailPanel.vue:180-183`
  - `oj-vue/src/api/judge-log/index.ts:77-100`
  - `oj-vue/src/utils/problem/judgeStatus.ts:48-57`
- 问题：工作台和提交详情各自实现 `formatMemory`、`formatDate`，状态 metadata 又在 API 和 utility 重复实现。
- 影响：同一条提交在 toast、列表和抽屉里可能显示不同单位/日期精度/未知状态文案；修复一个页面不会自动修复其它页面。
- 建议：抽取一个只依赖数据契约的 judge display module，统一状态、时间、内存、测试点计数格式；组件只负责布局和交互，不再复制格式化函数。

## 4. 做得较好的地方

- 判题结果已经提供了公开用户可见的提交详情、源代码、测试点结果和错误输出，后端对普通用户隐藏了 caseId、测试数据和判题机内部错误，权限边界方向是正确的。
- 轮询使用递归 setTimeout 而不是固定 setInterval，网络异常时有有限退避；页面卸载也尝试停止轮询，基础生命周期意识比旧版 SSE 更清晰。
- OjWorkbench 将题面、提交记录、题解、代码编辑器和自定义测试拆成区域，并用 ResizablePanel 管理测试区比例，布局职责已经有抽象基础。
- JudgeStatusBadge 和 judgeStatus utility 已经为 AC、WA、RE、TLE、MLE、CE、JUDGE_ERROR 提供了颜色和短码，用户能快速识别结果。
- 提交详情抽屉对源代码使用等宽字体、对错误输出和测试点使用独立区块，暗色模式主要依赖主题变量，视觉方向是合理的。

## 5. 建议修复顺序

1. 先修 FE-006-01、FE-006-02、FE-006-03、FE-006-04、FE-006-05：拆分测试/提交状态，增加取消、deadline 和活动提交权威状态。
2. 修 FE-006-06、FE-006-07、FE-006-12、FE-006-13、FE-006-14：统一状态/单位契约，补齐错误流和提交详情的请求状态。
3. 修 FE-006-08、FE-006-09、FE-006-10、FE-006-11：重构语言 registry 和 CodeMirror 配置，清理死代码、重复 import 和失败态。
4. 修 FE-006-15、FE-006-16、FE-006-17、FE-006-18、FE-006-19：收敛高度所有权、拖动生命周期、弹层/toolbar 响应式和键盘交互。
5. 最后处理 FE-006-20，并补测试：重复测试/提交、切后台/卸载、轮询超时、慢判题超过 12 秒、所有 status value/alias、内存单位边界、stderr+stdout 同时存在、长代码/长错误、320～1000px 和暗色模式。

## 6. 测试与验证

### 本次检查

- 逐行检查并修改了 OjWorkbench、EnhancedCodeEditor、CodeEditor、SubmissionDetailPanel、JudgeStatusBadge、ResizablePanel 和判题 API。
- 对照统一了题目页的提交/测试状态、HTTP 轮询接口、公开提交 VO、测试点脱敏接口和 judge-server 的时间/内存转换注释。
- 检查了前台题目页、比赛题目页、后台答题详情和提交详情抽屉对编辑器/面板的复用，保留旧版 `loading` 属性兼容后台答题详情。
- 清理了 CodeMirror 重复语言导入、无效提示配置和旧的固定轮询实现；新增共享的判题展示格式化工具。

### 已执行验证

- `oj-vue`: `pnpm run type-check` 通过。
- `oj-vue`: `pnpm run build` 通过；仅保留项目已有的动态/静态路由分包提示和大 chunk 警告。
- Java 11 `/usr/lib/jvm/java-11-temurin-jdk/bin/java`：`mvn -B -pl problem-service -am -DskipTests compile` 通过。
- Java 11：`mvn -B -pl judge-server -am -DskipTests compile` 通过。
- `git diff --check` 通过。

### 后续人工验证

- 需要浏览器实际验证慢判题、切换题目/卸载、移动端抽屉、暗色模式、CodeMirror 提示、分栏拖动和屏幕阅读器行为。
- 当前项目既有前端测试没有覆盖提交/测试并发和轮询生命周期；后续可补充状态归一化、轮询 fake timer、详情请求竞态和内存单位边界测试。

## 7. 本次修改内容

- 提交/测试分别使用 `submitLoading`、`testLoading` 和独立错误/轮询状态；测试按钮在请求和轮询期间统一防重。
- `pollSubmission`、`pollTestResult` 使用 AbortController、失败上限和超时兜底；组件卸载或切题时停止轮询，旧响应不会污染当前题目。
- 判题状态统一由 `normalizeJudgeStatus`、`isPendingJudgeStatus`、`isTerminalJudgeStatus` 和共享展示格式化工具处理。
- 提交详情支持测试点请求错误与重试，错误输出覆盖 CE/RE/TLE/MLE/判题异常，stdout 与 stderr 分开展示。
- CodeMirror 语言列表增加请求复用、错误重试、稳定的语言模式解析和防抖提示；保留旧编辑器调用方的 `loading` 兼容属性。
- 工作台/测试区/分栏补充 `min-width/min-height`、全屏单列布局、窄屏按钮布局、分隔条键盘操作和拖动监听清理。
- 后端 JudgeScore、SubmitLogVo 和 sandbox 结果转换注释统一为 MiB 口径；未改变已上线数据的数值存储格式。
- 未提交 Git，未修改数据库，未操作部署环境。
