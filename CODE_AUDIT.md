# Online Judge 代码审查报告

审查日期：2026-09-11  
范围：`oj-vue`、`gateway-server`、`commons`、`common-api`、`user-service`、`problem-service`、`judge-server`、`content-service`、Docker 与部署脚本。  
方式：静态代码审查与已运行 dev 环境的配置交叉核验；未做渗透测试，也未修改业务代码。

## 结论与优先级

| 优先级 | 数量 | 含义 |
| --- | ---: | --- |
| P0 | 4 | 可直接导致越权、凭据泄露、判题/提交丢失，应优先处理 |
| P1 | 7 | 会造成数据泄露、结果错误、稳定性或线上安全风险 |
| P2 | 4 | 可维护性、性能、健壮性与测试风险 |

建议修复顺序：先收紧内容服务公开接口和网关请求头，再处理 JWT/密钥管理，然后修正判题可靠性与竞赛计分，最后补测试与可观测性。

## P0：应优先修复

### A-01 未登录可伪造 `user-id` 上传/覆盖任意用户头像

- 模块：网关、公共鉴权、内容服务
- 证据：[`SecurityConfig.java`](common-api/src/main/java/com/anishan/api/config/SecurityConfig.java:40) 将 `/avatar/**` 设为公开；[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:79) 的 `POST /avatar` 直接信任 `user-id` 请求头；[`AuthFilter.java`](gateway-server/src/main/java/com/anishan/gateway/filter/AuthFilter.java:33) 无 token 时原样放行请求头。
- 影响：外部调用者可在没有 token 的情况下携带任意 `user-id` 上传头像，存在越权篡改与对象存储滥用风险。
- 建议：上传接口改为认证接口，用户 ID 只从已认证的 `SecurityContext` 取得；网关应先移除外部传入的 `user-id`，校验 JWT 后以单一可信值回填。

### A-02 图片上传接口公开，允许匿名消耗对象存储

- 模块：内容服务、公共鉴权
- 证据：[`SecurityConfig.java`](common-api/src/main/java/com/anishan/api/config/SecurityConfig.java:53) 放行 `/image/**`；[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:103) 的 `POST /image` 无认证、权限和文件类型/总大小限制。
- 影响：任意访客可持续上传任意内容，导致存储费用、带宽、恶意文件托管和可用性风险。
- 建议：仅公开图片读取路由，将上传迁到受保护的独立路径；在网关和服务两层限制 MIME、魔数、单文件/请求总大小、频率与配额。

### A-03 JWT 签名密钥硬编码且强度不足

- 模块：公共库、用户服务、网关
- 证据：[`JwtUtil.java`](commons/src/main/java/com/anishan/commons/util/JwtUtil.java:20) 使用固定的 `"legacy-secret-redacted"` 作为所有环境的 HMAC 密钥。
- 影响：源码、镜像或日志泄露后可伪造任意用户的 7 天有效 token；无法无停机轮换密钥。
- 建议：从密钥管理服务/部署环境注入至少 256 bit 的随机密钥；支持 `kid` 与双密钥轮换；同时校验 issuer、audience、算法白名单并缩短访问 token 有效期。

### A-04 判题消息异常后被确认，提交可能永久丢失

- 模块：判题服务、RabbitMQ
- 证据：[`JudgeListener.java`](judge-server/src/main/java/com/anishan/judge/mq/JudgeListener.java:81) 捕获全部异常后只记录日志，方法正常返回；Spring AMQP 默认自动确认时，RabbitMQ 会确认该消息。
- 影响：判题、保存提交日志或回写题目服务任一阶段失败时，用户只会收到运行时错误/无结果，消息不会重试也不会进入死信队列。
- 建议：使用手动 ack 或让可重试异常向上抛出；配置重试、指数退避、DLQ、告警与按 `uuid` 的幂等回写；将“判题执行”和“回写结果”状态持久化。

## P1：高风险功能与一致性问题

### A-05 MinIO access key 与 secret key 被完整写入日志

- 模块：公共 API
- 证据：[`MinioConfig.java`](common-api/src/main/java/com/anishan/api/config/MinioConfig.java:45) 与 [`MinioConfig.java`](common-api/src/main/java/com/anishan/api/config/MinioConfig.java:74) 输出 access key、secret key。
- 影响：拥有应用日志权限的人可直接读取对象存储凭据；集中日志、报错导出和备份都会扩大暴露面。
- 建议：删除两处 secret 日志，最多记录 endpoint、bucket 和脱敏后的 access key；轮换当前已使用的 MinIO 凭据。

### A-06 基础设施密码通过 Java 进程命令行暴露

- 模块：部署、题目/判题/内容服务
- 证据：各服务 Dockerfile 将 `MQ_PASSWORD` 等展开为 `java -jar ... --oj.mq.password=...`，例如 [`judge-server/Dockerfile`](judge-server/Dockerfile:18)、[`content-service/Dockerfile`](content-service/Dockerfile:13)。
- 影响：容器内 `ps`、诊断采集、部分 APM 或崩溃报告可看到密码；明文配置也在 Nacos/Compose 之间重复传播。
- 建议：保留为环境变量或挂载只读 secret 文件，由 Spring 外部化配置读取；生产使用 Docker/Kubernetes secrets，禁止在命令行拼接密码。

### A-07 竞赛部分得分可能被错误算为零

- 模块：判题服务
- 证据：[`DefaultJudgeImpl.java`](judge-server/src/main/java/com/anishan/judge/judge/impl/DefaultJudgeImpl.java:167) 使用 `score.divide(totalScore, RoundingMode.FLOOR)` 后再乘题目分数。
- 影响：该重载通常按被除数 scale 进行整除；常见的 `50 / 100` 会得到 `0`，从而把部分通过的竞赛题算成 0 分。
- 建议：指定足够精度，例如 `score.divide(totalScore, 8, RoundingMode.DOWN).multiply(listScore)`，并按业务规则在最终分数处统一保留小数位；补边界测试（0、部分通过、满分、非整除）。

### A-08 判题线程池满时静默丢弃旧测试用例任务

- 模块：判题服务
- 证据：[`DefaultJudgeImpl.java`](judge-server/src/main/java/com/anishan/judge/judge/impl/DefaultJudgeImpl.java:42) 的拒绝策略为 `DiscardOldestPolicy`。
- 影响：高并发或大测试集时，最早排队的 case 会被悄悄丢弃，结果可能错误地少计 case、卡住或产生不完整分数，且没有告警。
- 建议：改用 `CallerRunsPolicy`、显式拒绝并把“系统繁忙”回写给用户，或按提交维度限流；为单次提交设置 case 数与并发上限。

### A-09 判题 Future 无总超时，线程和消息可能长期占用

- 模块：判题服务
- 证据：[`DefaultJudgeImpl.java`](judge-server/src/main/java/com/anishan/judge/judge/impl/DefaultJudgeImpl.java:138) 对每个 Future 调用无超时的 `get()`。
- 影响：Sandbox、网络或存储异常未及时返回时，一个 RabbitMQ consumer 可长期阻塞；队列会堆积，重启前难以恢复。
- 建议：使用 `get(timeout, unit)` 或 `orTimeout`，失败后取消尚未完成任务、清理 sandbox 文件并回写系统错误；将超时指标接入监控。

### A-10 内容服务允许按任意对象键读取文件，缺失所有权校验

- 模块：内容服务
- 证据：[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:137) 的 `/file?path=` 直接把客户端 path 交给对象存储；[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:237) 下载接口同样直接使用 `fileName`。
- 影响：即使生产环境要求登录，任意登录用户仍可能枚举/猜测对象键并读取其他用户、题目测试数据或内部文件；`/file` 的 `@PermitAll` 注解与实际全局规则也不一致，易产生部署环境差异。
- 建议：客户端只传文件记录 ID 或签名短期 URL；服务端查询记录并校验 owner/课程/题目权限；对象键采用不可猜测前缀，测试用例对象与用户云盘隔离 bucket/前缀。

### A-11 公开文件读取的 Range 处理可异常或构造非法响应

- 模块：内容服务
- 证据：[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:266) 直接 `Long.parseLong` Range 值；未校验负数、`start > end`、`start >= fSize`，且 Content-Range 缺少空格。
- 影响：恶意请求会触发 500、负长度或不符合 HTTP Range 规范；下载链路稳定性较差。
- 建议：严格解析单 range，非法 range 返回 `416 Range Not Satisfiable` 和 `Content-Range: bytes */size`；使用框架/库支持的 range resource 写法替代手工实现。

## P2：可靠性、设计与维护问题

### A-12 验证码读取再删除不是原子操作

- 模块：公共 API、用户服务
- 证据：[`AuthUtil.java`](common-api/src/main/java/com/anishan/api/util/AuthUtil.java:147) 先 GET 再 DEL；[`AuthUtil.java`](common-api/src/main/java/com/anishan/api/util/AuthUtil.java:209) 依赖该实现校验图形验证码。
- 影响：并发请求可能同时读取到同一个验证码，破坏“一次性验证码”语义。
- 建议：用 Redis `GETDEL`（Redis 6.2+）或 Lua 脚本比较并删除；邮箱验证码也应在成功消费后原子删除。

### A-13 忘记密码的发码接口对不存在用户可能抛 500，且泄露用户枚举信号

- 模块：用户服务
- 证据：[`AuthenticationController.java`](user-service/src/main/java/com/anishan/user/controller/AuthenticationController.java:163) 未判空便调用 `getUserByUsernameOrEmail(...).getEmail()`；[`AuthenticationController.java`](user-service/src/main/java/com/anishan/user/controller/AuthenticationController.java:122) 的找回密码接口则明确返回“用户不存在”。
- 影响：不存在用户名会产生空指针 500，且不同响应可被用于枚举帐号。
- 建议：对外统一返回成功提示，不暴露用户是否存在；内部记录审计日志，加入 IP/帐号限流。

### A-14 开发/生产授权策略依赖单个布尔开关，误配会将全部业务接口开放

- 模块：公共 API、配置中心
- 证据：[`SecurityConfig.java`](common-api/src/main/java/com/anishan/api/config/SecurityConfig.java:83) 仅当 `sharedConfig.isProduct()` 为真时要求认证；否则 `anyRequest().permitAll()`。
- 影响：生产环境缺失、拼错或读取失败 `oj.product` 时，会将绝大多数接口开放；微服务直接暴露在内部网络时风险更高。
- 建议：安全默认值应为“拒绝/要求认证”，仅对本地 profile 显式放宽；将生产模式用不可缺失的 profile 或部署清单约束，并在启动时对不安全组合 fail-fast。

### A-15 测试覆盖极低，关键链路没有回归保护

- 模块：全项目
- 证据：目前仅发现 3 个 `@Test`，位于 user/problem/judge 服务；未发现前端单测覆盖核心登录、路由和请求拦截。
- 影响：上述鉴权、计分、消息确认和部署问题很容易在重构后复发。
- 建议：优先补充 JWT/网关头覆盖、匿名上传拒绝、文件授权、竞赛分数、MQ 重试/DLQ、判题超时，以及 Vite `/api` 代理的集成测试；CI 中运行后端测试、前端 `pnpm run type-check` 与 `pnpm test`。

## 模块补充观察

| 模块 | 观察 |
| --- | --- |
| 前端 `oj-vue` | `/api` 代理已在本轮之前修复为可配置目标；`http.ts` 仍包含较多 `any`、空 `defaultFail` 与类型强转，错误处理可预测性不足。Token 持久化在浏览器存储中，需接受 XSS 风险或改为更安全的会话方案。 |
| 网关 | `AuthFilter` 对异常 token 的处理依赖全局异常链；建议明确返回 401，并移除外部 `user-id`/`token` 的多值请求头后重建。 |
| 用户服务 | 默认密码配置为 `change-this-default-password`（`resources/config/user-service.yaml`），应仅作为 dev seed，生产强制通过 secret 覆盖；退出登录会按用户 ID 删除登录缓存，当前模型意味着同用户多端会话会被全部踢出，需确认是否符合业务。 |
| 题目服务 | `JudgeResultListener` 明确标注“未完成，未使用”；`SolutionController` 自注释“数据权限没做好”，应在开放题解编辑/删除前补对象级权限检查。OJ 测试用例的数据库和对象存储操作没有跨资源补偿，失败时会残留孤儿对象或数据库记录。 |
| 判题服务 | 除 A-04/A-07/A-08/A-09 外，判题任务的幂等键、重复消费处理、提交状态机和运行指标不足；建议把这些作为单独的可靠性改造。 |
| 内容服务 | 文件对象、用户云盘、头像、题目测试用例共用的访问模型需要拆分；文件响应异常被吞掉（[`FileController.java`](content-service/src/main/java/com/anishan/content/controller/FileController.java:73)），不利于排障和告警。 |
| 部署 | Nacos、MinIO、数据库和 RabbitMQ 密码在配置与启动参数间多处复制；dev 一键脚本已修复当前可运行问题，但生产应改用 secret 注入、健康检查和版本化迁移。 |

## 不在本次报告中下结论的项

- 前端的 `@ts-ignore`、`any` 和部分空 catch 已列为工程质量风险，但未逐条认定为运行时缺陷。
- SQL Mapper 的权限条件、索引和慢查询需要结合真实数据规模与执行计划复核，本次未把“可能慢”列为确定问题。
- Sandbox 镜像以 privileged 模式运行是判题类产品常见但高风险的架构选择；是否可接受取决于宿主机隔离、网络策略和 go-judge 配置，建议单独进行容器逃逸与资源隔离审查。

## 建议的修复批次

1. **安全热修**：A-01、A-02、A-03、A-05、A-06、A-10、A-14；轮换已暴露密钥。
2. **判题正确性与可靠性**：A-04、A-07、A-08、A-09；先补测试再改消息确认与线程模型。
3. **文件与账号体验**：A-11、A-12、A-13；统一文件授权模型和验证码原子消费。
4. **工程治理**：A-15、对象级权限、迁移管理、CI、监控告警。
