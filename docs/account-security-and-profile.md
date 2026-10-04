# 账号限制、敏感操作验证与提交热力图

## 角色承载的账号限制

在权限管理/角色分配中配置下列独立按钮权限，然后将角色分配给用户。不会自动创建限制角色，也不会给现有角色默认授予禁止权限。

| 权限字符串 | 效果 |
| --- | --- |
| `policy:solution:deny` | 拒绝题解新增、修改、删除和置顶等写入；保留阅读 |
| `policy:avatar:deny` | 拒绝用户上传自己的头像 |
| `policy:comment:deny` | 预留评论限制；当前项目尚无评论模块 |
| `user:user:reset-avatar` | 管理员通过用户管理恢复指定用户的默认头像 |

禁止权限先于正向写入权限生效。管理员的头像重置属于独立审核操作，即使目标用户不能更换头像，也可以由管理员重置。超级管理员角色不会因“全部授权”被自动赋予禁止权限；实际赋予某用户的禁止角色仍然生效。配置生成的虚拟 `root` 账号会排除禁止权限。

独立禁止权限不依赖后台父路由。现有文件上传/重命名权限也迁为独立权限，并继续保留原来的正向授权要求。后台业务管理权限仍要求祖先路由授权，前端提交前检查，后端授予时兜底。

移除确认冗余的 `problem:submit:read`、`content:file:download`、`content:info`。查看本人提交继续要求登录并验证所有权；文件下载继续使用现有的登录检查；站点公开内容不受这些权限控制。

角色/菜单权限变更提交后刷新角色菜单缓存及已有登录快照，不为已退出的账号创建新会话，不延长原登录缓存 TTL。已有前端页面的用户权限快照在刷新页面/重新加载用户信息后更新。

## 统一敏感操作验证

普通业务 API 继续使用内存 Access Token；HttpOnly Refresh Cookie 和 Gateway 的鉴权职责保持不变。敏感操作由 user-service 的 `StepUpService` 统一验证，账号变更由 `AccountSecurityService` 处理。

1. 个人主页的“账号安全”选择修改密码或绑定/修改邮箱。
2. 使用当前密码验证；已绑定邮箱的用户也可选择绑定邮箱验证码。
3. 服务端签发 300 秒、单次使用的操作凭证。凭证绑定用户、用途及当前密码/邮箱状态，Redis 只存凭证摘要。前端只在对话框内存保存它，关闭后清空。
4. 修改密码时消费 PASSWORD 凭证；修改邮箱时同时消费 EMAIL 凭证和**新邮箱**验证码。
5. 修改密码成功后撤销该用户所有 Refresh Session，并清除登录缓存及 Refresh Cookie；前端清除内存会话并广播退出事件。修改邮箱更新当前登录快照，保留会话。

敏感操作需要重新验证身份的依据参见 [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#require-re-authentication-for-sensitive-features)。此实现不是 MFA，密码验证和邮箱验证为可选择的身份确认方式。

新接口（通过 Gateway 调用时添加 `/api/user-api`）：

| 方法与路径 | 主要请求字段 |
| --- | --- |
| `POST /auth/step-up/verify` | `action: PASSWORD/EMAIL`、`method: PASSWORD/EMAIL`、`password` 或 `code` |
| `POST /auth/step-up/email-code` | `action`、`captchaToken`、`captchaCode`；接收邮箱从服务端当前账号读取 |
| `POST /auth/step-up/new-email-code` | `stepUpToken`、`newEmail`、图形验证码 |
| `PUT /auth/reset-pass` | `stepUpToken`、`password` |
| `PUT /auth/reset-email` | `stepUpToken`、`newEmail`、`newEmailCode` |

旧的登录态密码/邮箱修改请求只有 `code` 时会被拒绝，需要配套更新前端。独立“忘记密码”流程保留，不能使用敏感操作邮箱验证码代替找回密码验证码。验证接口要求登录，包括开发模式。验证码及凭证在 Redis 中使用 SHA-256 摘要，验证码为六位安全随机数；密码、验证码和操作凭证从相关 DTO 日志字符串中排除。

每账号十分钟最多五次身份验证请求，目标邮箱验证另计五次；每个发送阶段每账号一分钟一次。验证码与凭证均为五分钟 TTL，Redis Lua 原子比较和消费，避免并发重复使用。身份验证码与新邮箱验证码使用不同命名空间；后者绑定账号及目标邮箱。旧凭证在密码/邮箱修改后自动失效。虚拟 root 账号不在 sys_user 中，其账号配置仍由环境配置管理。

未绑定邮箱的用户每次主动登录后收到提醒，可前往账号安全或稍后处理。同一会话内切页、刷新和 silent refresh 不重复弹窗；下次主动登录再次提醒。绑定邮箱后同步当前用户状态，不在他人的公开主页暴露邮箱或安全设置。

## 缓存与热力图

热力图位于练习 tab 的已通过题目前。按北京时间聚合最近一年**提交次数**，包含当天，最多 366 个按天汇总项，不返回提交详情；不限于 AC，重复提交分别计数。空日期补零。复用个人主页两小时缓存，不进行轮询，也不为每次提交强制失效。

新增/变更的 Redis 前缀：

| 前缀 | 用途/TTL |
| --- | --- |
| `problem:profile:v2:` | 个人主页与热力图；两小时；替代 v1 |
| `problem:profile:v1:` | 旧缓存不再使用，保留缓存管理兼容清理入口，等待原 TTL 自然过期 |
| `user-service:step-up:grant:` | 单次操作凭证摘要；五分钟 |
| `user-service:step-up:identity-code:` | 当前邮箱验证码摘要，绑定用户、用途、邮箱；五分钟 |
| `user-service:step-up:new-email-code:` | 目标邮箱验证码摘要，绑定用户、邮箱；五分钟 |
| `user-service:step-up:mail-cooldown:` | 按账号、发送阶段限制频率；一分钟 |
| `user-service:step-up:attempts:` | 按账号、验证阶段计数；十分钟 |

新前缀已加入缓存管理目录，所有 step-up 内容沿用认证缓存的敏感值隐藏规则。

## 数据迁移和验证

新增两份可重复执行的迁移，开发基础设施脚本已纳入，服务器现有 `migration/V*.sql` 流程也会执行：

- `V20261003_1__account_policies.sql`：清理冗余权限、迁出独立文件权限、创建三个禁止权限和重置头像权限，仅向 admin/super_admin 默认授予重置头像。
- `V20261003_2__profile_heatmap_index.sql`：添加 `submit_log_user_submit_time(user_id, submit_time)` 覆盖热力图范围查询。

本次本地迁移前备份 sys_menu/sys_role_menu，两份迁移均重复执行成功。SQL EXPLAIN 确认热力图使用新增索引。不会修改现有题目、提交或用户数据。上线需同时更新 user/content/problem-service、共享 common-api 与前端；不要单独发布新的账号安全前端而保留旧认证接口。服务重启清理菜单缓存，现有用户重新加载账号快照后看到新权限。

验证命令：

```bash
JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk mvn -B -Dmaven.jar.forceCreation=true package
cd oj-vue
pnpm run build
pnpm exec vitest run
```

Redis 集成测试需本地 Redis 可达时显式启用：

```bash
OJ_SECURITY_REDIS_TEST=true JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk mvn -B -pl user-service -am test
```

集成测试仅操作随机虚拟用户的隔离键，并在结束时逐个清理；不接触真实用户、数据库和 SMTP。实际执行已验证凭证 TTL/过期、单次使用、跨用户/用途拒绝、四请求并发仅一次成功、邮箱验证码重复使用拒绝、错误新邮箱验证码不消费凭证、正确验证码与凭证原子消费、修改密码撤销 Refresh Session 和登录缓存。完整后端构建及 42 项测试（包含八项实际 Redis 测试）、前端构建、类型检查和 13 项前端测试通过。

SMTP 实际投递、真实浏览器弹窗交互和线上 MinIO 头像重置尚未进行端到端验证；已有邮件服务仍异步发送，投递失败需要查看服务端邮件日志。本次未部署到服务器。
