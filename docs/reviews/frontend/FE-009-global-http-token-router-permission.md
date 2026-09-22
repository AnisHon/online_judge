# FE-009 全局 HTTP、Token、动态路由与权限 Review

## 1. Review 范围

- HTTP 层：`oj-vue/src/utils/http.ts`、Axios 拦截器、401/403 处理、错误通知、请求参数序列化
- 认证 API：`oj-vue/src/api/auth/authentication.ts`、登录/注册/刷新/退出流程
- Token 与用户状态：`oj-vue/src/stores/useToken.ts`、`useUserStore.ts`、`useMenuStore.ts`
- 其它全局 Store：`useSiteConfig.ts`、`useLanguage.ts`、`useConfig.ts`、`useTabStore.ts`
- 路由：`oj-vue/src/router/index.ts`、`oj-vue/src/router/dynamic.ts`
- 权限：`oj-vue/src/utils/authUtil.ts`、`oj-vue/src/utils/hasAuth/index.ts`、权限指令使用方
- 菜单与后台布局：`RecursiveMenuItem.vue`、`Menu.vue`、`BackendMenu.vue`、`SubFormItem.vue`、`LayoutBackEnd.vue`、`HeaderBar.vue`、`CustomTab.vue`、`CustomBreadCrumb.vue`
- 本次覆盖：会话代际与刷新并发、登出竞态、错误隔离、请求统一性、动态路由权限、菜单刷新、权限指令响应性、KeepAlive/tab 参数、全局 CSS 泄漏、弹层/菜单层级和响应式溢出。
- 本次按报告执行全局基础设施修复；未修改 FE-007 竞赛/作业页面的既有未提交改动。

## 2. 结论摘要

这一批已经有一些正确的基础：刷新请求有 single-flight，用户和菜单 Store 通过 `sessionVersion` 尝试阻止旧请求写回，动态路由也引入了 `routeGeneration`，站点配置明确没有持久化。但是这些保护只覆盖了“写入 Store”的一小段路径，没有覆盖整个请求、重放和路由注册生命周期。

当前最高风险是会话代际没有绑定到 refresh promise 和被重放的 Axios request：用户退出登录或切换账号后，旧 refresh 响应可能重新写入旧 token，旧请求还可能以新账号的 token 重放。其次是动态路由把后端返回的菜单当成完全可信的组件/权限快照，既没有把权限元数据带入路由，也没有验证组件、路由名和菜单类型；后台入口还只按菜单数组非空判断，容易再次出现普通用户看到“进入后台”。

另外，HTTP 层仍有 raw Axios 绕过统一拦截器，生产错误消息会直接展示后端原文，登出网络失败时不会完成前端跳转，权限指令在绑定值变化和组件根节点场景下不可靠。后台菜单存在两套递归实现和全局未 scoped 的菜单 CSS，tab、面包屑、菜单 action 的布局也存在覆盖和参数链接问题。建议先修会话/权限边界，再收敛请求与路由基础设施，最后抽取菜单和后台布局样式。

## 3. 问题清单

### FE-009-01 [高] refresh single-flight 没有绑定会话代际，旧账号的响应可以复活当前会话

- 位置：`oj-vue/src/utils/http.ts:65-90`、`oj-vue/src/stores/useToken.ts:22-35`、`oj-vue/src/api/auth/authentication.ts:72-87`
- 问题：`refreshing` 是模块级全局 Promise。它创建时只捕获当时的 `refreshToken`，请求完成后无条件调用当前 `tokenStore.setTokens(...)`。如果用户在 refresh 期间退出、重新登录或切换账号，旧 refresh 响应仍可能覆盖当前 token；新的请求还会复用旧 Promise。
- 影响：表现为“偶尔自动退出”“退出后又短暂登录”“切换账号后页面出现上一账号数据”，而且问题只在慢网络、401 并发或服务响应较慢时出现，难以复现。
- 建议：为每次登录建立 session generation/session id，refresh promise 同时保存 generation、refresh token 指纹和账号上下文；响应落地前必须确认它仍是当前会话。`clearToken`、登录成功和账号切换都要使旧 generation 失效，不能只清空字符串。

### FE-009-02 [高] 401 请求重放没有会话快照，退出后旧请求可能以新账号身份发送

- 位置：`oj-vue/src/utils/http.ts:102-117,140-166`
- 问题：`retryWithFreshAccessToken` 只给 Axios config 写 `_retry`，没有记录请求发起时的 access token/session generation。请求等待 refresh 的过程中，如果用户退出并登录另一个账号，旧请求恢复后会读取全局 refresh 结果并用当前 token 重放。
- 影响：旧页面、旧组件或旧轮询的响应可能混入新账号状态；更严重时，旧账号触发的写操作会在新账号上下文中重放。`_retry` 只能防止同一个 config 无限重试，不能解决身份错位。
- 建议：请求 config 带内部 session id 和原始 token 指纹；重放前校验 session 未变化，否则以“会话已切换”终止，不再发送。退出时同时取消/拒绝等待中的请求和轮询。

### FE-009-03 [高][安全] access token 和 refresh token 一起持久化，且 token 替换不是原子会话切换

- 位置：`oj-vue/src/stores/useToken.ts:7-38`、`oj-vue/src/api/auth/authentication.ts:72-81`
- 问题：`persist: true` 会把 access token 和 refresh token 交给 Pinia 持久化插件；同时 `setTokens` 只有在传入 refresh token 为真值时才覆盖旧 refresh token。登录响应缺少 refresh token、响应格式切换或账号切换时，旧 refresh token 可能被保留。
- 影响：浏览器 XSS 或恶意扩展一旦读取持久化存储即可同时拿到长期凭据；旧 refresh token 残留会导致账号状态错乱，前端看起来是新账号，刷新流程却拿旧账号凭据。
- 建议：优先让 refresh token 由 HttpOnly/Secure cookie 管理，前端只持有短期 access token；若现有双 key 契约必须由前端保存，至少按 session 原子替换、明确 `undefined` 的清理语义、限制持久化范围并在退出/切换账号时清除所有旧凭据。

### FE-009-04 [高] 登出接口失败时不会跳转登录页，且调用方没有处理 rejected Promise

- 位置：`oj-vue/src/api/auth/authentication.ts:134-142`、`oj-vue/src/components/AccountMenu/AccountMenu.vue:38-45`
- 问题：`logout` 在 `finally` 中清 token，但 `router.replace({name: 'login'})` 位于 try/finally 之后。如果登出请求超时或返回错误，异常会在 finally 后继续抛出，路由跳转语句不会执行；AccountMenu 也没有 `await/catch`。
- 影响：页面已经没有 token，但用户仍停留在后台；后续请求收到 401，界面可能出现空数据、重复跳转或错误通知，看起来像“退出登录坏了”。网络不可用时登出尤其容易卡在这个状态。
- 建议：登出应采用本地会话优先：立即使 session 失效、清理动态路由和用户状态，随后尽力通知后端，最后在 `finally` 中无条件跳转登录页。调用处捕获网络错误，但不应阻止本地退出。

### FE-009-05 [高][生产隔离] 全局错误处理会把后端原始 message 直接展示给用户

- 位置：`oj-vue/src/utils/http.ts:160-175,179-192,229-232`、`authentication.ts:59-67`
- 问题：HTTP 层直接使用 `error.response.data.message`、Axios `error.message` 和业务 `result.message` 构造通知。后端若误返回 SQL、堆栈、MinIO/RabbitMQ 连接信息或网关内部异常，`defaultFail` 会原文弹给前台；认证页又会把异常重新转成 Error message 展示。
- 影响：违反生产环境异常隔离要求，泄漏内部实现、表名、路径、服务地址和判题基础设施信息；同时页面级通知和 HTTP 层 fail handler 可能重复弹出同一错误。
- 建议：生产环境只允许展示稳定的用户错误码/白名单文案，服务原文只进入后端日志或管理员诊断接口；前端统一把 `requestId/traceId` 放入“请求失败，请稍后重试”提示。认证页使用结构化错误类型，不要用任意 `Error.message` 作为用户文案。

### FE-009-06 [中] 403 没有全局去重和导航互斥，多个请求会重复通知并互相改路由

- 位置：`oj-vue/src/utils/http.ts:46-50,151-153,168-175`
- 问题：401 有 `isRedirectingToLogin`，403 没有类似保护。页面初始化、菜单请求和多个表格请求同时 403 时，每个响应都会弹通知并执行 `router.replace('403')`；当前已经在 403 页面时也没有停止重复导航。
- 影响：通知堆叠、导航竞态、组件卸载后仍有请求回调；与动态路由加载失败叠加时，用户看到的页面和错误原因不稳定。
- 建议：将认证失效、权限拒绝、网络错误统一纳入一次导航状态机；对相同会话只允许一个权限跳转，并在目标路由、请求来源和当前 session 都匹配时才执行。

### FE-009-07 [中][权限] 文件/测试点的二进制请求绕过统一 HTTP 层，认证、刷新、错误隔离和超时策略不一致

- 位置：`oj-vue/src/api/file/index.ts:158-179,199-207,213-219`、`oj-vue/src/api/problem/case.ts:40-59`
- 问题：下载、上传和测试点下载直接使用原始 `axios` 或创建 `<a>`，没有复用 `service` 的 token 注入、401 refresh、403 去重、超时和统一错误解析。`upload` 甚至没有显式 token，`downloadFile` 也没有自定义 token header。
- 影响：普通 JSON API 正常而文件下载/上传 403；token 过期时不会按双 key 策略恢复；错误响应被当作 Blob 后再读取 `message` 也不可靠。之前出现的“后端权限正常但前端下载坏掉”很可能由此重复发生。
- 建议：抽取统一的 `binaryService`，复用同一会话拦截器，只改变 `responseType`；下载失败先安全解析错误 Blob，上传也必须使用同一认证和取消机制。不要让页面直接拼接 `/api` 链接绕过认证层。

### FE-009-08 [中] refresh 路径判断依赖字符串 includes，内部 Axios config 使用大量 any

- 位置：`oj-vue/src/utils/http.ts:102-125`
- 问题：`canRefreshRequest` 通过 `request.url?.includes(path)` 判断是否跳过刷新，request config 和 `_retry/_skipAuthRefresh` 均为 `any`；请求 URL 可能是相对路径、绝对 URL、带 query 的路径或代理重写后的路径。请求拦截器又用 `endsWith('/auth/refresh')` 做另一套判断。
- 影响：包含相似片段的业务 URL 可能被错误跳过 refresh，或 refresh 请求被错误注入 access token；类型系统无法约束内部字段，后续迁移 Axios 版本时容易出现 header/config 运行时错误。
- 建议：定义 `InternalRequestConfig`，用明确的 `skipAuthRefresh`、`retryCount` 和规范化 pathname 判断；认证接口使用显式标记而不是 URL 子串匹配。access/refresh 请求应分别由明确的 client 负责。

### FE-009-09 [中] 路径参数统一使用 `.toString()`，未统一 URL 编码，数组/Long/特殊字符契约不清晰

- 位置：`oj-vue/src/utils/http.ts:195-199,220-227,246-257`
- 问题：`get`、`pathPut`、`del` 直接把参数或数组 `.toString()` 后拼到 URL；数组依赖逗号协议，路径参数没有统一 `encodeURIComponent`，对象会变成 `[object Object]`。不同 API 同时存在 path、query、repeat array 三种序列化方式。
- 影响：Long ID 通常能工作，但包含特殊字符的文件名、路径、用户名或空数组会产生错误 URL；批量删除/班级用户等接口的前后端契约靠调用者自行记忆，容易出现空段、重复逗号或参数污染。
- 建议：统一 `buildPath`/`buildQuery`，对每个参数逐项编码；数组明确使用 query repeat 或后端约定的 batch body，不再依赖隐式 `toString()`。API 类型应区分 path 参数、query 参数和 body。

### FE-009-10 [高] 站点配置加载实际会阻塞整个应用启动，和注释中的“不会阻塞”相反

- 位置：`oj-vue/src/main.ts:24-31`、`oj-vue/src/stores/useSiteConfig.ts:20-37`、`oj-vue/src/utils/http.ts:52-63`
- 问题：`bootstrap` 在 `app.mount` 前 `await useSiteConfig(pinia).load()`；HTTP 默认超时是 60 秒。配置服务不通时，页面可能空白近一分钟，而注释声称配置失败不会阻塞。并发调用 `load()` 时直接返回当前默认 config，不会等待正在进行的请求；失败后 `loaded=true`，普通调用也不会再重试。
- 影响：网关/Nacos/后端暂不可用时，登录页和错误页都无法及时显示；其它组件可能在同一启动窗口读取默认配置，导致备案号/站点名短暂不一致。
- 建议：先挂载应用再异步加载配置，或给配置请求单独的短超时；用共享 Promise 让并发调用等待同一请求，失败保留可重试状态而不是把失败标记成成功加载。配置更新应按版本原子替换。

### FE-009-11 [中] `loadUser(force)` 实际不会强制刷新，旧请求仍可能向调用方返回过期用户

- 位置：`oj-vue/src/stores/useUserStore.ts:25-45,48-69`
- 问题：`force=true` 只绕过缓存判断，但只要 `loadPromise` 存在仍直接返回旧 Promise；`sessionVersion` 只阻止旧请求写入 Store，却不阻止它把旧用户对象返回给调用方。`getUser` 最后还把可能仍为 null 的值强制断言为 `LoginUser`。
- 影响：刷新资料、登录切换、动态路由加载同时发生时，调用方可能拿到旧账号数据；“强制刷新”无法保证拿到最新结果，失败或空响应可能在后续使用位置才产生异常。
- 建议：force 请求要取消或替换旧请求，并返回带 generation 的结果；所有调用方写入前校验 session。`getUser` 应返回 `LoginUser | null` 或明确抛出认证错误，不能用类型断言掩盖状态不完整。

### FE-009-12 [中] 菜单 Store 的 force/ready 状态语义不完整，清理和加载不是一个原子事务

- 位置：`oj-vue/src/stores/useMenuStore.ts:8-55`、`oj-vue/src/router/index.ts:260-290`
- 问题：`load(force=true)` 与用户 Store 一样，只要旧 `loadPromise` 存在就不会真正刷新；`isDynamicReady()` 判断的是 `menu.value` 是否已赋值，并不代表菜单树加载成功、权限仍匹配、动态路由已注册。`clear()` 把 promise 引用置空，但不会取消旧网络请求。
- 影响：退出/切换账号或权限刷新时可能短暂使用上一账号菜单；路由守卫看到 ready 但路由未注册，或看到未 ready 后重复构建。旧请求虽然可能被 generation 防住，但仍会占用网络并制造难以追踪的响应时序。
- 建议：把“请求中、树已加载、路由已注册、当前 session”建成显式状态；Store 和 router 使用同一个 session generation。刷新时取消旧请求，只有完成菜单过滤、路由注册和 Store 提交后才进入 ready。

### FE-009-13 [高][权限] 动态菜单没有把后端权限元数据写入路由，直接 URL 访问缺少前端二次边界

- 位置：`oj-vue/src/router/dynamic.ts:183-209,212-236`
- 问题：`buildRouteRaw` 只把 `menuName`、`icon`、拼接后的 `path` 放到 `meta`，没有复制 `menuType`、`perms` 或路由所需权限；固定动态页才有 `meta.has/hasAny`。动态菜单能否访问完全依赖 `/auth/menus` 接口返回是否已经过滤正确。
- 影响：一旦后端菜单树缓存错误、角色变更未同步或接口返回过宽，用户可以直接输入已注册的后台 URL；前端按钮可能隐藏，但页面路由仍能进入。前端无法诊断“菜单显示了但权限不对”还是“接口返回错了”。
- 建议：后端继续作为最终鉴权，同时把权限字符串和菜单类型带到 route meta，守卫对每次后台导航重新检查权限；菜单接口只返回可见菜单不能替代 API 级鉴权。固定页和动态页统一同一套 permission predicate。

### FE-009-14 [高] 动态路由构建没有校验组件、路由名、路由地址和菜单类型

- 位置：`oj-vue/src/router/dynamic.ts:183-208,222-236`
- 问题：组件通过 `modules[`../views/${menu.component}.vue`]` 查找，找不到时仍创建 `component: undefined` 的路由；`name` 直接使用 `menu.router`，没有检测重复；所有树节点都参与构建，没有按 `MenuType.MENU/MENU_ITEM` 排除按钮节点。父节点 redirect 直接取第一个子节点的原始 router。
- 影响：后台新增一个按钮或错误组件路径就可能生成空白/无法渲染的菜单项；重复 router 会触发 Vue Router 警告、覆盖或删除错误路由；父菜单的第一个子节点如果是按钮/不可见节点，点击父菜单会跳到不存在的地址。
- 建议：构建前做 schema 校验和归一化：只把菜单/菜单项转为 route，按钮只留作权限数据；组件缺失、重复 name、非法 path 记录诊断并跳过；redirect 从最终可访问的叶子路由计算，而不是从原始树第一项读取。

### FE-009-15 [高] 动态路由重建可能累积旧 children，导致重复路由和菜单污染

- 位置：`oj-vue/src/router/dynamic.ts:174-177,249-257,266-293`
- 问题：`loadDynamicRoutes` 在 `menuStore.isDynamicReady()` 但 `router.hasRoute('backend')` 为 false 的情况下，会重新构建后执行 `dynamicRoute.children.push(...menuTree)`；此时没有先恢复 `fixedDynamicChildren`。另外 `filterDynamic()` 在 generation 校验前就直接修改全局 `dynamicRoute.children`，reset 如果恰好发生在这段时间，也可能留下被放弃会话的 children。
- 影响：重复 name、重复菜单、路由删除/添加顺序异常，甚至出现一次登录正常、刷新或切换账号后后台菜单逐渐变多的现象。
- 建议：不要原地修改共享 `dynamicRoute`；每次按当前 session 深拷贝出完整 route record，再一次性 `addRoute`。注册前验证 generation，注册后再提交 menu Store；reset 只删除当前 generation 的 route。

### FE-009-16 [高][权限] 后台入口只判断“菜单数组非空”，不能代表用户拥有后台页面权限

- 位置：`oj-vue/src/stores/useMenuStore.ts:61-64`、`oj-vue/src/components/AccountMenu/AccountMenu.vue:50-58`、`oj-vue/src/router/index.ts:286-290`
- 问题：`hasBackendAccess` 只判断 `menuTrees.value.length > 0`。如果响应中有按钮节点、空路由节点、不可渲染节点，或后端返回了与后台首页无关的菜单，仍会显示“进入后台”；路由守卫也用同一条件放行 `/backend`。
- 影响：普通用户可能看到后台入口，点击后进入空后台、403 或菜单异常；这正是“只用数组非空代替权限判断”的典型边界错误。
- 建议：定义明确的后台访问权限或至少检查最终可访问的页面 route（排除按钮、无组件节点），入口展示与路由守卫复用同一 `canAccessBackend`，并由后端接口最终拒绝无权限请求。

### FE-009-17 [中] 菜单 Store 保存了可变 route 数组，BackendMenu 又只取一次快照

- 位置：`oj-vue/src/stores/useMenuStore.ts:53-59`、`oj-vue/src/router/dynamic.ts:281-285`、`oj-vue/src/layout-backend/component/BackendMenu/BackendMenu.vue:31-36`
- 问题：`setMenu(menuTree)` 直接保存数组引用，后续 `menuTree.splice/push` 会改变 Store 内的旧值；BackendMenu 中 `const routes = menuStore.getMenu()` 只在 setup 时读取，不是 computed。权限刷新或菜单重建后，路由可能已经变化，但视觉菜单仍然使用旧数组。
- 影响：路由、面包屑和侧边栏不一致；旧菜单项可能继续显示并覆盖当前布局，或者新菜单已经可访问但侧边栏没有入口。
- 建议：Store 只接收不可变快照/深拷贝；BackendMenu 使用 computed 读取当前 menu，并在 session/permission 变化时清理展开项、active key 和旧菜单缓存。

### FE-009-18 [中] 路由守卫失败/取消时没有保证 NProgress 结束

- 位置：`oj-vue/src/router/index.ts:245-294`
- 问题：`beforeEach` 一开始 `NProgress.start()`，但动态路由加载失败、权限判断返回 false 或导航被异常中断时，`afterEach` 不会执行；代码没有 `router.onError` 或守卫级 finally 调用 `NProgress.done()`。
- 影响：网络异常或权限失效后页面顶部进度条可能一直处于加载状态，用户误以为页面仍在请求；多次导航会叠加错误状态。
- 建议：把导航 loading 生命周期放在统一的 `beforeResolve/afterEach/onError`，无论成功、取消、异常都 done；同时给动态路由加载失败提供可重试的错误态，而不是静默 `return false`。

### FE-009-19 [中][权限] 权限工具允许 undefined 直接通过，且 ID 比较没有统一 Long 字符串化

- 位置：`oj-vue/src/utils/authUtil.ts:4-29,32-40`
- 问题：`hasPerm(undefined)` 和 `hasAnyPerm(undefined)` 返回 true；这把“没有配置权限要求”和“权限元数据缺失/拼写错误”混为一谈。`isUserIdEqual` 使用严格相等，没有将后端可能返回的 number/string Long ID 统一成字符串。
- 影响：动态 route meta 丢失权限时可能被默认放行；用户 ID 在不同接口类型不一致时，自己的题解/提交/资料入口可能错误地判断为非本人或本人。权限问题会表现成按钮忽隐忽现，而不是明确配置错误。
- 建议：区分 `noRequirement` 与 `invalidRequirement`；路由和管理操作对缺失权限元数据采用 fail-closed。所有 ID 比较统一 `String(a) === String(b)`，并在 API 边界明确 `IdType` 为字符串。

### FE-009-20 [中][权限] 权限指令的 watcher 捕获旧 binding，动态权限值变化后只更新一次

- 位置：`oj-vue/src/utils/hasAuth/index.ts:38-76`、动态使用如 `ContestManagement.vue:99`
- 问题：`mounted` 中创建的 `update` 闭包捕获了初始 `binding.value`；`updated` 只调用一次 `applyVisibility`，没有重建 watcher。像新增/编辑按钮这种根据状态切换权限字符串的绑定，后续用户权限刷新时 watcher 仍检查旧权限。
- 影响：权限变更、账号切换或动态 dialog 状态变化后，按钮可能显示错误；页面看起来已根据新权限刷新，但下一次 Store auths 更新又回到旧判断。
- 建议：把 required permission 放进响应式源，`updated` 先停止旧 watcher 再按新 binding 重建；更好的方式是抽取 `usePermission` computed，让按钮 `v-if/v-show` 和指令共享同一实现。

### FE-009-21 [高][权限/CSS] 指令直接操作组件根节点，无法保证 Element Plus 组件和 Teleport 内容真正隐藏

- 位置：`oj-vue/src/utils/hasAuth/index.ts:20-35,49-76`、多个 `el-button/el-switch/el-empty` 使用处
- 问题：指令假设 `element` 一定是最终 DOM 元素并设置 `hidden/aria-hidden`。Vue 指令挂在组件上时，如果组件是多根节点、使用 fragment 或将内容 Teleport 到 body，可能产生 “Runtime directive used on component with non-element root node” 警告，或只隐藏包装节点而没有改变真实交互元素。`hidden` 也只是视觉隐藏，不能替代业务操作的后端鉴权。
- 影响：用户之前看到的 Element Plus roving focus/dropdown 警告可能重复出现；被隐藏的菜单项可能仍参与组件状态、焦点或 Popper 计算，权限切换后还可能留下不可见的点击区域。
- 建议：优先对真实原生元素使用 directive；组件级权限改成 `v-if`/computed wrapper，菜单项统一由过滤后的数组生成。对权限不足应同时移除 tab stop 和事件入口，后端接口继续做最终校验。

### FE-009-22 [中][工程化] 权限判断和菜单渲染存在多套实现，语义已经发生漂移

- 位置：`oj-vue/src/utils/authUtil.ts`、`oj-vue/src/utils/hasAuth/index.ts`、`oj-vue/src/components/RecursiveMenuItem/RecursiveMenuItem.vue`、`oj-vue/src/layout/component/menu/SubFormItem.vue`
- 问题：`authUtil` 用同步函数做 route/组件判断，`hasAuth` 用 directive + watcher 做 DOM 隐藏；菜单又有 `RecursiveMenuItem` 和 `SubFormItem` 两套递归实现。前者任何叶节点都会渲染，后者只有 `meta.type === MENU_ITEM` 才渲染；dynamic route builder 又没有设置 `meta.type`，两套实现的结果必然不同。
- 影响：修了一个入口的权限不一定修另一个入口；菜单节点可能在某处消失、另一处仍显示，按钮/菜单/路由的类型边界无法追踪。重复实现还增加了 CSS 和 key/active 状态不一致的概率。
- 建议：保留一个 route/menu view-model builder，统一 `visible`、`menuType`、`permission`、`path`、`icon` 字段；按钮权限、路由守卫、菜单渲染和 action visibility 都从同一 predicate 派生，删除未使用的旧递归组件。

### FE-009-23 [中][CSS] BackendMenu 的非 scoped CSS 会污染全局 Element 菜单和前台移动菜单

- 位置：`oj-vue/src/layout-backend/component/BackendMenu/BackendMenu.vue:57-79`
- 问题：第二个 `<style lang="scss">` 没有 `scoped`，其中 `.menu`、`.el-menu>.el-menu-item`、`.el-menu .el-sub-menu...` 都是全局选择器。前台 `Menu.vue` 和移动抽屉同样使用 Element `el-menu`，会被后台菜单的 hover、子菜单背景和边框规则影响。
- 影响：后台布局调整可能让前台导航、抽屉、暗色模式一起变化；层级和 z-index 相关问题会表现为“某个抽屉被菜单覆盖”或样式在不同路由间串掉。
- 建议：所有后台选择器挂在 `.backend-menu` 根类下，并用 `:deep()` 只穿透到该组件内部；颜色、hover、collapse 尺寸放入后台专用 token，不要写全局 `.el-menu`。

### FE-009-24 [中][CSS/布局] 前台 nav action 放在 `el-menu` 内并绝对覆盖，未给导航内容预留空间

- 位置：`oj-vue/src/layout/component/menu/Menu.vue:3-9,39-47`
- 问题：`theme-trigger/account-menu` 被放在 `el-menu` 的非菜单子节点中，再通过 absolute 和渐变背景覆盖右侧。导航项没有为这块区域预留 padding，宽度接近断点时最后的菜单项会被遮挡或无法点击；Element Menu 的键盘/roving focus 也不应混入普通 div。
- 影响：响应式缩放、长站点名、菜单文字变长或浏览器字体放大时，导航出现点击区域重叠；这和之前“网站图标被挤下去、code 的 e 被挡住”属于同一类布局风险。
- 建议：将品牌、菜单、操作区拆成三列 flex/grid：中间菜单独立滚动，右侧 action 固定占位；移动端单独渲染 drawer。不要让普通容器成为 `el-menu` 的直接子节点。

### FE-009-25 [中][CSS] 后台 tab 会无限累积并被 header 裁剪，且 tab identity 忽略 params/query

- 位置：`oj-vue/src/stores/useTabStore.ts:26-69`、`CustomTab.vue:31-39,56-86`、`LayoutBackEnd.vue:22-34`
- 问题：tab 以 route name 为唯一 key，忽略 params/query；同一页面打开不同题单、用户或比赛时会复用同一个 tab/component，只更新 URL，组件内部可能仍保留上一个上下文。tab 数量没有上限或关闭策略，CSS 又对 header/nav 使用 `overflow:hidden`，没有稳定的横向滚动容器。
- 影响：后台二级页切换 ID 后可能显示上一次数据；tab 过多时后面的标签和关闭按钮被裁剪，甚至溢出覆盖正文。KeepAlive 的 `include` 也以 component basename 为 key，重名组件会互相影响缓存。
- 建议：用 `name + normalized params/query` 作为 tab key，明确哪些路由允许多实例；tab header 使用单一水平滚动区并保证 active tab 自动可见，设置上限/最近使用淘汰策略。KeepAlive 使用稳定组件标识，避免只用 basename。

### FE-009-26 [中][路由/CSS] 面包屑直接拿 route record path，带参数的二级页链接会失效

- 位置：`oj-vue/src/layout-backend/component/CustomBreadCrumb/CustomBreadCrumb.vue:20-31`
- 问题：`route.matched` 中的 `item.path` 可能包含 `:contestId`、`:problemId` 等动态段，组件直接把它作为 `router-link :to`。它没有用当前 `route.params` 生成已解析的 href，也没有区分可点击父节点与当前叶子。
- 影响：在题目分数、用户答案、学生管理等带参数页面点击面包屑，可能跳到包含字面量 `:id` 的错误地址；长标题又受 `max-width`、`overflow:hidden` 约束，用户看不到当前上下文，进一步加剧“页面像卡住”的感受。
- 建议：使用 `route.matched` 的 route name + 当前 params/query 生成可解析目标，只对可复用的父级目标添加 link；面包屑容器保留单一横向滚动/省略策略，并在窄屏下允许查看完整标题。

### FE-009-27 [低][工程化] Store 中重复的懒加载模式没有统一错误、取消和缓存契约

- 位置：`useUserStore.ts:22-45`、`useMenuStore.ts:10-45`、`useSiteConfig.ts:16-37`、`useLanguage.ts:8-19`
- 问题：用户、菜单、站点配置、编程语言各自实现 `loading/loadPromise/loaded` 的一部分；有的共享 Promise，有的 loading 时直接返回默认值，有的空数组即重复请求，有的失败后没有 retry 状态。它们都没有 AbortController/request id，也没有统一的 session 归属。
- 影响：慢网络下重复请求、旧响应覆盖新状态、失败态被误认为空数据/默认数据；开发者需要在每个 Store 单独记住不同的生命周期规则，后续很容易重现前端“按钮/路由刷不出来”。
- 建议：抽取通用 `useResource`/`useSessionResource`，统一 `idle/loading/success/error`、共享请求、取消、generation、force refresh 和 stale-while-revalidate 语义；页面按状态渲染，不要以空数组同时表示加载失败和真实空结果。

## 4. 做得较好的地方

- `http.ts` 已经将 refresh client 和业务 client 分开，并通过 `refreshing` 避免同一时间大量 refresh 请求，这是正确方向；下一步只需补 session generation 和取消机制。
- `useUserStore`、`useMenuStore` 已经意识到旧请求写回的问题，引入 `sessionVersion`；说明代码已经具备会话隔离的设计入口，建议扩展到 refresh、重放请求和路由注册，而不是推倒重来。
- 动态路由已经有 `routeGeneration`，并在异步菜单返回后检查 generation；当前缺口主要是检查位置太晚、共享 route 对象原地变异，以及权限/组件 schema 没有在构建前验证。
- 站点配置 Store 明确不持久化，符合“进入网站前读取、页面生命周期内保存”的需求；需要把启动等待改为可恢复的异步加载。
- 权限指令已经通过 watcher 在用户权限异步加载后恢复按钮，而不是永久删除 DOM；这个思路比一次性 `removeChild` 更容易恢复，但仍需修复组件根节点和 binding 生命周期。
- 后台布局已经尝试使用 `min-width: 0`、响应式 padding、统一 CSS variables 和 `append-to-body` 友好的层级方向，后续适合以作用域和布局结构收敛，而不是继续提高 z-index。

## 5. 建议修复顺序

1. 先修 FE-009-01、02、03、04、05、16、13、14、15：建立会话代际、登出兜底、错误隔离和真正的后台/路由权限边界。这些属于账号安全和全站可用性问题。
2. 再修 FE-009-06、07、08、09、10、11、12、18：统一 HTTP/二进制请求、Store 加载状态、配置启动策略和导航错误收尾，解决 401/403、下载权限、空白启动和进度条残留。
3. 修 FE-009-19、20、21、22：合并权限 predicate、路由/菜单 view-model 和权限指令，消除权限刷新后按钮/路由不一致及 Vue directive warning。
4. 最后修 FE-009-17、23、24、25、26、27：收敛菜单/递归组件、后台 CSS 作用域、tab identity、面包屑参数和通用资源加载器，避免后续页面优化继续互相影响。

## 6. 建议补充的回归测试

- 会话：两个并发 401、refresh 延迟期间退出、refresh 延迟期间登录另一账号、refresh token 轮换、无 refresh token、登出接口超时/500。
- HTTP：JSON 错误、非 JSON 错误、Blob 错误、401/403 并发、网络超时、重复通知、接口返回 SQL/堆栈时生产只显示通用文案。
- 权限：普通用户没有后台页面、只有按钮权限、角色切换后刷新菜单、权限异步加载前后按钮恢复、动态权限 binding 变化、直接输入受限后台 URL。
- 路由：组件路径不存在、重复 router、按钮节点混入菜单、父菜单首个子节点不可见、退出/重新登录时旧动态路由不会复活或累积、NProgress 在取消/异常时会结束。
- 布局：后台菜单与前台菜单互不串样式，drawer/dialog 不被 header/menu 覆盖；320/768/1024/1440px、暗色模式、长菜单、10 个以上 tab、带参数的面包屑和同名不同参数页面。
- 文件与状态：上传/下载过期 token、重试/取消、跨账号响应不能写入 Store，站点配置服务离线时登录页应及时挂载并支持重试。

## 7. 测试与验证

### 本次检查与验证

- 逐行检查了 HTTP 拦截器、双 key 刷新、登录/注册/登出、用户/菜单/站点/语言 Store、动态路由构建和 reset、路由守卫、权限工具与 directive。
- 检查了前台/后台递归菜单、后台布局、header、tab、面包屑和相关 CSS 的作用域、overflow、z-index、响应式行为。
- 额外检查了文件/测试点的原始 Axios 调用，确认它们没有完整复用统一 HTTP 层。
- 已执行 `pnpm type-check`，通过。
- 已执行 `pnpm build-only`，通过；仅保留 Vite 关于静态/动态重复引用和大 chunk 的既有提示。
- 未执行数据库、Docker、部署或其它外部状态变更。

### 当前验证限制

- 本次修复以静态检查和生产打包为主，仍需要真实浏览器回归 refresh 竞态、跨账号请求、Element Plus directive warning、菜单弹层层级和窄屏 tab 行为。
- 已执行 `pnpm type-check`、`pnpm build-only`，均通过；同时应在提交前执行 `git diff --check`。

## 8. 本次修复结果

### 已处理

- `FE-009-01/02`：Token Store 增加内存 session generation；refresh single-flight 绑定 generation 和 refresh token，旧 refresh/旧 401 请求不能覆盖或清理新会话。
- `FE-009-04/06`：401/403 导航增加会话校验和去重；登出已有本地优先、无条件清理并跳转逻辑；路由错误时补齐 NProgress 收尾。
- `FE-009-05`：生产环境对 5xx、网络错误统一显示安全文案，开发环境保留业务错误提示，避免把后端异常原文直接暴露给用户。
- `FE-009-07/08/09`：新增带 token 的 `binaryService`；文件、测试点下载、分片上传不再绕过鉴权；refresh 路径改为规范化 pathname 判断，路径参数统一编码。
- `FE-009-10`：应用先挂载再异步加载站点配置，配置服务慢或离线不会阻塞登录页启动。
- `FE-009-11/12/17`：用户旧请求不再向调用方返回跨会话数据；菜单 Store 和后台菜单使用快照/计算读取，避免可变数组污染。
- `FE-009-13/14/15`：动态路由过滤按钮节点，校验组件路径，处理重复路由名，使用最终可用子路由生成 redirect，并避免 children 累加。
- `FE-009-18/20/23/24/25/26`：权限指令更新时读取最新绑定；后台菜单 CSS 收敛到组件作用域；前台操作区移出 `el-menu`；参数化后台页使用独立 tab identity；面包屑改为按当前 params/query 解析链接。
- `FE-009-19`：用户 ID 比较统一字符串化，兼容后端 Long 的 string/number 表现。

### 仍需后续处理

- `FE-009-03`：当前双 key 仍按既有契约保存于 `sessionStorage`，已经做到登录/刷新/退出的原子替换和浏览器会话级别；若后端后续支持 HttpOnly refresh cookie，建议再迁移以进一步降低 XSS 暴露面。
- `FE-009-16`：前端后台入口已使用明确的 `system:backend:access` 权限，但该权限字符串在数据库/角色中的迁移属于后端部署范围，本次没有擅自改线上数据。
- `FE-009-21/22/27`：Element Plus 组件上的历史 `v-has` 用法、旧递归组件清理和通用 resource composable 仍需要单独批次处理，避免一次性改动大量业务页面。

## 9. 本次未修改内容

- 未修改 FE-007 范围内已有的竞赛/作业页面、活动面板和 API 未提交改动。
- 未执行数据库、Docker、服务器部署或生产环境状态变更。
