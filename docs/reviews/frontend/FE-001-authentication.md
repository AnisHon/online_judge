# FE-001 前端认证模块 Review

## 1. Review 范围

- 认证页面：`oj-vue/src/views/authentication/Auth.vue`、`Login.vue`、`SignUp.vue`、`ForgetPassword.vue`
- 认证 API：`oj-vue/src/api/auth/authentication.ts`、`captchaCode.ts`、`emailCode.ts`
- 直接依赖：`oj-vue/src/utils/http.ts`、`stores/useToken.ts`、`stores/useUserStore.ts`
- 本次只做代码质量和 Bug review，不修改业务实现。

## 2. 结论摘要

当前认证流程的主路径可以工作，但存在几个会直接影响用户登录、注册或找回密码的问题：

1. 注册时的异步用户名/邮箱可用性校验没有处理请求失败，可能导致表单校验一直不结束。
2. 登录、注册、找回密码的提交入口缺少统一的请求锁，键盘回车、表单提交和按钮点击可能产生重复请求。
3. 验证码刷新没有统一的失败处理；失败时会出现未处理 Promise、验证码空白或旧 Token 继续使用。
4. 验证码/邮箱验证码/找回密码 API 对 `data` 为空的响应直接解构，会触发 `data is null` 类运行时异常。
5. 邮箱验证码发送没有独立 loading、倒计时或前端并发保护，存在重复发送和接口滥用风险。
6. 双 Token 被 Pinia 持久化保存，刷新 Token 会进入持久化存储，需要明确安全策略。

## 3. 问题清单

### FE-001-01 [高] 注册异步校验失败时不调用 callback

- 位置：
  - `oj-vue/src/views/authentication/SignUp.vue:184-198`
  - `oj-vue/src/views/authentication/SignUp.vue:216-230`
- 问题：`validateUsername` 和 `validateEmail` 调用 `checkAvailableUsername` / `checkAvailableEmail` 后只处理了 `.then()`，没有 `.catch()` 或 `async-validator` 的 Promise 返回值。
- 影响：网络异常、网关 502、接口 401/500 时，校验回调不会执行。表单可能一直处于校验中，用户看不到准确错误；同时会产生未处理 Promise rejection。
- 触发方式：注册页填写用户名或邮箱后断开网络，失焦或提交表单。
- 建议：统一改为返回 `Promise<void>` 的异步规则；请求失败时明确 `reject(new Error(...))`，并用请求序号或取消请求避免旧响应覆盖新输入。

### FE-001-02 [高] 登录/注册存在重复提交窗口

- 位置：
  - `oj-vue/src/views/authentication/Login.vue:21-24,70-76,145-155`
  - `oj-vue/src/views/authentication/SignUp.vue:21-24,130-136,263-273`
  - `oj-vue/src/views/authentication/ForgetPassword.vue:102-108,199-210`
- 问题：同一个流程同时存在表单 `submit`、输入框 `keyup.enter`、按钮 `click` 三类入口；`isLoading` 只在 `form.validate` 回调中设置，入口函数本身没有先判断请求是否已经进行。
- 影响：快速连续回车、回车和点击同时发生时，可能发出多个登录/注册/重置请求，造成验证码被消费、邮箱验证码状态异常、重复创建账号或重复刷新 Token。
- 建议：抽取统一的 `submitOnce`/提交锁，在进入校验前立即判断并设置状态；按钮使用明确的 `native-type="submit"`，删除重复的 `keyup.enter` 或按钮 click 入口。

### FE-001-03 [中] 验证码刷新失败未被页面处理

- 位置：
  - `oj-vue/src/views/authentication/Login.vue:157-165`
  - `oj-vue/src/views/authentication/SignUp.vue:293-300`
  - `oj-vue/src/views/authentication/ForgetPassword.vue:230-238`
  - `oj-vue/src/api/auth/captchaCode.ts:9-12`
- 问题：`refreshCaptchaCode` 直接 `await getCaptcha()`，调用方在 `onMounted`、组件 setup 和登录失败回调中都没有等待或捕获异常。
- 影响：验证码接口失败会产生未处理 Promise；图片可能保持空白，登录失败后的刷新还会产生第二个错误，用户无法判断到底是登录失败还是验证码服务异常。刷新失败时旧的验证码 Token 也可能继续保留。
- 建议：验证码 API 层返回明确结果或统一抛出 `ApiError`；页面维护 `captchaLoading`/`captchaError`，刷新成功后再替换图片和 Token，失败时清空 Token 并显示可重试状态。

### FE-001-04 [高] 多个认证 API 对空 data 直接解构

- 位置：
  - `oj-vue/src/api/auth/captchaCode.ts:11`
  - `oj-vue/src/api/auth/emailCode.ts:21,29`
  - `oj-vue/src/api/auth/authentication.ts:117`
- 问题：这些代码分别使用了 `const {data: {image, token}}`、`const {data: {message, success}}` 和 `const {data: {success, message}}`，没有先判断 `data` 是否存在。
- 影响：后端返回 HTTP 200 但业务 `data: null`、网关包装不完整或接口升级时，前端会抛出 `Cannot destructure ... as it is null`，真正的后端错误信息被遮蔽。这与此前出现的“`data is null`”属于同一类缺陷。
- 建议：在 API 层统一使用 `unwrapData`/响应 schema 校验；空数据转为带业务上下文的 `ApiError`，页面只处理标准错误对象。登录和注册当前已有部分 `!result.data` 检查，但应统一到 API 层。

### FE-001-05 [中] 邮箱验证码发送没有独立并发控制和冷却

- 位置：
  - `oj-vue/src/views/authentication/SignUp.vue:116-124,275-290`
  - `oj-vue/src/views/authentication/ForgetPassword.vue:88-96,213-227`
  - `oj-vue/src/api/auth/emailCode.ts:19-32`
- 问题：发送邮箱验证码的按钮没有 `isSendingEmail` 状态、倒计时或请求去重。注册页和找回密码页的按钮 loading 绑定的是提交用的 `isLoading`，发送邮箱验证码时按钮仍可重复点击。
- 影响：一次点击可能产生多次邮件请求，造成用户收到多封验证码，也会增加接口和邮件服务压力。
- 建议：抽取可复用的邮箱验证码发送 composable，至少包含请求锁、60 秒倒计时、失败恢复和按钮固定宽度；用户名/邮箱、图形验证码和 Token 在请求前统一校验。

### FE-001-06 [高] 双 Token 被持久化保存，存在长期暴露风险

- 位置：`oj-vue/src/stores/useToken.ts:7-12,22-38`
- 问题：Token Store 设置了 `persist: true`，access token 和 refresh token 都会进入 Pinia 持久化存储。当前没有看到只持久化必要字段或按会话模式切换的配置。
- 影响：只要页面存在 XSS 或浏览器持久化数据被读取，长期有效的 refresh token 可以被窃取并换取新的 access token；共享电脑上也会留下登录凭据。
- 建议：优先将 refresh token 改为 HttpOnly、Secure、SameSite Cookie，由后端负责保存；如果必须前端保存，应至少使用 sessionStorage、增加显式“记住登录”策略，并限制持久化字段和生命周期。此项需要和后端部署策略一起决定，暂不建议单独在前端盲改。

### FE-001-07 [中] setTokens 不会清理旧 refreshToken

- 位置：`oj-vue/src/stores/useToken.ts:22-25`
- 问题：只有在新 `refreshToken` 为 truthy 时才覆盖旧值。如果登录响应走旧的单 Token 兼容字段，或者刷新接口未返回 refresh token，旧账号的 refresh token 会继续留在 Store 中。
- 影响：可能出现新 access token 配旧 refresh token 的会话混用，造成偶发退出、刷新失败或账号状态错乱。
- 建议：`setTokens` 使用明确的 token bundle 替换语义；登录成功前先清理旧会话，或者把 `refreshToken` 设计为必填并在响应校验中拒绝不完整的双 key 响应。

### FE-001-08 [中] 忘记密码页面的验证码字段校验不完整

- 位置：
  - `oj-vue/src/views/authentication/ForgetPassword.vue:49-72,155-177`
  - `oj-vue/src/views/authentication/SignUp.vue:74-99,232-240`
- 问题：忘记密码的 `rules` 没有 `captchaCode` 规则，注册页的验证码规则被注释掉；发送验证码函数只在点击时手动检查，表单提交阶段不会检查验证码输入状态。
- 影响：页面上的必填校验表现不一致。用户可以绕过表单校验直接提交，直到后端或邮箱验证码流程返回错误；维护者也无法从 `rules` 判断完整表单约束。
- 建议：将验证码字段校验纳入统一表单规则；如果最终重置/注册接口不需要再次提交图形验证码，也应在 UI 上明确它只用于“发送邮箱验证码”，避免字段语义混乱。

### FE-001-09 [中] 认证错误处理职责分散，容易产生二次提示和未处理错误

- 位置：
  - `oj-vue/src/api/auth/authentication.ts:69-70,91-123`
  - `oj-vue/src/utils/http.ts:179-185`
  - 各认证页面的 `.catch()`
- 问题：认证 API 传入空的 `handledByAuthPage`，HTTP 层仍通过旁路 Promise 监听错误；页面再自行弹通知。当前依赖 `ApiError.notified` 和空回调来避免重复提示，职责和控制流不直观。
- 影响：未来任意一处改变旁路错误处理、增加新的 API 封装或抛出普通 Error，都可能出现重复通知或没有通知；验证码刷新等非认证请求还没有统一行为。
- 建议：HTTP 层只负责标准化错误和 401/403 会话处理，页面层负责表单错误提示；用明确的 `notify: false` 请求选项替代空函数和隐式旁路 catch。

### FE-001-10 [低] 认证模块存在明显的重复和死代码

- 位置：
  - `Login.vue:104-122` 的 `validateNotEmpty` 未使用
  - `SignUp.vue:200-206` 的 `validateNotEmpty` 未使用
  - `ForgetPassword.vue:151-159` 的 `validateNotEmpty` 未使用
  - `ForgetPassword.vue:127-135` 的 `nikeName`、`email` 未参与请求
  - `authentication.ts:127-132` 的 `resetPassword`、`144-149` 的 `refreshLogin` 当前未找到调用方
- 问题：认证页面各自复制密码校验、重复密码校验、验证码刷新和错误提取逻辑；同时保留了未使用的字段和 API。
- 影响：修复规则或交互时容易出现登录、注册、找回密码行为不一致；死代码会掩盖真正的接口入口。
- 建议：先抽取小型认证表单规则和验证码 composable，再清理未使用字段/API。不要直接大规模重构，先以 FE-001-01～05 的行为修复为优先。

## 4. 测试与验证

### 已执行

- `pnpm exec vitest run`

### 测试结果

- 失败：`src/__test__/common.test.ts` 在加载 Element Plus CSS 时出现 `Unknown file extension ".css"`。
- 该测试文件显示 `0 test`，当前没有真正覆盖登录、注册、验证码或找回密码流程。
- 因此本次结论主要来自源码和接口契约检查，不能替代浏览器端交互测试。

## 5. 建议修复顺序

1. 先修 FE-001-04：统一 API 空数据和错误结构，解决 `data is null` 类问题。
2. 修 FE-001-01、FE-001-02：收口异步表单校验和提交锁。
3. 修 FE-001-03、FE-001-05：统一验证码刷新和邮箱验证码发送状态机。
4. 单独评审 FE-001-06、FE-001-07 的 Token 持久化和双 key 生命周期，需和后端一起定方案。
5. 最后清理 FE-001-08～10，并补最小化认证流程测试。

## 6. 本次未修改内容

- 未修改认证页面、API、Token、HTTP 或后端代码。
- 未提交 Git。
- 仅新增本 review 报告。
