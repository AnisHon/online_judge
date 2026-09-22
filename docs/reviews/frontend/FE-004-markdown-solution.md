# FE-004 Markdown 编辑器与题解编辑 Review

## 1. Review 范围

- 编辑器：`oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue`
- 预览：`oj-vue/src/components/MarkdownPreview.vue`
- 题解编辑：`oj-vue/src/views/solutions/edit-solution/EditSolution.vue`
- 直接依赖：题解 API、图片上传 API、文件大小工具、ProblemPicker、题解后端接口与 DTO
- 本次覆盖：编辑/预览状态同步、图片上传、异步竞态、空数据与权限、Markdown 安全、CSS 尺寸/层级/暗色模式、响应式、重复逻辑和可维护性
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

当前 Markdown 链路基本能完成“编辑、上传图片、选择题目、提交题解”，但编辑器和题解提交之间缺少一个完整的状态边界，主要风险集中在：

1. 图片大小换算实际按 GB 计算，5MB 限制几乎失效；上传失败或校验失败时也没有可靠地结束编辑器上传回调。
2. `EditSolution` 异步初始化没有 loading/error 状态，路由复用时不会重新加载；用户可能在默认模板还没被真实数据覆盖前提交，或者把上一个题解内容提交到下一个题解。
3. 提交 API 用四套重复的 lodash debounce 包装请求，既没有 catch/cancel，也把响应式表单对象原样延迟传递；组件卸载后仍可能发请求。
4. `MarkdownPreview` 创建的 MarkdownIt 实例没有传给 `MdPreview`，KaTeX、HTML、linkify 等配置实际上是死代码；所有预览实例还共用同一个 DOM id。
5. 预览没有显式的 Markdown 清洗策略，且外链 KaTeX 版本与 md-editor-v3 内部版本不一致；这既是内容安全边界缺失，也是离线/CSP/暗色样式不稳定来源。
6. 编辑器和预览使用全局样式及硬编码 z-index，和 Element Plus 弹层、账号菜单、全屏编辑器互相竞争；编辑页的父级高度控制还会被编辑器的内联 500px 高度覆盖。

## 3. 问题清单

### FE-004-01 [高] 图片大小换算错误，Markdown 图片上传的 5MB 限制实际近似失效

- 位置：
  - `oj-vue/src/utils/file/index.ts:12-18`
  - `oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue:48-60`
  - `content-service/src/main/resources/application.yml`（服务端 multipart 限制）
- 问题：工具函数注释和常量都声明单位是 MB，但实现返回 `byte / (1024 * 1024 * 1024)`，实际是 GB。编辑器再拿这个结果和 `MAX_IMAGE_SIZE = 5` 比较。
- 影响：客户端会放行远大于 5MB 的图片，直到请求到服务端才被拒绝；用户看到的前端校验与后端 20MB 限制不一致，大文件还会先占用浏览器内存和网络。它也会让“图片上传失败”看起来像 MinIO 或网关故障。
- 建议：统一用一个明确命名的 `bytesToMegabytes` 或直接按字节比较，避免单位在函数和常量之间隐式转换；前端限制应小于服务端硬限制，并对超限返回可识别的业务错误。补充 0、5MB、5MB+1byte、20MB 边界测试。

### FE-004-02 [高] 图片上传失败时没有调用 md-editor 回调，也没有 catch，编辑器可能卡在上传状态

- 位置：`oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue:48-66`
- 问题：数量或大小校验失败直接 `return`，上传请求没有 `try/catch/finally`。当 `uploadImages` 抛异常时，`callback` 永远不会调用；即使后端返回空值或返回数量与文件数量不一致，也会继续拼接 URL 并访问 `files[index].name`。
- 影响：编辑器的图片上传 UI 可能一直显示上传中，控制台出现未处理 Promise；用户无法得到稳定的失败提示。部分上传成功、部分失败时，图片和文件名也可能错位。
- 建议：把上传流程收口为成功/失败两条明确路径：校验失败和请求失败都结束编辑器上传回调状态，并保留可见错误提示；校验 `urls` 是非空数组、长度和文件映射关系，单个文件失败时不要生成假 URL。最好让 `uploadImages` 返回带原文件索引和错误的结果，而不是依赖数组下标。

### FE-004-03 [高] 题解初始化是未等待的异步副作用，页面可能在数据回填前提交

- 位置：`oj-vue/src/views/solutions/edit-solution/EditSolution.vue:169-208`
- 问题：文件底部直接调用 `fillForm()`，没有 `await`、初始化 loading 或错误状态。编辑模式下，真实题解和题目名称尚未回来时，表单已经可编辑，提交按钮也没有被禁用。请求失败后 `fillForm` 可能产生未处理 rejection，页面仍保留默认模板。
- 影响：用户可能把空的/默认模板提交到后端；编辑接口也可能因为 `solutionId` 尚未回填而发出错误请求。网络慢时，页面看起来像正常的“编辑题解”，实际编辑的是未初始化表单。
- 建议：建立 `idle/loading/success/not-found/error` 初始化状态；编辑模式必须在题解数据成功回填后才允许提交，失败时提供重试和明确的不存在/无权限提示。不要让全局 HTTP 提示代替页面级状态。

### FE-004-04 [高] 路由参数变化不会重新加载题解，组件复用时存在串题解风险

- 位置：
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:99-134,201-208`
  - 编辑题解路由配置
- 问题：`isAdd` 是基于 `route.query.solutionId` 的 computed，但 `fillForm` 只在 setup 时调用，没有 watch route query，也没有重置 `form` 和 `selectedProblem`。Vue Router 复用同一组件实例切换 `solutionId` 时，旧内容会继续留在页面。
- 影响：从题解 A 切到题解 B 后，标题、正文、关联题目可能仍显示 A；保存时却用 B 的 ID，产生严重的数据覆盖风险。新增模式切换到编辑模式时也可能残留新增页模板。
- 建议：把默认表单抽成工厂函数，在 `solutionId/problemId` 变化时取消旧请求、重置表单和选中题目，再重新初始化；用请求序号或 AbortController 防止旧响应回填新路由。路由切换期间禁用提交。

### FE-004-05 [高] 用 solution:list 推断“管理员编辑”权限，导致权限职责混用和潜在 403

- 位置：
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:101-102,178-182,185-194`
  - `oj-vue/src/components/ProblemPicker/ProblemPicker.vue:92,105-117`
  - `oj-vue/src/api/solution/index.ts:37-45,57-111`
- 问题：`isAdmin = hasPerm('problem:solution:list')` 同时决定使用管理员查询题解、管理员新增/修改接口和管理员题目查询。后端却分别要求 solution:list、solution:add、solution:edit，以及 problem:list 等权限；“能查看列表”不等于“能新增/修改”。
- 影响：拥有只读管理权限的用户可能被导向管理员写接口并收到 403；有编辑权限但没有列表权限的用户又可能走普通接口，表现不稳定。权限变化在组件挂载后刷新时也不会重新计算。
- 建议：按操作拆分 capability：读取当前题解、普通本人编辑、管理员新增、管理员编辑、管理员题目选择分别判断；前端只决定体验，后端继续做最终鉴权。权限状态应来自响应式 store/computed，而不是 setup 时一次性读取。

### FE-004-06 [高] Markdown 正文在 el-form 外，空正文不会被前端校验

- 位置：
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:18-61,137-140`
  - `problem-service/.../DetailSolutionDto.java`
- 问题：`el-form` 在第 54 行结束，`MarkDownEditor` 在表单外；rules 只校验 title 和 problemId，没有 content 规则。后端 DTO 对 content 有非空约束。
- 影响：用户删除正文后点击发布，前端认为校验通过，随后收到后端校验错误；错误信息依赖全局 HTTP 层，页面没有把焦点带回编辑器。默认模板也可能被误认为“已有有效内容”。
- 建议：将编辑器纳入同一表单或在提交前显式校验 Markdown 的有效文本；给编辑器容器提供错误态和可聚焦入口。标题、题目、正文校验规则和后端约束应从同一份契约派生。

### FE-004-07 [中] 题解 API 对 data:null 和非法 ID 没有归一化，后端还可能先 NPE

- 位置：
  - `oj-vue/src/api/solution/index.ts:37-45`
  - `problem-service/src/main/java/com/anishan/problem/service/impl/SolutionExplanationServiceImpl.java:71-117`
- 问题：前端 API 直接返回 `data`，类型却声明为非空 `Solution`。普通后端接口对不存在/不可访问题解可能返回 `success(null)`；服务端 `doGet` 在判断查询结果为空前就读取 `solution.getUserId()`，非法 ID 可能直接变成 500。
- 影响：编辑页会在 `Object.assign` 或后续字段访问处出现空数据异常；用户看到的是通用错误，而不是题解不存在/无权访问。前端类型检查也无法帮助发现这个契约问题。
- 建议：API 层统一把空 data 转成明确的 NotFound/Forbidden 错误；后端先判断 solution 是否存在，再查用户信息和正文。编辑页根据 404、403、网络错误分别展示，不要把 null 当正常题解。

### FE-004-08 [中] 四套 debounce 提交实现重复且没有异常收口、取消和请求锁

- 位置：`oj-vue/src/api/solution/index.ts:57-117`
- 问题：普通/管理员新增/编辑分别复制一套 lodash debounce。每套只写 `.then(success).finally(final)`，没有 `.catch`；返回的 debounce 实例也没有暴露给页面做 `cancel`。页面把响应式 `form` 对象原样传入，延迟的一秒内表单改变，最终发送的内容也会改变。
- 影响：请求失败可能形成未处理 Promise；离开页面后延迟任务仍可能发请求并回调 `router.back()`；用户快速点击或键盘触发时，loading 状态和真实请求状态容易不同步。所谓 debounce 还是固定延迟，并没有清晰表达“提交锁”。
- 建议：抽一个带 endpoint/权限参数的提交函数，提交时立即生成结构化快照；用显式 `isSubmitting`/请求锁防重复，成功/失败/结束统一收口；组件卸载时取消 pending task。若保留 debounce，必须暴露 cancel，并且所有 reject 都由调用方可控地处理。

### FE-004-09 [中] MarkdownIt、KaTeX、MdCatalog 和 baseURL 是死代码，预览没有使用预期配置

- 位置：`oj-vue/src/components/MarkdownPreview.vue:15-26,48-53`
- 问题：组件创建了 `new MarkdownIt` 并注册 `markdown-it-katex`，但没有把 `md` 传给 `MdPreview`；`MdCatalog` 和 `baseURL` 也没有使用。实际渲染仍由 md-editor-v3 自己的解析器和插件配置决定。
- 影响：维护者以为已经开启 HTML、linkify、typographer、KaTeX，实际行为可能完全不同；后续修改这段 dead code 不会改变页面，造成排查误导和重复依赖。
- 建议：明确只保留一种 Markdown 渲染管线：要么使用 md-editor-v3 的统一配置并删除 MarkdownIt/mk，要么真正把 parser/plugin 注入预览组件，并为编辑器和预览共享同一配置。不要在 wrapper 内保留不会生效的“备用实现”。

### FE-004-10 [中][安全] 预览没有显式 Markdown 清洗策略，不应假设用户内容已安全

- 位置：`oj-vue/src/components/MarkdownPreview.vue:1-10`
- 问题：`MdPreview` 没有显式传入 sanitize/URL 过滤策略。当前组件用于题解、公告、题目描述等可由用户或管理端录入的内容；md-editor-v3 的默认 sanitize 配置不能替代项目对 HTML、链接协议、图片来源和嵌入内容的安全决策。
- 影响：一旦未来打开 HTML、iframe 或自定义插件，内容安全边界会随依赖默认值变化；恶意链接、外链资源、HTML 注入和 CSP 行为也没有统一规则。这里不等同于已确认存在 XSS，但当前 wrapper 没有提供可审计的安全策略。
- 建议：在后端和前端明确一套 Markdown 白名单/URL 协议策略；对用户生成内容默认关闭原始 HTML 和任意 iframe，必要的图片走受控资源域名；把 sanitize 配置封装成共享模块并增加恶意 HTML、javascript URL、外链图片的测试。

### FE-004-11 [中] 所有 MarkdownPreview 实例使用相同 DOM id

- 位置：`oj-vue/src/components/MarkdownPreview.vue:28-34`
- 问题：`const id = 'preview-only'` 是固定值，而预览组件在题目详情、题解详情、公告、比赛概况等页面会被多次挂载；同一页面可能同时出现多个预览实例。
- 影响：依赖 id 的目录、滚动定位、编辑器内部插件或浏览器 DOM 查询会命中错误实例；问题通常只在一个页面同时有多个题目区块时出现，难以复现。
- 建议：通过 prop 传入稳定且唯一的 id，或者用 `useId`/递增生成器按组件实例生成；如果不使用目录功能，则不要伪造一个全局固定 id。

### FE-004-12 [中][CSS] 编辑器内联 500px 高度会覆盖 EditSolution 的 flex 高度，容易溢出

- 位置：
  - `oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue:3-6,25-27`
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:61` 及 `.editor` 样式
- 问题：MarkDownEditor 始终给内部 md-editor 设置内联 `height: 500px`；题解页又试图用 `.editor :deep(.md-editor) { height: 100%; }` 让编辑器占满 flex 容器，但普通 CSS 无法覆盖内联 height。编辑器外层没有明确的 `min-height: 0/overflow` 组合。
- 影响：题解页、弹窗和窄屏布局会出现固定高度、双滚动条或内容把卡片撑开；当页面改成一列或进入全屏时，父级高度和编辑器高度可能互相放大。这与此前代码框无限延长、弹窗内容超出的问题属于同一类高度所有权冲突。
- 建议：明确组件的高度策略：由 MarkDownEditor 提供 `height`/flex 模式二选一，不要由父级 deep 覆盖内部 inline style；flex 模式下所有中间层都设置 `min-height: 0`，滚动只由编辑器内容区承担。为页面、弹窗、全屏分别定义 variant，而不是依靠 `height:100%` 猜测。

### FE-004-13 [中][CSS] 编辑器和预览硬编码 z-index，可能再次覆盖或被覆盖 Element Plus 浮层

- 位置：
  - `oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue:98-107`
  - `oj-vue/src/components/MarkdownPreview.vue:86-87`
  - `oj-vue/src/components/AccountMenu/AccountMenu.vue:73-75`
- 问题：编辑器把 md-editor 的 dropdown、modal、fullscreen 强行设为 2500/2501，预览代码头也使用 `!important`；账号菜单又固定为 3000，而 md-editor-v3 本身有自己的高层级默认值。各组件不共享层级 token，也不区分挂载到 body 还是局部容器。
- 影响：抽屉、Dialog、题目选择弹框、账号菜单和编辑器工具栏会根据打开顺序互相覆盖；修改一个组件的 z-index 只能把冲突转移到另一个组件，难以保证后台和前台一致。
- 建议：统一使用 Element Plus 的层级管理和项目级浮层 token；编辑器弹层应使用 append-to-body/teleport 策略并只配置一个明确层级。删除局部 `!important` 补丁，至少建立“页面内容 < 卡片 < Drawer < Dialog < 全局菜单”的可审计层级表。

### FE-004-14 [中][CSS] 文章预览和题目短文本共用一套大容器，造成重复边框、过大内边距和滚动条

- 位置：`oj-vue/src/components/MarkdownPreview.vue:67-88`
- 问题：所有使用 MarkdownPreview 的场景都得到同一个边框、16px 圆角、`clamp(22px, 4vw, 48px)` 内边距和至少 500px iframe 高度；题解正文、题目输入输出、公告片段和比赛概况的内容密度完全不同，但组件没有 compact/article/embedded 变体。
- 影响：短内容被撑成大块空白，页面需要重复写 deep CSS 才能“压回去”；嵌入 iframe 自带 `resize: both` 和 500px 最小高度，容易出现用户不可预期的滚动条和可拖动区域。
- 建议：把容器视觉和内容密度拆成明确 variant/token：长文章才使用阅读宽度和大内边距，题面片段使用紧凑模式；默认不要给所有 iframe 开启 resize/min-height，嵌入能力应单独受控。代码块、长 URL、公式的换行策略也应分别设置，不要在整个 preview 根节点上统一 `overflow-wrap:anywhere`。

### FE-004-15 [中][CSS/依赖] 外链 KaTeX 0.5.1 与 md-editor-v3 内部 KaTeX 版本不一致

- 位置：`oj-vue/src/components/MarkdownPreview.vue:62-65`
- 问题：组件通过 CDN 引入 KaTeX 0.5.1；当前 md-editor-v3 依赖链使用的是更新版本。样式版本、字体资源和渲染结构可能不匹配，而且页面运行依赖外网。
- 影响：生产环境 CSP、内网、离线部署或 CDN 失败时公式样式缺失；不同页面可能出现公式字体、间距、暗色颜色不一致，升级 md-editor-v3 后又可能被旧 CDN 样式覆盖。
- 建议：不要在组件样式中运行时引入旧 CDN；统一使用锁定版本的本地依赖或 md-editor-v3 提供的扩展样式，并纳入构建产物和 CSP 校验。公式渲染应至少覆盖浅色/暗色、长公式、移动端溢出测试。

### FE-004-16 [中] 前后端标题长度不一致，题解正文也没有统一内容上限

- 位置：
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:20`
  - `problem-service/.../DetailSolutionDto.java`
- 问题：前端 `maxlength="100"`，后端 DTO 标题最大长度为 50；正文没有前端字符/字节上限，也没有保存前的大小提示。Markdown 中的代码、图片 URL 和 HTML 会显著增加请求体。
- 影响：用户输入 51～100 个字符时，前端显示可提交，后端才拒绝；大正文或重复粘贴图片可能触发网关/服务端请求体限制，错误提示不聚焦到具体字段。
- 建议：从后端契约生成或共享标题上限；正文在编辑器层提示合理上限并在 API 层做请求体保护。长度判断要明确是 Unicode 字符、UTF-8 字节还是数据库字段长度。

### FE-004-17 [低] 题目 ID 截断、日期/Markdown 配置和 API 操作存在重复实现

- 位置：
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:142-145`
  - `oj-vue/src/components/ProblemPicker/ProblemPicker.vue:98-101`
  - `oj-vue/src/api/solution/index.ts:57-117`
- 问题：题解编辑页和 ProblemPicker 各自实现相同的 `shortId`；普通/管理员四套提交函数重复同一流程；预览又单独维护一套 Markdown 配置和 KaTeX 引入。
- 影响：ID 截断规则、错误处理、loading 和提交行为变更时需要多处同步，容易出现一处修复另一处仍有旧行为；这类重复也是此前“组件样式改一处、另一个页面崩掉”的根源之一。
- 建议：优先复用项目已有通用 ID/日期格式化工具；抽取一个受 endpoint/权限策略控制的 solution mutation composable；编辑器和预览共享一个 Markdown 配置模块，但不要把页面业务状态塞进基础组件。

### FE-004-18 [低][CSS/可访问性] 编辑器和题解操作缺少统一焦点、减少动效及窄屏策略

- 位置：
  - `oj-vue/src/components/MarkDownEditor/MarkDownEditor.vue:72-108`
  - `oj-vue/src/views/solutions/edit-solution/EditSolution.vue:211-488`
  - `oj-vue/src/components/ProblemPicker/ProblemPicker.vue:135-164`
- 问题：样式大量依赖 page-level deep selector、固定网格最小列宽和全局 md-editor 选择器；没有看到统一的 `:focus-visible`、`prefers-reduced-motion` 和编辑器/题目选择弹窗的窄屏降级策略。ProblemPicker 的筛选网格在中间宽度也较依赖固定列。
- 影响：键盘用户难以确认当前编辑/选择位置；减少动效设置不生效；720px 附近或弹窗宽度较小时，筛选栏和编辑器会挤压，页面级补丁继续增加。
- 建议：基础编辑器只负责结构和 token，页面通过 variant 设置尺寸；统一交互 focus token 和减少动效规则；ProblemPicker 使用内容最小宽度断点或容器查询，编辑器弹窗在窄屏改为单列并明确滚动容器。

## 4. 做得较好的地方

- MarkDownEditor 已封装主题、预览主题和代码主题，能够跟随暗色模式，且图片上传路径没有散落在各个题解页面。
- 编辑题解使用 ProblemPicker 选择题目，回填时同时保留题目名称和可截断的 Long ID，避免让用户手填题目 ID；选择器也提供搜索、分页和刷新入口。
- 普通题解的更新接口在后端服务层有归属校验，管理员接口有单独的权限注解；前端问题主要是权限分支选择过于粗糙，不能替代后端鉴权。
- 编辑页已经有 `min-width: 0`、暗色 CSS 变量、卡片化结构和全屏入口的基础，后续适合通过统一编辑器 variant 收口，而不是继续添加页面级覆盖。

## 5. 建议修复顺序

1. 先修 FE-004-01、FE-004-02、FE-004-03、FE-004-04：修正图片限制，补齐上传失败回调，建立题解初始化/路由切换状态机。
2. 修 FE-004-05、FE-004-06、FE-004-07、FE-004-08：拆分权限能力，校验正文，统一 null/错误契约，替换重复 debounce 提交。
3. 修 FE-004-09、FE-004-10、FE-004-11、FE-004-15：统一 Markdown 渲染配置，明确清洗策略，移除重复 ID 和外链 KaTeX 版本冲突。
4. 修 FE-004-12、FE-004-13、FE-004-14、FE-004-18：重新定义编辑器高度、浮层层级和 preview 密度，补齐窄屏/暗色/键盘回归。
5. 最后处理 FE-004-16、FE-004-17，并补测试：5MB 边界、上传 4xx/空 data/部分失败、切换两个 solutionId、初始化失败、空正文、无 solution:list 权限、同页多个预览、Drawer/Dialog/全屏编辑器叠加。

## 6. 测试与验证

### 本次检查

- 逐行检查了 MarkDownEditor、MarkdownPreview、EditSolution、ProblemPicker 及题解/文件/题目 API。
- 对照检查了题解后端 Controller、Service、DetailSolutionDto 校验约束和文件服务的 multipart 限制。
- 对照检查了 md-editor-v3 当前依赖的预览配置、sanitize 能力、默认层级和 KaTeX 依赖。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 本次 review 没有替代真实浏览器回归；上传失败回调、同页多预览、弹层叠加、窄屏编辑器和暗色公式需要后续浏览器验证。
- 现有前端测试此前会在加载 Element Plus CSS 时因 `Unknown file extension ".css"` 失败，且当前测试没有覆盖题解编辑/Markdown 上传流程。

## 7. 执行结果

本次已按上述优先级执行修复：

- 图片大小改为按字节校验，修复 MB/GB 换算，并确保校验失败、网络失败和无效响应都会结束编辑器上传回调。
- `EditSolution` 增加初始化 loading/error、路由 query 变化重置与序列号竞态保护；提交使用快照和显式请求锁，标题上限与后端 50 字符一致，并增加正文非空校验。
- 题解 API 去除重复的四套 debounce 包装，普通/管理员读写能力分开判断；删除题解改为可等待的直接请求，避免离开详情页后请求仍在后台执行。
- Markdown 编辑器增加 fixed/flex 高度模式；预览支持 article/compact/embedded 三种密度，实例 ID 唯一并移除外链 KaTeX 和局部硬编码 z-index。
- 编辑器和预览复用 `sanitizeMarkdownHtml`，统一过滤 HTML、危险链接协议和嵌入内容；前端新增直接依赖 `xss@1.0.15`。
- 后端题解详情查询对不存在的题解、缺失用户信息和缺失正文做空值兜底；新增/修改接口不再把所有异常误报成“题目不存在”。

验证结果：

- `pnpm run type-check` 通过。
- `pnpm run build` 通过；仅保留项目原有动态/静态导入和大 chunk 警告。
- 使用 Java 17 Maven 容器执行 `mvn -B -pl problem-service -am -DskipTests compile` 通过。
- 本机 Maven 使用 JDK 25 仍会触发既有 Lombok/Java 11 构建兼容问题，未修改本机 Java 环境或项目全局编译配置。
