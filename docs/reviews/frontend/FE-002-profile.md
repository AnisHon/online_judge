# FE-002 前端个人主页 Review

## 1. Review 范围

- 页面：`oj-vue/src/views/profile/Profile.vue`
- 活动组件：`oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue`
- Profile API：`oj-vue/src/api/profile/index.ts`
- 直接契约：`user-service` 的公开资料接口、`problem-service` 的个人活动接口及其缓存/查询实现
- 本次覆盖：TypeScript/Vue 逻辑、异步状态、权限边界、API 数据契约、CSS/响应式/暗色模式、重复实现和可维护性
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

个人主页的整体分层已经比把所有内容堆在一个页面里清晰，但当前实现仍有几个需要优先处理的风险：

1. 个人主页切换用户或快速连续进入页面时，没有请求取消/版本校验，旧请求可能覆盖新用户资料；加载失败也没有页面级错误状态。
2. 公开题解是否包含私有内容由 `user-id` 请求头决定，组件又直接渲染 API 返回的全部题解，隐私边界缺少前端防线，服务端身份来源也和请求头强耦合。
3. Profile API 对 `data: null`、缺字段和列表为空没有统一归一化，页面多处直接解构/访问 `.length`，会再次产生此前遇到的 `data is null` 或运行时异常。
4. 验证码刷新被当作普通副作用调用，没有错误处理；密码修改成功后刷新验证码失败，会误报“密码更新失败”，且验证码 Token 清理不完整。
5. 主页、活动组件和其他前端模块重复实现 `shortId`、日期格式化、列表项结构和错误转换，规则已经出现不一致。
6. CSS 只有一个 780px 主断点，侧栏网格、`height: 100%` 与 `Transition mode="out-in"` 组合存在中间宽度挤压、切换跳高和过渡溢出的风险；按钮也缺少键盘焦点样式。

## 3. 问题清单

### FE-002-01 [高] 加载请求没有取消或版本校验，旧资料可能覆盖当前用户

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:241-257`
  - `oj-vue/src/views/profile/Profile.vue:336-341`
- 问题：`load` 直接读取当前路由参数并在请求完成后写入 `profile`，没有 AbortController、请求序号或当前 ID 校验。路由复用时，用户快速从 A 主页切到 B 主页，A 的慢请求可能晚于 B 返回并覆盖 B；`onMounted(load)`、保存资料后的 `load()`、头像上传后的 `load()` 也会制造并发请求。
- 影响：页面标题、头像、活动列表和 owner 状态可能短暂或持续显示成另一个用户；`profileForm` 也会被旧响应回填。这个问题尤其容易和“偶尔显示不对”的前端状态问题混淆。
- 建议：封装 profile loader，至少使用递增 requestId，在响应落地前确认 requestId 和 route userId 仍一致；如果 HTTP 层支持取消，应在路由变化时取消上一请求。保存/上传后优先局部更新或复用同一刷新队列，不要无条件并发整页 reload。

### FE-002-02 [高][隐私] 私有题解展示依赖请求头和后端过滤，组件没有防御性过滤

- 位置：
  - `problem-service/src/main/java/com/anishan/problem/controller/ProfileController.java:27-32`
  - `problem-service/src/main/java/com/anishan/problem/service/impl/ProfileServiceImpl.java:33-62`
  - `problem-service/src/main/resources/mapper/ProfileMapper.xml:46-60`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:27-35`
- 问题：后端通过 `viewerId != null && viewerId.equals(userId)` 判断是否本人，并把该结果传给 `includePrivate`；前端活动组件收到 `activity.solutions` 后无条件渲染全部项，只是用“仅自己可见”标签标记私有题解。当前网关会由 JWT 重写 `user-id`，但 ProfileController 本身仍把请求头当成身份判断来源；前端也没有在非 owner 场景过滤 `private_`。
- 影响：只要下游服务被绕过网关、请求头注入链路配置错误，或后端未来返回了未过滤的数据，公开主页就可能展示私有题解标题、题目和时间。前端显示一个标签不等于隐私保护。
- 建议：后端使用已认证的 `SecurityContext`/`AuthUtil` 身份判断 owner，不要直接信任业务请求头；公开接口默认只返回公开题解，私有题解单独走“本人活动”分支。前端也应根据 `activity.owner` 对列表做防御性过滤，并把“公开活动”和“本人活动”定义成明确的 API 类型。

### FE-002-03 [高] Profile API 没有响应归一化，空数据会在页面深处直接抛异常

- 位置：
  - `oj-vue/src/api/profile/index.ts:41-49`
  - `oj-vue/src/views/profile/Profile.vue:38-53,246-253`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:16-35`
- 问题：`getUserProfile`、`getProfileActivity` 直接返回 `data`，没有验证 `data` 是否存在，也没有把 `contests`、`solutions`、`solvedProblems` 的空值归一化。页面随后直接访问 `data.user`、`profile.activity.contests.length`，子组件直接访问 `activity.contests.length` 和 `activity.solutions.length`。
- 影响：HTTP 200 但 `data: null`、后端部分字段缺失、缓存中存在旧结构时，错误会变成 `Cannot read properties of null/undefined`，用户看到的是空白页或通用错误，真正的 API 错误上下文丢失。这与认证模块中已发现的 `data is null` 属于同类问题。
- 建议：在 API 层定义 `ProfileData` 并统一 `unwrap/normalize`：缺少根对象直接抛带接口名的 `ApiError`；列表统一为空数组，分组统一包含 `easy/medium/hard/unknown`；页面只接收归一化后的不可变数据。

### FE-002-04 [高] 页面没有 profile 加载失败状态，旧数据可能与 loading 叠加

- 位置：`oj-vue/src/views/profile/Profile.vue:241-257`
- 问题：`load` 只有 `finally`，没有 `catch` 和错误状态；加载新用户时也没有先清空旧 `profile`。模板在 `v-loading` 的同时仍然渲染旧内容，失败后只会依赖 HTTP 层的旁路提示，`el-empty` 也无法区分“用户不存在”和“网络/服务异常”。
- 影响：用户切换个人主页时会看到旧用户内容覆盖在 loading 上；请求失败后页面可能保留旧主页，造成严重的错误归属感。HTTP 层若已弹过错误，页面又没有可重试入口。
- 建议：增加 `loadState: idle/loading/success/not-found/error` 和页面级重试；切换 userId 时先清理旧数据或标记为 stale；对 404 与 5xx 分开呈现。不要依赖全局 HTTP 拦截器承担页面状态。

### FE-002-05 [中] 验证码刷新是未捕获的副作用，错误处理会互相覆盖

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:286-290,338-340`
  - `oj-vue/src/views/profile/Profile.vue:301-303,318-320`
- 问题：`watch(activeTab)` 中直接调用异步 `refreshCaptchaCode()`，没有 `await/void catch`；发送验证码和修改密码的 `catch` 中又 `await refreshCaptchaCode()`。刷新失败时会覆盖原始发送/修改错误，刷新成功失败也没有 loading/error 状态。验证码请求返回前还可能被多次触发，旧响应可以覆盖新 token。
- 影响：浏览器出现未处理 Promise rejection；验证码图片空白但 token 仍可能是旧值；密码已经修改成功时，如果随后刷新验证码失败，会进入 `catch` 并提示“密码更新失败”，实际结果与用户看到的提示相反。
- 建议：把验证码抽成带 `loading/error/requestId` 的 composable；先清空旧 token，只有图片和 token 成对返回时才替换；业务请求的结果提示与验证码刷新解耦，刷新失败只提示“验证码刷新失败”，不能改变已经完成的业务结果。

### FE-002-06 [中] 密码表单清理不完整，且缺少提交后的表单状态重置

- 位置：`oj-vue/src/views/profile/Profile.vue:308-323`
- 问题：密码成功后 `Object.assign` 清理了密码、重复密码、图形码和邮箱码，但没有显式清理 `captchaToken`；同时没有调用 `resetFields/clearValidate`，Element Plus 可能保留上一次的校验状态。`resetPassword` 成功后再刷新验证码也让成功路径依赖非关键请求。
- 影响：旧验证码 token 可能在下一次发送验证码时被复用；用户刚清空表单却仍看到旧校验红字。验证码刷新异常会让成功业务进入错误提示路径。
- 建议：密码提交成功后原子清理所有验证码字段和校验状态；验证码 token 由 composable 内部管理；主操作成功后独立启动验证码刷新，不把刷新结果串到“修改密码成功/失败”的 try-catch 中。

### FE-002-07 [中] 保存资料和上传头像都会整页重新请求，容易重复、闪烁并误判结果

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:262-271`
  - `oj-vue/src/views/profile/Profile.vue:325-333`
  - `oj-vue/src/api/user/index.ts:205-208`
- 问题：`saveMyProfile` 已经调用 `useUserStore().loadUser(true)`，页面随后又调用 `load()` 并并行获取 user/profile activity；头像上传成功后也重新拉取整个 Profile。上传开始时先关闭对话框，上传失败无法留在当前裁剪上下文中重试。`load()` 失败时外层会把已成功的资料保存/头像上传表现成失败。
- 影响：一次简单资料修改产生多组重复请求和 loading 闪烁；头像上传成功但主页刷新失败时会提示头像上传失败；活动缓存本来允许陈旧，整页刷新也不一定拿到新活动。
- 建议：保存资料后局部更新 `profile.user`，或者统一由一个刷新队列负责刷新；头像成功后只更新头像缓存版本/刷新头像组件。上传期间保持对话框并显示上传状态，成功后再关闭；把“业务成功”和“刷新失败”拆成两个结果。

### FE-002-08 [中] Tab 配置使用位置切片，权限与 UI 顺序产生隐式耦合

- 位置：`oj-vue/src/views/profile/Profile.vue:205-215,230-238`
- 问题：非本人通过 `allTabs.slice(0, 3)` 获得公开 tab，公开权限依赖数组前 3 项；`validTab` 和 `isActivityTab` 还分别维护了两套字符串列表。未来只要调整 tab 顺序或插入新 tab，就可能误隐藏/误显示设置页。`router.replace` 也没有捕获失败，路由状态和 `activeTab` 可能短暂不一致。
- 影响：权限规则变成了布局实现细节，后续扩展个人主页 tab 时容易产生越权展示或 tab 无法切换的问题。
- 建议：给 tab 配置显式加 `visibility: 'public' | 'owner'`，使用 `filter` 而不是 `slice`；活动 tab 使用 `Set`/统一配置派生，路由切换成功后再确认本地状态，或集中由一个 watcher 驱动。

### FE-002-09 [中] API 类型与后端契约过宽/不一致，缺少“结果是否截断”的表达

- 位置：`oj-vue/src/api/profile/index.ts:4-39`
- 问题：`IdType`、`points`、日期同时接受多种类型，`ProfileActivity.solvedProblems` 使用任意 `Record<string, string[]>`，而后端实际只产生固定四组并将 Long 序列化为字符串。`ProfileContest.type` 使用裸数字 `0 | 1`，组件中直接写 `type === 1`。后端还对题目、比赛、题解设置了固定上限，但响应没有 `limit/hasMore/truncated` 等字段。
- 影响：类型没有真正约束接口，后端把 `"1"`、`1` 或新增 ContestType 值返回时 UI 可能误显示为比赛；用户看到的“参加过的比赛/写过的题解”实际上可能只是前 50 条，却没有任何提示。
- 建议：前端使用与后端一致的序列化契约：ID统一为字符串，日期统一为 ISO 字符串，ContestType 使用命名常量/枚举；将难度组定义成显式接口；后端返回截断元数据，前端明确显示“仅展示最近 N 条”，为后续分页保留契约。

### FE-002-10 [中][CSS] 响应式断点过少，中间宽度下侧栏和表单会挤压

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:486-499`
  - `oj-vue/src/views/profile/Profile.vue:702-706`
  - `oj-vue/src/views/profile/Profile.vue:790-875`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:64-67`
- 问题：主页只在 780px 下从双列切换为单列，活动组件只在 700px 下把难度卡片改成单列。780～900px 左右时，主内容、220px 侧栏和双列表单同时存在；头像/昵称、验证码按钮和活动列表没有独立的中间布局策略。
- 影响：平板或窄窗口下标题、表单和列表会显得拥挤，按钮可能被压缩；活动卡片和外层布局的断点不一致，容易出现一边已经适配、另一边仍然拥挤的状态。
- 建议：以内容最小宽度而不是设备宽度设计断点：侧栏低于可读宽度时先改为顶部横向/可滚动 tab，再在更窄宽度改单列；表单网格、活动难度网格和列表日期分别设置可读性断点，并用容器查询或统一布局 token 收口。

### FE-002-11 [中][CSS] `height: 100%` 与 `mode="out-in"` 组合会造成切换跳高和溢出风险

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:68-70,598-603,651-671`
- 问题：`.profile-main` 是 flex 子项，`.profile-panel` 固定 `height: 100%`，但父级 `.profile-layout` 高度由内容自动决定；tab 切换使用 `Transition mode="out-in"`，离场期间内容被移除，父级高度会重新计算。过渡元素又没有明确的 `overflow`/尺寸稳定策略。
- 影响：不同 tab 内容高度差异较大时，页面可能出现跳动、短暂塌陷、滚动条闪现；进入活动列表或长题解列表时，过渡 transform 可能造成横向/纵向溢出。这类问题和此前个人主页“tab 动画/左右盒子高度”问题属于同一风险面。
- 建议：用明确的面板最小高度或高度动画容器承载 transition，避免对自适应内容使用无条件 `height: 100%`；必要时在 transition wrapper 上控制 `overflow: clip/hidden`，并针对活动、资料、安全三个面板分别验证最小高度。

### FE-002-12 [中][CSS/可访问性] 交互按钮没有 `:focus-visible`，只针对鼠标 hover

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:612-633`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:8,17,28,65-66`
- 问题：tab、题目 chip、比赛/题解列表都定义了 hover，但没有键盘焦点样式、`aria-current` 或等价状态表达；活动按钮还依赖模板中的 `$emit`，没有可读的 aria-label 补充截断后的 ID。
- 影响：键盘用户无法稳定判断当前 tab 和焦点位置；ID 被截断后，辅助技术获得的信息不足。暗色模式下默认 outline 也可能被组件样式覆盖。
- 建议：统一增加 `:focus-visible` 高对比度轮廓，当前 tab 设置 `aria-current="page"` 或 `aria-selected`；对题目/比赛/题解按钮提供完整标题和类型 aria-label。

### FE-002-13 [低][CSS/维护性] 存在死样式、整段压缩规则和无必要的全属性 transition

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:501,583-596`
  - `oj-vue/src/views/profile/Profile.vue:612-625`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:64-67`
- 问题：模板中没有 `.sidebar-note`，但仍保留完整样式；活动组件的主体 CSS 压缩成多条超长单行规则，难以 review 和定位响应式问题。多个按钮使用 `transition: .16s ease/.18s ease/.2s ease`，等价于对所有可动画属性做过渡。
- 影响：无效样式增加认知负担，后续修改容易误以为存在对应 DOM；全属性 transition 可能让尺寸、边框、布局变化也被动画化，出现按钮抖动或布局过渡异常；压缩 CSS 让重复规则难以发现。
- 建议：删除确认无用的 `.sidebar-note`；把活动组件 CSS 按布局、状态、响应式拆行；只 transition `color, background-color, border-color, box-shadow, transform` 等明确属性，并集中到可复用的 profile interaction token。

### FE-002-14 [低] 主页与活动组件重复实现显示工具和列表视觉结构

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:217-228`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:59-60`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:17-33`
- 问题：`shortId` 和 `formatDate` 在父子组件重复实现；主页的活动标题、活动 tab 判断、可见 tab 判断也分别维护字符串集合。比赛项和题解项虽然字段不同，但图标、主文案、次文案、日期、箭头和 hover 结构几乎完全重复。
- 影响：截断长度、日期格式或空值行为变更时需要改多处，已经出现父子组件日期逻辑复制；列表样式调整也容易只改到一个类型。
- 建议：抽取项目已有的通用 ID/日期展示工具；把 activity tab 和 list item 定义成数据配置，必要时只抽一个轻量 `ProfileActivityListItem`，不要再复制两套 CSS。优先复用现有通用工具，避免为 Profile 再造一套格式化标准。

### FE-002-15 [低] 存在无用 import 和魔法值，增加编译/维护噪声

- 位置：
  - `oj-vue/src/views/profile/Profile.vue:174`
  - `oj-vue/src/components/ProfileActivityPanel/ProfileActivityPanel.vue:18-20`
- 问题：`Profile.vue` 引入了未使用的 `Lock`；活动组件直接用 `contest.type === 1` 判断作业，页面文案和后端 `ContestType.HOMEWORK(1)` 的语义没有集中定义。
- 影响：当前可能不会阻塞构建，但会降低 lint 的信噪比；后端枚举增加类型时，前端默认会错误归类成比赛。
- 建议：清理未使用 import；建立 `ContestType` 映射和兜底文案，未知类型显示“其他活动”而不是静默落到比赛。

## 4. 做得较好的地方

- `UserProfileVo` 没有返回邮箱、权限、备注等敏感字段，公开资料 DTO 的字段边界是清楚的。
- 后端 Long ID 使用字符串序列化，能避免前端 JavaScript 精度丢失；前端也基本把 ID 当作 `IdType` 处理。
- Profile 活动查询有题目、比赛、题解上限，并且缓存 key 包含 `includePrivate` 参数，当前实现不会把本人私有题解直接复用给访客缓存。
- 个人资料、安全设置在模板层面已经只对 owner 展示，公开主页不直接出现修改密码等入口；但这仍然不能替代后端接口权限校验。
- 主页已有 `min-width: 0`、ID 截断、暗色变量和移动端布局意识，后续应在这些基础上收口，而不是继续复制页面级样式。

## 5. 建议修复顺序

1. 先修 FE-002-02、FE-002-03、FE-002-04：收紧公开/私有活动边界，统一 Profile API 响应归一化和页面错误状态。
2. 修 FE-002-01、FE-002-05、FE-002-06：处理请求竞态、验证码状态机和密码提交结果隔离。
3. 修 FE-002-10、FE-002-11、FE-002-12：补中间宽度布局、稳定 tab 面板高度、焦点样式和过渡溢出控制。
4. 最后处理 FE-002-07～09、FE-002-13～15：减少重复请求/重复逻辑，收紧类型契约和清理 CSS/魔法值。
5. 增加最小化测试：快速切换两个 userId、Profile API 返回 null/缺列表、公开主页包含私有题解、验证码刷新失败、密码成功但刷新验证码失败、780～1000px 和暗色模式布局。

## 6. 测试与验证

### 本次检查

- 逐行检查了 `Profile.vue`、`ProfileActivityPanel.vue` 和 `api/profile/index.ts`。
- 对照检查了 user-service 的公开资料 DTO/controller 和 problem-service 的 ProfileController、缓存、SQL 查询。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 当前前端已有测试命令会在加载 Element Plus CSS 时因 `Unknown file extension ".css"` 失败，且现有测试没有覆盖个人主页交互。
- 因此本次结论主要来自源码、接口契约和 CSS 结构检查，不能替代浏览器端多窗口尺寸/暗色模式回归。

## 7. 本次未修改内容

- 未修改个人主页、活动组件、Profile API、用户服务或题目服务。
- 未提交 Git。
- 仅新增本 review 报告。
