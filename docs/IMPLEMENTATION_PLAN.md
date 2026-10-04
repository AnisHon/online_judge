# 关注、个人通知、题解互动与比赛最终榜单实施方案

日期：2026-10-03。范围：需求 21～24。架构修订：T01/T02 已执行，T03+ 尚未执行。题解归属 content domain；下文为修订后的目标契约，并非宣称新功能已上线。

执行清单见 [LUNA_TASKS.md](LUNA_TASKS.md)。两份文档中的接口、字段、状态和任务编号是一套契约；任务执行者不得另选架构。本文包含决策理由，执行文档只包含落实指令。

## 0. T01/T02 已执行后的架构修订（本次只改计划）

### 0.1 实际工作区审查与处理清单

先执行了 `git status --short` 和 `git diff`，再读取未跟踪文件及直接调用链。工作区含 Step-up、禁止权限、热力图等其他修改，不能将整个 diff 当作 T01/T02，也不能回退这些修改。以下是实际代码而非原计划推断。

| 实际实现 | 处理决定 | 后续任务 |
| --- | --- | --- |
| T01：db_user 四表；UserFollow/UserNotification/NotificationFanoutJob/UserPointAwardReceipt 实体、Mapper/XML、状态枚举、UserSocialMapperTest | 全部保留；没有业务 API/消费者，不重复实施 T01 | T04/T05 在这些基础上扩展 |
| T01：commons ApiStatusException、common-api GlobalExceptionAdvice 的精确 HTTP 状态/脱敏 handler、测试依赖及 AdviceTest | 原地保留，各服务复用，不复制异常体系 | 所有新 API |
| T02：SolutionExplanation 新增七字段、四个互动/审计实体及其 Mapper/XML、两个状态枚举、InteractionSchemaTest | 迁入 content-service，语义、ID、状态、约束不变；测试拆分保留 | R02-B/R02-C |
| 旧题解正文实体/表、CRUD/detail/list/recent Controller/Service/DTO/VO/Mapper | 连同旧实现一起迁移，不能只迁新实体；删除跨域 SQL JOIN 和隐式物理删除 | R02-C |
| T02：ProblemEventOutbox 实体/Mapper/XML/table | 留在 problem；仅用于积分/判题派发。另建同结构 ContentEventOutbox，用于七类社区事件 | R02-B、T03-C/T03-P |
| T02：db_user 初始化和 V20261004_3 权限迁移 | 保留 comment:list/remove、judge:dispatch:retry、admin/super_admin 路由祖先和 contest:rank grant；不重命名现有权限字符串 | 不重做 T02 权限 |
| db_problem 初始化中的题解/正文/互动表与题目外键 | 新装结构迁往 db_content，取消跨域题目外键；既有库旧表暂留只读回滚备份，不自动 DROP | R02-B |
| ProfileMapper.selectSolutions、ProfileService 的两小时完整活动缓存 | 题解查询迁 content；problem 只缓存练习/比赛/热力图；前端组合三个域接口 | R02-D |
| ProblemServiceImpl.removeByIds 中的 SolutionExplanation 引用检查 | 删除本地题解查询，不改提交/作答/活动引用保护；题目只能逻辑删除，content 保留历史关联并即时限制公开读取 | R02-C |

SolutionExplanation 当前同时有 `@Version updateTime` 和未标注的 Long version：不能直接把两个字段都设成 optimistic-lock 字段。修订后仅 Long version 是并发版本，updateTime 是时间；事务显式按旧 version 更新并递增，不能依赖插件隐式改变业务状态。

### 0.2 迁移执行证据与历史风险

T02 的先前实施记录是在隔离的临时 MySQL 8.0.27 验证重复迁移，不能据此证明真实开发/生产库没有执行。**本次没有连接真实数据库或服务器，实际 schema/执行记录未知。** 当前 server deploy.sh 会遍历全部 V*.sql，而 apply.sh/dev 清单不同；历史脚本可能被其他启动/发布执行，且没有足够的统一 checksum journal 证明未执行。

因此冻结已写入的三份历史迁移，不改名、不改 USE、不重写内容：

| 文件 | 当前 SHA-256（作为修订基线，不等于数据库执行证明） |
| --- | --- |
| V20261004_1__user_social.sql | 667063a2bd0f1d8ca8d52eb01b5e5842509b6b18c07c5fad0172b7523bf64751 |
| V20261004_2__solution_interactions.sql | 54dcaf7e041bd73a37044e9ca10ef4ed2806e530ee6519b12e05d3d68c383d32 |
| V20261004_3__interaction_permissions.sql | 7613129c62eb4260ceafaec6a64a941385413cf6f9a99fbc65ff09061c9d4802 |

新增 forward migration：`V20261004_4__content_solution_schema.sql`、`V20261004_5__content_solution_transfer.sql`。原计划的 contest migration 尚未创建，将其编号改为 `V20261004_6__contest_final_rank.sql`，仅调整未实施文件的编号，不改比赛设计。transfer 是显式维护阶段，禁止自动 V*.sql 遍历执行。

### 0.3 修订原则

T01/T02 是已完成历史，不是下一轮 agent 的实施任务。先完成 R02-A～D correction，再按依赖实施后续功能；只在被指派时执行对应 Task。保留所有原先的并发、幂等、outbox、MQ、纯文本评论、权限、安全、Long ID、HTTP 状态、排名与历史成绩约束。领域迁移必须调整的分页/跨域一致性语义在 §2.1 明确，而不是让 agent 再做选择。

## 1. 分析范围与当前基线

### 1.1 阅读方式与边界

已检查根 POM、各服务依赖与入口、公共配置、Gateway、相关 Controller/Service/Mapper/XML、初始化 SQL 与增量迁移、部署/开发迁移入口、前端路由/API/相关页面及测试。对题解、比赛、提交、判题结果、认证和缓存追踪了实际调用链；另有两个只读审查 agent 分别交叉检查题解/身份和 MQ/缓存。没有修改业务代码、执行迁移或部署服务器。

基线是**当前工作区**，包括尚未提交的禁止权限、Step-up Authentication 和个人主页热力图修改，不只是 HEAD。后续实施必须保留这些改动。未核验线上数据库实际版本，部署任务必须先执行 schema/迁移预检查；本次不能把仓库 SQL 等同于线上实际 schema。

原始设计阶段验证（不是本次重新运行）：`JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk mvn -B -o test` 成功；当前存在的测试源对应 34 个通过、1 个 Redis 集成测试类条件跳过。`pnpm exec vitest run`：3 个文件、13 个测试全部通过。旧的已删除测试仍可能留有过期 Surefire 报告，不把这些报告当作本次失败。没有新增功能的运行验证，也没有重建/启动生产服务。

### 1.2 模块分工

| 模块 | 现在的职责 | 本需求新增职责 |
| --- | --- | --- |
| `commons` | `R`、`PagedQuery/PagedResult`、业务异常、枚举、JWT 工具 | 不搬入业务 Service；必要的共享枚举仍保持小范围 |
| `common-api` | Feign 契约、Redis/Rabbit/Security 配置、登录身份、缓存、公共异常 | 小型事件契约、安全用户摘要契约；不负责写跨服务数据库 |
| `gateway-server` | `/user-api`、`/problem-api`、`/content-api` 路由、Access JWT、阻断 internal | 保持职责；阻断遗留伪造判题结果入口，不放行个人消息匿名读取 |
| `user-service` / `db_user` | 用户、班级、角色权限、认证、积分、公开资料 | 关注关系、个人通知持久化/已读、消息消费与关注者扇出、幂等积分收据 |
| `problem-service` / `db_problem` | 题目/题单、比赛/作业、提交/判题记录 | 题目只读资格 API；积分/派发 outbox；比赛评分投影与最终榜单。当前旧题解代码待迁出 |
| `judge-server` | 消费判题任务、编译、按用例判题、并发信号量、回调结果 | 保持计算职责，配合幂等回调；不创建榜单、不写通知 |
| `content-service` / `db_content` | 公告、FAQ、MinIO 文件/头像、缓存管理 | 题解及正文、可见性/审核、点赞、评论/回复/评论点赞、内容 outbox；公告不是个人通知 |
| `oj-vue` | Vue 3 + Pinia + Element Plus + Vite、API 包装、静态/动态路由 | 关注入口、个人消息收件箱、文本评论、互动按钮、最终成绩展示 |

不新增微服务、不引入 Elasticsearch、WebSocket/SSE、OAuth 或单独排行榜引擎。

### 1.3 现有规范与复用点

* Spring Boot 2.7.6、Java 11、MyBatis-Plus 3.5.5，实体/DTO/VO 分开；ServiceImpl + Mapper/XML 是现有持久化方式。新增复杂 SQL 放 Mapper，不在 Controller 拼 SQL。
* 统一响应 `R<T> = {code,message,data}`；分页 `PagedResult<T> = {currentPage,pageSize,totalRecords,data}`，不是新建另一套响应格式。新分页 DTO 限制 `currentPage>=1`、`1<=pageSize<=50`（默认 20）；不能沿用允许 `pageSize=0` 的无上限行为。
* JSON 内所有业务 Long ID、版本/序号和 Map 的 ID key 使用十进制字符串；前端 `IdType=string`。禁止先转 `Number` 再转回字符串。分数统一 `BigDecimal`，输出精确十进制字符串，显示时格式化，不在 JS 重算排名。
* `GlobalExceptionAdvice` 已在生产隔离未知异常，但普通 `BusinessException` 和方法级 `AccessDeniedException` 当前通常只写 `R.code`、HTTP 仍为200。T01已经新增 `ApiStatusException extends BusinessException`，只允许400/401/403/404/409/429/503，经专门的 `ResponseEntity` handler返回一致HTTP/R.code并复用安全消息过滤；现有AccessDenied/IllegalToken handler补403/401。旧普通BusinessException分支保持兼容。内部 SQL、堆栈、判题基础设施错误不得进通知或用户 VO，不能使用被通用handler吞掉的ResponseStatusException冒充实现。
* Gateway `AuthFilter` 删除浏览器自带的 `user-id`，验证 Access JWT 后注入身份；`UserAuthenticationFilter` 从 Redis 的 `LoginUser` 建立 SecurityContext。业务 Service 使用 `AuthUtil`，不得信任请求 body 的 actor/author/recipient ID。
* 当前认证已经是内存 Access Token + HttpOnly Refresh Cookie + Redis Refresh Session。`useToken.ts` 无 Token 持久化，`authBridge.ts` 只传播状态。新功能不得改变这一机制。
* 正向管理权限用 `@PreAuthorize(hasAuthority(...))`；当前工作区 `AccountPolicy` 已有 `policy:solution:deny`、`policy:comment:deny`、`policy:avatar:deny`。普通社交操作用登录身份/所有权，不要求给 student 默认授予无意义的按钮权限。
* `UserInternalClient.nikeName` 可批量补昵称；`UserClient.listUser` 要管理权限且返回邮箱/备注，不适合评论/榜单。新增安全批量摘要契约，不能复用管理 UserVo 作为公开数据。
* 当前“公开资料/公开题解”是对其他登录用户公开，前端相关路由仍 `requireAuth`，生产 SecurityConfig 也没有全面放行游客。保持现有边界；平台公告/FAQ的游客可见性不变。

### 1.4 关键代码证据

| 调用链/事实 | 需要依据的真实文件 |
| --- | --- |
| 普通/后台题解接口分离 | `problem-service/.../controller/SolutionController.java`、`service/impl/SolutionExplanationServiceImpl.java` |
| 旧题解/正文仍在 db_problem；T02 已补互动表/删除和审核字段，尚无业务能力 | `resources/sql/db_problem.sql` 的 `solution_explanation`、`solution_explanation_content` |
| 现有通知页其实是公告 | `oj-vue/src/views/notification/Notification.vue`、`content-service/.../controller/NoticeController.java` |
| 比赛与作业共用活动模型，0 CONTEST / 1 HOMEWORK | `ContestController.java`、`ContestServiceImpl.java`、`commons/.../enumeration/ContestType.java` |
| 成绩来自每人每题一条投影，不是提交次数汇总 | `RecordsServiceImpl.java`、`mapper/RecordsMapper.xml`、`contest_records` 唯一键 |
| 交卷是独立 user_submit，不会主动发起所有 OJ 判题 | `ContestController.submit`、`RecordController.save`、`JudgeController.judge` |
| 有效 OJ 链路 | `JudgeServiceImpl.judgeOj -> SubmitLogServiceImpl.createQueued -> JudgeClient.judge -> judge-server JudgeServiceImpl.sendJudgeMessage -> JudgeListener -> DefaultJudgeImpl -> /internal/judgeResult` |
| 实际用例评分/并发 | `judge-server/.../judge/impl/DefaultJudgeImpl.java`；不要把该模块旧 `service/impl/JudgeServiceImpl` 的同步残留误认为实际评分路径 |
| 缓存键生成/序列化 | `common-api/.../aspect/CacheAspect.java`、`config/MvcConfig.java`、`config/RedisConfig.java` |
| MQ JSON 与投递配置 | `common-api/.../config/RabbitConfig.java`、`resources/config/shared-rabbit.yaml` |
| 迁移入口有差异 | `resources/sql/migration/apply.sh`、`scripts/start-dev-infra.sh`、`deploy/server/deploy.sh` |

上述 `...` 在本文指 Java 包根；执行文档定义了可直接定位的路径缩写。

### 1.5 必须连同需求修复的真实问题

1. `SolutionExplanationServiceImpl.get` 的 `@Cacheable(key="#id")` 在方法执行前命中；方法中的私有作者检查不会执行。作者访问后可能把私有详情缓存给他人。这不是简单补 eviction 能解决的问题。
2. `recent` 只有公开条件，修改/删除/置顶没有完整失效策略；`ProfileServiceImpl.getActivity` 把题解一起缓存两小时。管理限制后需要立即停止公开展示，而不是容忍两小时旧数据。
3. 普通题解列表 SQL 相当于 `(筛选 AND 公开) OR 自己私有`，私有分支绕过题目筛选。普通列表还强制清空 userId。需要统一分组过滤。
4. 后台 `adminAdd` 接受已有 solutionId 并 `saveOrUpdate`，只有 add 权限也能更新已有题解/作者。新增必须拒绝 ID；修改不得改作者/题目归属。
5. 题解服务没有关联题目的访问控制；仅外键存在不能保证比赛题不会借公开题解泄露。第一版仅允许 PUBLIC 题目的题解对外公开/产生互动通知，CONTEST 题解只能作者或后台读取，不改变题目本身的比赛访问规则。
6. 比赛非 OJ 的 `record()` 在题单比例换算之前执行，库里可能是原始得分；除法还缺少指定比例精度。最终榜单不能直接信任这是统一满分下的成绩。
7. `contest_records` 只保存最后回调写入的分数，缺少受理序号；较早 OJ 任务晚回调会覆盖较新提交。草稿保存还会走成绩写入。必须把草稿与判题成绩区分。
8. `/internal/judgeResult` 没有包住公开 log、case、records 的整体事务/业务幂等；`complete` 无终态 CAS。同一 MQ 重试不仅重写日志，还会重复触发成绩/积分路径。
9. `RecordController` 留有 `/record/judge-save`，两个 Feign 契约仍声明但全局没有业务调用。`SubmitLogController` 还暴露 POST `/log/queue`、`/log/log-judge`、`/log/update`、`/log/change-status`，允许绕过真正结果链写状态/日志；`SubmitLogClient` 的对应修改声明没有业务调用。删除这五个公开写入口及无调用声明，保留日志读取/轮询和受控internal结果链，避免伪造或破坏最终榜单。
10. `RecordsServiceImpl.addRecord` 不论 status 都调用 `problemCompleteService.finish`；首 AC 积分调用远端服务处于本地事务内，可能远端已加分而本地回滚。方案只做必要幂等修复，不修改奖励规则。
11. 当前 MQ 开启 publisher confirm/return 和容器 retry=3，但生产者未可靠处理确认；消息自动 ID 不是业务幂等。`JudgeResultListener` 注释“未使用”但实际是启用的空消费者，不能拿它实现通知/榜单。
12. 初始化 SQL 的三条 `contest_records_*_idx` 实际误建到 `records`；已有 `(contest_id,user_id,problem_id)` 唯一键能帮助查询，但需要迁移按实际表检测，不照抄错表索引。
13. `apply.sh` 尚未纳入工作区的 V20261003 两份迁移，而 dev 脚本已纳入、服务器脚本遍历 V*.sql。统一迁移清单是上线验收的一部分。

## 2. 最终职责和依赖方向

```text
Browser -> Gateway -> content-service：solutions/comments + content_event_outbox
                  -> problem-service：problem/contest/judge/rank + problem_event_outbox
                  -> user-service：follow/inbox/fanout/points
content-service --只读 ProblemContentReadClient--> problem-service
content-service --安全批量摘要--> user-service
problem-service --安全批量摘要--> user-service
content outbox --community.events.v1--> user-service（仅本库生成通知）
problem outbox --account.points.v1--> user-service
problem outbox --judge-exchange/judge-info--> judge-server
judge-server --既有 internal 结果回调--> problem-service
```

problem 不依赖 content 的同步 API，不跨库 JOIN/读写 db_content；user notification consumer 不反查 content/problem 获取正文或标题。现有 MinIO FileOperation/CaseFileService 是共享设施直接访问对象存储，不是 problem→content Feign；本次不改变测试用例文件存储。Profile 组合在浏览器，不能由 problem 同步聚合 content。common-api 只放小型契约/技术设施，不放题解业务 Service 或 content 实体。

### 2.1 跨域题目资格和列表查询（固定设计）

1. 新增 `POST /internal/content-problems/read`，body 只有 `problemIds:string[]`（1～100，正 ID、去重）；响应 `R<List<ContentProblemReadVo>>`，每个请求 ID 都有 `{problemId,exists,deleted,auth,publicReadable,privateWritable,title}`。缺失：exists=false/deleted=true/auth=null/两个资格=false/title=null。PUBLIC=1、CONTEST=2，不能与 ContestAuth 混用。未删除 PUBLIC 可公开；CONTEST 不对外题解公开，新增其私有题解只允许当前身份已有 `problem:problem:list`，不因报名比赛额外授予题解写入。旧 CONTEST 题解作者仍可读取本人历史正文。title 仅 PUBLIC 可读或拥有上述题目管理权限时返回，否则 null；不返回正文、答案、case、参赛名单。
2. problem 用直接 Mapper 查真实状态，当前 caller 从既有 AuthUtil/SecurityContext 获取；不信 body userId/permission。新契约是独立 `ProblemContentReadClient`，不混进 judge mutation client；不要假设存在 canAccessContestProblem（当前普通 doGetProblem 排除 CONTEST）。不复用外层 @Cacheable 的 getDetailProblem。Gateway 阻断所有域 internal，服务内网部署防护/身份头信任边界维持既有机制并在 T24 检查。
3. content 新建/编辑/审核恢复/点赞/评论等需要外部资格的操作，先在行锁事务外批量检查，再在 content 事务内重核题解/评论状态与版本。已有历史非PUBLIC/失效题目的作者仍可编辑本人私有或受限内容（关联ID不变、不能转公开、仍受policy:solution:deny）；privateWritable控制新增非PUBLIC题解，不剥夺已有作者历史编辑权。远程失败为安全 HTTP503，缺失/隐藏为404；不得退化为旧缓存放行。作者历史只读、取消自己旧点赞、本人删除已有评论等不要求题目重新公开；不因此泄露他人标题或计数。每次对外返回正文/摘要前都做本次批量即时资格检查；禁止网络调用放进行锁事务。
4. 无分布式事务：题目状态与 content 写入的交叉竞态，以本次 fresh 资格响应为校验时点，随后本地题解锁为内容操作时点。题目随后关闭/删除可能保留刚产生的互动/泛化通知，但之后任何公共读取都会重新校验，不泄漏正文。不能声称跨域两个状态原子一致，也不加反向锁 RPC/XA。
5. 为保留普通/后台列表的 R/PagedResult 和过滤能力，同时避免跨库 JOIN 或每次扫全部题解，content 新建 **非权威只读投影** `solution_problem_reference`（§3.3）。只用于 SQL 分页候选/统计，不作为访问许可。新建题解前、当前页面 fresh 校验时更新它；迁移一次性初始化。固定同步器每秒最多100个已关联题目，按 checked_at/problem_id keyset 循环，目标刷新周期5分钟，积压只延迟投影、不允许放宽权限。无新 MQ 题目状态事件、无 problem→content 回调。
6. 普通候选 SQL：所有筛选在括号外；本人与公众条件括起来，公众分支 JOIN 本地 reference 的 exists_flag=1/del_flag=0/auth=1。先只取候选ID/版本/关联题；拿到一页最多50条后批量 fresh 校验，比较资格并更新投影；网络完成后再批量读本域最新题解/正文，复核del/private/moderation/owner，不直接返回远程等待前的旧正文。候选版本或资格变化则重新分页，最多3轮，每轮最多100题，仍持续变化返回安全503而非把未核验正文发出。recent20、profile最多51沿用同流程；详情也是元信息→锁外fresh→本域最新内容/可见性复核。公开 title 用本次响应，不使用投影旧 CONTEST 标题；作者无法查看题目时标题显示安全“关联题目不可用”。
7. `totalRecords` 是 **本地资格投影快照下的候选总量，最终一致**，不是跨库同一事务的精确实时总量；正文行的访问资格则每次即时验证。分页排序保持 top_up DESC/create_time DESC/solution_id DESC，count 和分页在同一 content 只读事务；网络校验在事务结束后。不把总量当权限依据。题目由 CONTEST 改 PUBLIC 后，投影同步或按题目筛选时 fresh 查询使其重新出现。此调整是移除跨库 JOIN 后必须声明的语义，不改变 PagedResult 字段。
8. reference 只存元信息，不存题目内容、用户资格、permission。后台题解 list 权限允许 inspect 内容，但并不授予题目管理/赛题正文读取；title 同样遵守远程限制。题目逻辑删除不级联删题解；历史作者/后台保留处理记录。物理清理题目不在本计划授权内。

### 2.2 Gateway / 前端兼容

canonical API：`/content-api/solution/**`、`/content-api/comment/**`。现有页面路由 `/solution/:id`、后台 solution-edit component/menu 不改，不把业务菜单父节点搬来搬去。旧权限 `problem:solution:*`、`problem:comment:*` 保留为稳定 capability 字符串，与微服务 URL 无须同名。

Gateway 新增明确 order=-10 的 `legacy-solution` route：Path=/problem-api/solution/**，uri=lb://content-service，StripPrefix=1；必须优先于默认 problem route。这是服务端代理，不是 HTTP redirect，不改 method/body/query/Authorization、HTTP 状态与 R 结构。旧前端/书签可继续访问原 CRUD/list/recent URL；新前端统一 content-api。未发布过的 comment API 不创建 problem-api alias。双路径都经过现有 JWT/Internal 防护，不新增匿名 solution 权限或权限别名。同步修改实际 gateway application.yml 以及已有 Nacos route 配置/上传入口；不能只改一个未被使用的 resources/config 文件。alias 保留本轮发布，后续独立授权且访问日志确认无旧调用才能移除。

### 2.3 Profile 聚合

problem 现有 `GET /profile/{userId}`（网关 `/problem-api/profile/{userId}`）保留练习/比赛/热力图及统计，删除 solutions/solutionsTruncated 字段和 SQL/VO 引用；独立 ProfileActivityCacheService 的纯基础 VO 缓存两小时。content 新增 `GET /profile/{userId}/solutions`，返回 `{userId,solutions,solutionsTruncated}`：显示最多50，取51判截断；仅本人可含私有/受限未删除项，访客仅有效公开；不接受 includePrivate 参数，不缓存整个响应。前端 profile API 并发 user资料、problem基础、content题解，再归一到现有 ProfileData；单独题解失败保留其他活动并明确局部错误，不能显示成“零题解”。不改纵向 tab、设置安全界面或全局路由动画。旧前端遇到取消的字段按空数组兜底（现有 normalizeActivity 已有此处理，仍补回归测试），只表现活动降级、不退回 login；协调发布新前端，不为此恢复 problem→content 聚合。

## 3. 数据模型（确定契约）

### 3.1 通用约定

新表：InnoDB、`utf8mb4 COLLATE utf8mb4_unicode_ci`；字符串事件键使用 `ascii_bin`（仅 ASCII），ID 为 BIGINT，不用 CAST 连接。新增表跨服务不建用户外键；同服务父资源使用 RESTRICT 外键，避免物理删除审计/成绩。服务仅执行逻辑删除。时间 DATETIME(3)，按项目 Asia/Shanghai 解释；事件传输 `occurredAt` 为有偏移的 ISO-8601 字符串。outbox状态固定PENDING/SENDING/SENT/FAILED，fanout固定PENDING/SENDING/DONE/FAILED。新数值计数 NOT NULL DEFAULT 0，不允许负数。新事务不在数据库行锁内执行 Feign/MQ/Redis 网络等待。

历史 _1/_2/_3 冻结；forward _4 创建 content schema，_5 在显式维护阶段转移数据，未实施的 contest migration 为 _6。执行顺序/新装和旧装选择见 §13 manifest，不能盲按文件名全部执行。用 information_schema 检测列/索引/约束、`CREATE TABLE IF NOT EXISTS`，重复执行不重置状态或重复通知；DDL 非事务回滚不能假装由 START TRANSACTION 保证。初始化 SQL 同步，但线上禁止执行含 DROP DATABASE 的初始化文件。

### 3.2 db_user

| 表 | 字段（除标注外 NOT NULL） | 主键/索引 |
| --- | --- | --- |
| `user_follow` | follower_id BIGINT、followee_id BIGINT、created_at DATETIME(3) | PK(follower_id,followee_id)；索引(followee_id,follower_id,created_at)、(follower_id,created_at,followee_id)；CHECK follower_id<>followee_id |
| `user_notification` | notification_id BIGINT ASSIGN_ID、recipient_id BIGINT、actor_id BIGINT NULL、event_id CHAR(36)、dedupe_key VARCHAR(160) ascii_bin、type VARCHAR(32)、solution_id BIGINT NULL、comment_id BIGINT NULL、action VARCHAR(32) NULL、reason VARCHAR(500) NULL、occurred_at DATETIME(3)、created_at DATETIME(3)、read_at DATETIME(3) NULL | PK(notification_id)；UNIQUE(recipient_id,dedupe_key)；索引(recipient_id,read_at,notification_id)、(recipient_id,notification_id) |
| `notification_fanout_job` | event_id CHAR(36)、actor_id BIGINT、solution_id BIGINT、occurred_at DATETIME(3)、cursor_user_id BIGINT DEFAULT 0、status VARCHAR(16) DEFAULT 'PENDING'、lease_owner VARCHAR(64) NULL、lease_until DATETIME(3) NULL、attempts INT DEFAULT 0、next_attempt_at DATETIME(3)、last_error VARCHAR(1000) NULL、created_at/updated_at DATETIME(3) | PK(event_id)；索引(status,next_attempt_at,lease_until) |
| `user_point_award_receipt` | user_id BIGINT、problem_id BIGINT、event_id CHAR(36) ascii_bin、amount DECIMAL(10,2)、created_at DATETIME(3) | PK(user_id,problem_id)；UNIQUE(event_id) |

关注取消采用物理删除关系，重复 PUT/DELETE 都幂等。不存在自关注；虚拟 root 的 ID=0 无真实用户行，不参与关注、通知和榜单用户摘要。状态异常/删除用户不能成为新关注目标；已经存在的关系读列表过滤已删除用户，用户删改不做分布式级联事务。扇出只给未删除且正常账号送达，既有通知收件人封禁期间无法登录，解封后可正常读取。

通知不保存题解正文、评论正文、源码或私有题解标题，管理 reason 仅通知收件人可读。标题在前端根据 type 渲染；目标点击走 content 题解访问检查，再按需向 problem 只读校验。消息生成在当时有效，随后题解改私有/删除不要求收回泛化通知，但不能借通知读取内容。

### 3.3 db_content：题解、正文与互动

`solution_explanation`、`solution_explanation_content` 从 db_problem 完整迁入 db_content，保留 solution_id/problem_id/user_id/title/top_up/private/create_time/update_time 和原 TEXT 正文、字节内容。移除指向 db_problem.problem 的外键；正文到本域 solution 的 FK 改为 RESTRICT；problem_id 是外部 ID，无运行态跨库外键。保留 user_create 索引。T02 已增加的字段原样迁移：`del_flag BOOLEAN DEFAULT 0`、`moderation_state VARCHAR(16) DEFAULT 'NORMAL'`、`comments_open BOOLEAN DEFAULT 1`、`like_count BIGINT DEFAULT 0`、`comment_count BIGINT DEFAULT 0`、`first_published_at DATETIME(3) NULL`、`version BIGINT DEFAULT 0`。`moderation_state` 只允许 NORMAL / AUTHOR_ONLY；已有 private 列仍代表作者选择，不拿一个字段混合作者设置和管理员强制限制。新增索引 `(del_flag,moderation_state,private,create_time,solution_id)`。

有效公开条件：`s.del_flag=0 AND s.private=0 AND s.moderation_state='NORMAL'`，并满足 §2.1 本次 problem fresh 响应 publicReadable=true。本地 reference JOIN 只筛候选，不是替代远程许可。作者普通入口可读取自己的未删除题解，包括 AUTHOR_ONLY；其他普通用户只能有效公开。后台按权限走 admin 入口。作者修改不能解除 AUTHOR_ONLY。

迁移把**迁移前已存在且有效公开**题解的 first_published_at 设为 create_time，只标记而不发送历史消息；旧私有题解保留 NULL。迁移中的标记只处理首次加列时的历史集合，重复部署不能吞掉新事件。

| 表 | 字段（除标注外 NOT NULL） | 主键/索引/外键 |
| --- | --- | --- |
| `solution_like` | solution_id、user_id BIGINT，active BOOLEAN DEFAULT 1、first_notified BOOLEAN DEFAULT 0、created_at/updated_at DATETIME(3) | PK(solution_id,user_id)，索引(user_id,solution_id)，solution FK RESTRICT |
| `solution_comment` | comment_id BIGINT ASSIGN_ID、solution_id/user_id BIGINT、root_id/parent_id/reply_to_user_id BIGINT NULL、content VARCHAR(4000)、state VARCHAR(16) DEFAULT 'VISIBLE'、deleted_by BIGINT NULL、deleted_at DATETIME(3) NULL、like_count/reply_count BIGINT DEFAULT 0、client_request_id CHAR(36) ascii_bin、created_at/updated_at DATETIME(3) | PK(comment_id)，UNIQUE(user_id,client_request_id)，索引(solution_id,root_id,created_at,comment_id)、(root_id,created_at,comment_id)、(user_id,created_at)，solution FK RESTRICT；parent/root 由 Service 保证同题解，不依赖递归外键 |
| `comment_like` | comment_id/user_id BIGINT、active BOOLEAN DEFAULT 1、first_notified BOOLEAN DEFAULT 0、created_at/updated_at DATETIME(3) | PK(comment_id,user_id)，索引(user_id,comment_id)，comment FK RESTRICT |
| `solution_moderation_action` | action_id BIGINT ASSIGN_ID、solution_id BIGINT、target_type VARCHAR(16)、target_id BIGINT、author_id/operator_id BIGINT、action VARCHAR(32)、reason VARCHAR(500)、created_at DATETIME(3) | PK(action_id)，索引(target_type,target_id,action_id)、(author_id,action_id)；不建会连带清理审计的外键 |
| `content_event_outbox` | event_id CHAR(36) ascii_bin、dedupe_key VARCHAR(160) ascii_bin、event_type VARCHAR(48)、exchange_name/routing_key VARCHAR(100)、payload JSON、status VARCHAR(16) DEFAULT 'PENDING'、attempts INT DEFAULT 0、next_attempt_at DATETIME(3)、lease_owner VARCHAR(64) NULL、lease_until DATETIME(3) NULL、last_error VARCHAR(1000) NULL、created_at DATETIME(3)、sent_at DATETIME(3) NULL | PK(event_id)，UNIQUE(dedupe_key)，索引(status,next_attempt_at,lease_until) |

点赞保留 inactive 关系而非取消时删除，用 first_notified 记录该账号对该对象是否曾触发通知。这样重新点赞仍正确恢复计数，但不会刷同一个作者的消息。first_notified 与首次通知 outbox 插入同事务。

评论 state：VISIBLE / AUTHOR_DELETED / ADMIN_DELETED；存储被删正文供后台审计，不返回公共 tombstone 正文。`root_id=NULL,parent_id=NULL` 是一级评论；回复 root_id=一级评论 ID，parent_id=实际被回复评论 ID，reply_to_user_id 从 parent 作者推导。数据库允许回复任意已存在可见回复，但展示永远两层，不做递归树查询。

新增 content 本域辅助表（不替代题解/评论契约）：

* `solution_problem_reference`：problem_id BIGINT PK；exists_flag BOOLEAN NOT NULL、del_flag BOOLEAN NOT NULL、auth INT NULL、title VARCHAR(255) NULL、checked_at DATETIME(3) NOT NULL、requested_at DATETIME(3) NOT NULL；索引(checked_at,problem_id)、(exists_flag,del_flag,auth,problem_id)。无跨库 FK。requested_at 在发请求前用可注入 Clock 生成；旧响应不得覆盖 requested_at 更新的投影，等时只接受相同内容，冲突重新 fresh 查询。title 对普通 caller 无资格时为NULL，不能缓存别人的授权摘要；后台/作者 title 展示仍以当前 fresh 响应为准。
* `content_solution_migration_state`：migration_key VARCHAR(64) ascii_bin PK（固定 solution-domain-v1）、source_mode VARCHAR(16) NOT NULL（LEGACY/FRESH）、status VARCHAR(16) NOT NULL（PREPARED/COPIED/VERIFIED/CUTOVER）、manifest_sha256 CHAR(64) ascii_bin NOT NULL、source_digest/target_digest CHAR(64) ascii_bin NULL、counts_json JSON NULL、started_at DATETIME(3) NOT NULL、verified_at/cutover_at DATETIME(3) NULL。只用于显式迁移状态和防重复旧库导入，不是业务 session；CHECK 两组状态值。

`content_event_outbox` 结构完全沿用 T02 ProblemEventOutbox 的字段、unique/lease/索引，仅允许七个社区 event_type。**db_problem.problem_event_outbox 留在原域，仅允许 POINTS_AWARDED/JUDGE_DISPATCH**，不把它整体搬走；未知类型不得发送。每域 worker 只认自己固定白名单，不跨库 claim。content 使用明确 ContentEventOutbox 实体/Mapper/Service，不是继承 problem-service 实体或注入 problem mapper。

### 3.4 db_problem：比赛成绩

复用 contest、user_contest、user_submit、submit_log、judge_case_log、contest_records；只补它们缺少的稳定来源、冻结和最终快照。

* contest 增加 `scoring_version INT DEFAULT 2`、`next_attempt_seq BIGINT DEFAULT 0`、`problem_snapshot_at DATETIME(3) NULL`。迁移时已有提交/成绩的 CONTEST 置 scoring_version=1（历史规则），无数据活动为 2；HOMEWORK 不开放最终排名，但评分写入修复仍适用。
* submit_log 增加 `score DECIMAL(10,2) NULL`、`result_applied BOOLEAN DEFAULT 0`、`completed_at DATETIME(3) NULL`；新增 `(contest_id,status,submit_time,submit_id)`。score 是实际回调最终题单得分，不暴露内部异常。旧终态数据必须维护窗口确认无在途后标记 applied，不伪造历史回调时间。
* contest_records 增加 `applied_attempt_seq BIGINT DEFAULT 0`、`applied_attempt_id BIGINT NULL`，用于旧统计接口的最新有效评分投影。每人每题原唯一键保留。
* judge_case_log 增加 UNIQUE(submit_id,case_index)，迁移预检查重复并按最新 update_time、最大 id 保留一条、备份被清理行；不可无检查加唯一键导致部署失败。

| 表 | 字段（除标注外 NOT NULL） | 主键/索引 |
| --- | --- | --- |
| `contest_problem_snapshot` | contest_id/problem_id BIGINT、problem_order INT、title VARCHAR(255)、problem_type INT、max_score DECIMAL(10,3)、created_at DATETIME(3) | PK(contest_id,problem_id)，UNIQUE(contest_id,problem_order)，contest FK RESTRICT；不依赖当前题单成员查询 |
| `contest_attempt` | attempt_id BIGINT ASSIGN_ID、contest_id/user_id/problem_id BIGINT、attempt_seq BIGINT、submit_id BIGINT NULL、kind VARCHAR(16)、accepted_at DATETIME(3)、state VARCHAR(16) DEFAULT 'PENDING'、judge_status VARCHAR(32) NULL、score DECIMAL(10,2) NULL、correct BOOLEAN NULL、completed_at DATETIME(3) NULL | PK(attempt_id)，UNIQUE(contest_id,attempt_seq)，UNIQUE(submit_id)，索引(contest_id,state,attempt_seq)、(contest_id,user_id,problem_id,attempt_seq) |
| `contest_rank_snapshot` | contest_id BIGINT、state VARCHAR(16) DEFAULT 'WAITING'、version BIGINT DEFAULT 0、next_version BIGINT DEFAULT 0、build_attempts INT DEFAULT 0、next_build_at DATETIME(3)、source_seq BIGINT DEFAULT 0、source_mode VARCHAR(24)、rule_version VARCHAR(24)、total_users BIGINT DEFAULT 0、pending_count BIGINT DEFAULT 0、lease_owner VARCHAR(64) NULL、lease_until DATETIME(3) NULL、generated_at DATETIME(3) NULL、last_error VARCHAR(1000) NULL、updated_at DATETIME(3) | PK(contest_id)，索引(state,next_build_at,lease_until)，contest FK RESTRICT |
| `contest_rank_entry` | contest_id/version/user_id BIGINT、rank_no/row_position BIGINT、score DECIMAL(18,2)、correct_count/answered_count INT、handed_in BOOLEAN、created_at DATETIME(3) | PK(contest_id,version,user_id)，UNIQUE(contest_id,version,row_position)，索引(contest_id,version,rank_no,row_position) |

不把源码/答案再复制到 contest_attempt/榜单；OJ 的 submit_id 指向提交原记录，非 OJ 答案继续在 contest_answer_records；草稿只更新答案，不更新成绩/受理序号。

## 4. 关注系统

PUT 关注、DELETE 取消，以当前登录用户为 follower。验证 target>0、非本人、目标真实存在且可用；重复操作成功返回当前关系，不抛主键冲突。不通知“有人关注你”（需求未要求，减少噪声），不建关注计数缓存；主页统计走索引 COUNT，列表按 created_at DESC、另一方 ID DESC 分页。

需要关注列表/粉丝列表，均返回安全摘要；可供任意登录用户查看，但不暴露邮箱/角色权限。删除账号保留逻辑引用，过滤掉被删除用户。本人/无效目标按钮不显示。

扇出查询 `(followee_id, follower_id)` keyset 每批 200；条件 created_at<=事件时间且投递时关系仍存在。晚关注不补发历史题解、扇出前取消关注不继续送达；重新关注的 created_at 是新时间。不是永久冻结“发布瞬间粉丝名单”，此语义明确接受取消关注对未送达通知的影响。

## 5. 通知事件、事务和送达

### 5.1 通知类型与收件人

| type / routing key | 触发与目标 | 去重键 |
| --- | --- | --- |
| SOLUTION_PUBLISHED / `solution.published.v1` | 题解首次成为有效公开；关注者，不含作者 | `solution-published:{solutionId}` |
| SOLUTION_LIKED / `solution.liked.v1` | 某账号首次点赞；题解作者，排除自点赞通知 | `solution-liked:{solutionId}:{actorId}` |
| SOLUTION_COMMENTED / `solution.commented.v1` | 新一级评论；题解作者，排除自己 | `solution-commented:{commentId}` |
| COMMENT_REPLIED / `comment.replied.v1` | 新回复；parent 作者与题解作者取集合，剔除操作者、去重 | `comment-replied:{commentId}` |
| COMMENT_LIKED / `comment.liked.v1` | 某账号首次点赞评论；评论作者，排除自己 | `comment-liked:{commentId}:{actorId}` |
| SOLUTION_MODERATED / `solution.moderated.v1` | 管理限制/解除/删除；题解作者，附 action 与必要 reason | `solution-moderated:{actionId}` |
| COMMENT_MODERATED / `comment.moderated.v1` | 管理删除评论；评论作者，附删除 reason | `comment-moderated:{actionId}` |

同一回复事件只用 COMMENT_REPLIED，不能再给题解作者发一次 SOLUTION_COMMENTED。自己点赞允许计数变化，不给自己发点赞通知。管理行为即使管理员也是作者仍留审计，并向作者投递处理结果。

第一版不做时间窗口聚合或 Redis 通知计数。这套通知是个人持久化消息，不是瞬时 toast；点赞按账号/对象首次通知已经阻止反复切换刷屏。首页/消息列表可分页，粉丝扇出批处理；后续再加聚合字段，当前不引入会覆盖已读语义的聚合更新。

### 5.2 事件格式

```json
{
  "schemaVersion": 1,
  "eventId": "UUID",
  "eventType": "COMMENT_REPLIED",
  "dedupeKey": "comment-replied:2090000000000000001",
  "occurredAt": "2026-10-03T12:00:00.123+08:00",
  "actorId": "100",
  "solutionId": "2090000000000000000",
  "commentId": "2090000000000000001",
  "recipientIds": ["101", "102"],
  "action": null,
  "reason": null
}
```

SOLUTION_PUBLISHED 的 recipientIds 为空，由用户服务本地 follow 表计算；其他最多两个、服务端计算接收者。用户请求不允许提交 recipients/type/reason 作为普通评论字段。事件 JSON <=16KiB，不含文本内容快照、Token、密码、邮箱、源码。用确定类/枚举反序列化，不信任任意 Rabbit type header 实例化 Java 类型。

### 5.3 已有 MQ 与两域各自同库 outbox

关注者位于用户服务，题解位于内容服务，同步 Feign 广播会扩大一次发布的失败面，也不能与本地数据库提交原子关联。已有 RabbitMQ 能承担跨服务投递，但“DB commit 后直接 send”仍有进程崩溃丢消息窗口。复用现有 broker、vhost 和 JSON converter，以 content_event_outbox 解决社区窗口，problem_event_outbox 仅解决积分和判题派发窗口，不做跨库 XA。

1. 同一数据库事务写入业务变化、计数、moderation audit、outbox。回滚时都不生效。
2. content/problem 两域各自定时器每秒认领最多 20 条到期事件，使用 `FOR UPDATE SKIP LOCKED` + SENDING/owner/60 秒租约；claim 事务提交后才发 MQ。固定发送线程池4、有限队列20；本实例上批未完成不继续claim，确认等待5秒，最长一批约25秒，避免顺序发送20条超过租约。所有状态更新校验owner，不新建独立部署进程。
3. 独立 `communityRabbitTemplate` 与相关 CorrelationData，持久消息、mandatory；等待确认上限 5 秒，收到 nack、returned message 或超时均不标 SENT。成功条件 ack && 无 return，使用 owner 条件更新状态。
4. 重试间隔 5/10/20/... 秒，最大 300 秒；第 20 次后 FAILED，保留 last_error、报警和受保护 replay 接口。SENDING 租约过期可被重新认领。重复投递是预期情况。
5. user-service 专用 listener factory，AUTO ACK 在**事务提交后**生效，失败抛出不能吞掉；重试 3 次后进新增 community DLQ。未知 schema/type 进入 DLQ，不能默默确认。不修改已有判题 listener factory 的重试/并发。
6. 点对点事件事务中按 UNIQUE(recipient,dedupe_key) 插入通知；重复忽略但不得更新 read_at 或 created_at。发布事件先 INSERT fanout_job；持久化 job 完成即可 ACK，不在 listener 一次循环全部粉丝。
7. fanout worker 每秒 claim 一个 job；每批 200 接收者与 cursor 更新同一事务；崩溃重复批次由通知唯一键兜底。全部处理结束 DONE。每次claim只处理一批200，成功后有剩余则置PENDING、清lease、next_attempt_at=now并归零连续失败attempts；结束才DONE。失败使用同样退避/租约，受保护replay保留游标，不能生成新eventId，避免长粉丝列表一直占着过期租约。

拓扑：durable topic `community.events.v1`、durable queue `user-community-notification.v1` 绑定 `solution.*.v1`/`comment.*.v1`；DLX `community.events.dlx.v1`、DLQ `user-community-notification.dlq.v1`。积分使用独立direct `account.points.v1`、routing key `points.awarded.v1`、queue `user-point-award.v1`、direct DLX `account.points.dlx.v1`/DLQ `user-point-award.dlq.v1`。社区DLX也用direct，dead routing key分别固定为 `user-community-notification.dead.v1`、`user-point-award.dead.v1`并绑定对应DLQ。在现有vhost（当前配置 `/judge`）部署，不动旧 exchange/queue 的属性以免 PRECONDITION_FAILED。

判题队列当前没有DLX、listener重试耗尽存在丢任务风险。声明新的direct `judge-exchange.dlx.v1`、durable `judge-info.dlq.v1`，以 `judge-info.dead.v1`绑定。既有 `judge-info-queue` **不修改声明arguments**，由发布手册在实际vhost设置仅匹配 `^judge-info-queue$` 的broker policy：dead-letter-exchange=`judge-exchange.dlx.v1`、dead-letter-routing-key=`judge-info.dead.v1`。保留原并发/重试配置，验证失败消息进DLQ；管理员修复后原ID重放，不重新生成提交。

重放接口：content-service `POST /solution/admin/events/{eventId}/replay` 需 `problem:solution:edit`，仅允许七种社区事件；user-service `POST /notification/admin/fanout/{eventId}/replay` 需 `user:user:edit`。只允许FAILED，置PENDING、attempts=0、next_attempt_at=now、清lease，保留eventId/dedupe/cursor，记录操作者日志。JUDGE_DISPATCH 另用 `POST /log/admin/{submitId}/dispatch-retry`，需要新管理权限 `problem:judge:dispatch:retry`，仅允许原事件FAILED且原提交未应用/未终态；不生成新submitId、不重新判已完成提交。业务人员不直接发送任意消息。DLQ运维重放原消息同样保留ID。POINTS_AWARDED的FAILED仅由发布手册中的受控运维SQL按明确eventId与event_type重置（同样归零attempts/清lease），不向浏览器开放任意事件重放。

已 SENT 社区/积分 outbox 30 天、已 SENT JUDGE_DISPATCH 24小时后分批清理，每小时最多 500（后者含代码，不重复存一个月）；FAILED 永不自动删除。个人通知保留 180 天，每天按批 500 清理；DONE 扇出 job 保留 30 天。通知消费者/扇出worker不再投递occurredAt早于180天的社区事件，记录过期丢弃指标，防止清理后的古老重放重新制造未读；该规则不适用于永久保留收据的POINTS_AWARDED。first_published_at/first_notified永久保留。审计/比赛快照不自动删除。

### 5.4 首次公开与历史事件

first_published_at 是“一生一次发布提醒”，不是每次更新。新公开题解插入时设置；私有→公开、管理员解除限制且作者 private=false 时，由持锁检查从 NULL 设置并写 outbox；普通编辑、重新私有后再公开不重新广播。有效公开必须关联 PUBLIC 题目，修改关联题目本需求一律禁止。

状态变化和 outbox 同事务。通知延迟期间若又隐藏题解，泛化消息可能送达，但无内容泄露；点击目标返回通用不可访问。若需要“已经送达的通知也一律消失”属于后续产品要求，不混入本次版本。

## 6. 点赞、评论与管理语义

### 6.1 点赞并发与数量

题解/评论点赞使用 PUT 表示 active=true，DELETE 表示 active=false，不做 toggle API。锁顺序：solution 行 → root（如有）→ comment（如有）→ like 行。不存在关系 INSERT；已存在相同状态直接返回，只有状态真的改变才 `count=count±1`。关系、计数、first_notified、outbox 同事务；数据库唯一键兜底。取消后重赞不重复通知，取消已取消也不减计数。

计数保存在资源表中，详情取 like_count，分页用一次批量查询当前 viewer 的点赞关系；不把每个用户的 likedByMe 缓入共享详情。第一版不加 Redis 点赞缓存/异步累加，省掉 DB/Redis 双写与重建复杂度。后台可用受保护维护命令对 inactive/active 关系 COUNT 对账，不提供普通用户改计数接口。

新增点赞要求资源有效公开（评论还必须 VISIBLE）；已存在自己的点赞允许取消，即使资源后来私有/删除，返回只包含 liked=false，不返回不可见内容。关闭评论区不禁止既有评论点赞/取消。`policy:comment:deny` 禁止新增评论/回复，不禁止阅读、删除本人评论和取消点赞；点赞不是“评论写入”限制的隐式扩张。

### 6.2 评论查询与回复

* 一级列表：每页默认20、最大50，按 created_at DESC、comment_id DESC；根评论含 replyCount，无嵌套无限回复数组。
* 回复列表：rootId 独立分页，按 created_at ASC、comment_id ASC；只呈现两层，“回复 @昵称”通过 reply_to_user_id 摘要显示，可回复可见子回复而不加第三层。
* 回复必须 parent 存在且 VISIBLE、同 solution；root 必须真实一级且属于同 solution。root 已删除但其可见子回复仍可被回复，root tombstone 保留；不能直接回复已删除 root/parent。
* 一级删除不级联删除子回复；公共响应 tombstone 的 content=null、reason=null，只表示作者删除/管理员删除；其已存在回复仍展示。
* 删除不允许恢复原正文/再次编辑；第一版评论不提供编辑接口，避免变更被回复语境及无意义历史版本。
* clientRequestId 为 UUID，重试必须复用同一值；同用户同 UUID 相同内容/目标返回原 commentId，不重复计数/发通知；不同内容/目标返回409。先查幂等记录后检查当前 comments_open，使“已经成功但响应丢失”的重试在关评后仍可返回已存在结果。

### 6.3 内容与表情

评论最多 2,000 Unicode code points，同时 UTF-8 最大8KiB；去除两端空白、CRLF→LF、NFC 规范化后校验，纯空白拒绝。拒绝 NUL、无效代理对、除 TAB/LF 外的控制字符。数据库 VARCHAR(4000) 容纳原长度限制，不以数据库字段长度替代业务校验。

基础表情第一版为固定 Unicode：🙂、😄、😂、😅、😍、🤔、👍、👎、🎉、❤️、😢、😡。EmojiPicker 插入字符，数据库直接保存 Unicode；不支持自定义图片、`:xxx:` 解析、HTML entity 解码或 Markdown。

“不允许 Markdown/嵌入”指不进行任何解析渲染，不是用脆弱正则禁止用户输入 `[]` 或 `<script>` 这些文本。渲染只能 Vue 插值/textContent + `white-space:pre-wrap; overflow-wrap:anywhere`，URL只是文字、不自动链接，不用 v-html、MarkdownPreview、富文本编辑器；这些输入须显示为文字，不能执行。管理 reason 同样是纯文本。

限制刷评论：校验身份/可见性后，`content:comment:write:{userId}` Redis Lua INCR/EXPIRE，一分钟最多10次首次创建尝试。已成功的幂等重试不再计数，删除不受该限制。Redis 限制器不可用时返回503安全业务消息，禁止用 Redis 失效绕过限频；点赞计数/关注关系仍由 MySQL 保证，不依赖此键。

### 6.4 关评与管理

关评是 resource comments_open=false：禁止新增一级评论与回复，已有内容照常按原可见性展示，点赞/删除本人仍允许。作者和 `problem:solution:edit` 可关/开，锁同一 solution 行，保证“关评先提交，则随后新评论事务失败”；评论先持锁并提交的内容保留。

管理员设 AUTHOR_ONLY 必填 reason 1～500字符；RESTORE 只重置 moderation_state，不改变作者 private。作者可修改自己的受限内容，但无法自行重新公开。管理删除评论必填 reason，reason/audit 与 tombstone/outbox 同事务。重复删除不再产生 audit/通知；重复设置当前管理状态（即使理由不同）返回当前状态，不另造处理记录；本版本不提供修改旧理由接口。

作者读取自己处理记录的独立接口只返回自己的 reason/action/time；普通读者及评论被回复者不见处理原因。后台需要权限才看正文/完整审计；author-only 不是后台管理员也不可见。

题解删除改为逻辑删除，评论/点赞关系及审计保留但普通请求不见。管理员删除题解使用新的 POST 单项命令并必填原因；旧无原因批量 DELETE admin 返回400“请使用带处理原因的删除接口”，前后端同版本发布。普通 DELETE 路径保留，批次最多50，逐项所有权全量校验后原子删除，不把部分失败包装成全部成功。

## 7. 权限模型

| 操作 | 普通登录用户 | 作者/评论作者 | 管理权限 |
| --- | --- | --- | --- |
| 关注/取消、列表/关系 | 自己发起；列表安全摘要 | 无额外特权 | 无需管理权限 |
| 收件箱/未读/标读 | 只能 recipient=自己 | 相同 | 默认管理员也不能浏览别人私人通知 |
| 有效公开题解/评论阅读 | 允许 | 私有/受限仅题解作者读 | 专用 admin API；题解 `problem:solution:list`，评论 `problem:comment:list` |
| 点赞/取消 | 有效公开可新增；已有自己的关系可取消 | 自赞允许、无自身通知 | 不得替别的用户点赞 |
| 创建评论/回复 | 登录且无 `policy:comment:deny` | 同样受禁止策略 | 管理员发评论也是普通行为，同样受禁止策略 |
| 删除评论 | 只可删自己的 | 可以删除自己评论；题解作者不能凭作者身份删他人评论 | `problem:comment:remove`；必须填写原因 |
| 关评 | 不允许 | 题解作者可设置，受 `policy:solution:deny` 限制 | `problem:solution:edit`，沿用禁止策略优先原则 |
| 题解审核/审计 | 不允许 | 作者只能读本人结果 | `problem:solution:edit/list/remove` 按操作区分，保留既有禁止策略 |
| 最终榜单 | 比赛结束后：公开赛对登录用户；密码赛/白名单需已参加 | 当前参与人可见，与是否交卷无关 | `problem:contest:rank` 可通过 admin API；重建也用此既有权限 |

新建 B 权限 `problem:comment:list`、`problem:comment:remove` 放在已有题解管理 I 菜单下（按 router='solution-edit' 定位，不硬编码线上 menu_id）；`problem:judge:dispatch:retry` 放在已有 `judge-submit-log` 管理路由下。以 `role_name in ('admin','super_admin')` 赋权限并确保相应父菜单、路由、system:backend:access；不默认赋给 teacher/student、不赋任何 deny。后台评论管理是题解管理的抽屉，不新建没有路由的菜单操作。

比赛已有 `problem:contest:rank`，不可再造新字符串。教师保留其既有授权；admin/super_admin 补齐此权限所需的真实教师模块/比赛路由祖先，只添加所需关联、不扩大到所有教师按钮。迁移后通过现有角色刷新入口更新 Redis 登录权限快照，并验收管理员/超管/教师各自的实际菜单。没有依赖祖先菜单的普通社交 API不生成按钮权限。

不直接暴露服务端端口；internal 安全仍依赖 Gateway 阻断和部署网络。最终上线须确认 user/problem/judge 只在内网可达，不能把仅 user-id header 可信的端口开放公网。

## 8. API 契约

浏览器经 `/api` 反向代理，以下表列 Gateway 路径（前端 http 工具已处理 `/api`）；服务 Controller 不重复加 `-api` 前缀。全部普通新接口要求登录，不修改公告的匿名接口。

### 8.1 用户、关注、通知

| 方法 / Gateway 路径 | 请求 | data |
| --- | --- | --- |
| PUT `/user-api/follow/{userId}` | 无 body | `{following:true,followersCount,followingCount}` |
| DELETE `/user-api/follow/{userId}` | 无 body | 同上 following=false |
| GET `/user-api/follow/{userId}/summary` | 无 | `{userId,following,followersCount,followingCount}`；following 指当前 viewer→目标 |
| GET `/user-api/follow/{userId}/following`、`/followers` | currentPage/pageSize | PagedResult<UserSummaryVo> |
| GET `/user-api/notification/page` | currentPage/pageSize、unreadOnly:boolean 默认false | PagedResult<NotificationVo> |
| GET `/user-api/notification/unread-count` | 无 | `{count}`，indexed COUNT |
| PUT `/user-api/notification/{notificationId}/read` | 无 | `{read:true}`，不存在或非本人404 |
| PUT `/user-api/notification/read-all` | `{throughId:string}`，列表响应中见到的最大 ID | `{updatedCount}`；只改本人 ID<=throughId 的 unread，不能误标并发新消息 |
| POST `/user-api/notification/admin/fanout/{eventId}/replay` | 无 | `{accepted:true}`；仅 FAILED、user:user:edit |
| POST `/internal/user-summaries`（Feign，不给 Browser） | `{userIds:string[]}`，去重、最大100 | `List<UserSummaryVo>` |

UserSummaryVo 仅 `{userId,userName,nikeName,specialRoles}`；头像继续 Avatar(userId) 访问现有缓存化头像地址。无邮箱、状态、备注、权限。被删除用户不返回，前端缺失摘要显示“已注销用户”而非当前用户头像；后台不存在批量 public 权限的隐式要求。

NotificationVo：`notificationId,type,actor:UserSummaryVo|null,solutionId|null,commentId|null,action|null,reason|null,occurredAt,readAt|null`。reason 仅当前 recipient，action 对管理类型从事件保存；不返回 event payload/内部错误。未知目标404以当前面板提示，不让个人消息跳全局异常页。

### 8.2 题解/评论

| 方法 / Gateway 路径 | 请求 | data |
| --- | --- | --- |
| GET 既有 `/content-api/solution/{id}` | 不变 | 原 DetailSolutionVo 加 `likeCount,likedByMe,commentCount,commentsOpen,moderationState,effectiveVisibility`；不可见404 |
| PUT / DELETE `/content-api/solution/{id}/like` | 无 | `{liked,likeCount}`；不可见资源取消只返回 `{liked:false,likeCount:null}` |
| PUT `/content-api/solution/{id}/comments-state` | `{open:boolean}` | `{commentsOpen}`，作者/管理分开验证 |
| GET `/content-api/solution/{id}/comments` | currentPage/pageSize | PagedResult<CommentVo> 的一级评论 |
| POST `/content-api/solution/{id}/comments` | `{content,clientRequestId}` | CommentVo |
| GET `/content-api/comment/{rootId}/replies` | currentPage/pageSize | PagedResult<CommentVo> |
| POST `/content-api/comment/{parentId}/replies` | `{content,clientRequestId}` | CommentVo |
| PUT / DELETE `/content-api/comment/{id}/like` | 无 | `{liked,likeCount}`，取消规则同题解 |
| DELETE `/content-api/comment/{id}` | 无 | `{deleted:true}`，只可本人，重复删除幂等 |
| GET `/content-api/comment/{id}/moderation` | 无 | 本人评论的处理记录，不含其他目标 |
| GET `/content-api/solution/{id}/moderation` | 无 | 本人题解的处理记录 |
| POST `/content-api/solution/admin/{id}/moderation` | `{action:'AUTHOR_ONLY'|'RESTORE',reason}` | `{moderationState}` |
| POST `/content-api/solution/admin/{id}/delete` | `{reason}` | `{deleted:true}` |
| GET `/content-api/comment/admin/page` | solutionId、state可选、currentPage/pageSize | PagedResult<AdminCommentVo> |
| POST `/content-api/comment/admin/{id}/delete` | `{reason}` | `{deleted:true}` |
| GET `/content-api/solution/admin/{id}/moderation-history` | currentPage/pageSize | PagedResult<ModerationActionVo>，含对应题解/评论处理 |
| POST `/content-api/solution/admin/events/{eventId}/replay` | 无 | `{accepted:true}` |

CommentVo：`commentId,solutionId,rootId|null,parentId|null,author:UserSummaryVo|null,replyTo:UserSummaryVo|null,content|null,state,likeCount,likedByMe,replyCount,createdAt,canDelete`。canDelete 为服务端便捷提示，不代替所有权校验。普通 VO 无删除原因/删除者 ID/未清洗管理字段。AdminCommentVo 可读保存正文与审计，不借普通接口开管理员通道。

现有 add/edit 返回 boolean 保持兼容；detail/list VO 新字段默认可选到同版本部署完成。普通 DTO 禁止客户端赋值 moderationState/delFlag/counts/author，后台编辑 private 不再表达“强制审核”，必须走 moderation 命令并填原因。新增/编辑（含后台）关联非PUBLIC题目而请求private=false时返回400，提示该题目题解只能本人可见；不偷偷改客户端字段。历史CONTEST题解仍按作者/管理边界处理。detail/list的effectiveVisibility统一为PUBLIC/AUTHOR_ONLY/DELETED，由当前关联题/审核/private/删除状态计算，UI以它而非private单字段标识公开性，避免历史数据误标公开。

### 8.3 比赛最终榜单

| 方法 / Gateway 路径 | 请求 | data |
| --- | --- | --- |
| GET `/problem-api/contest/{id}/final-rank` | currentPage/pageSize | FinalRankVo；未到结束409，HOMEWORK 400，无权404/403且不泄露名单 |
| GET `/problem-api/contest/admin/{id}/final-rank` | 同上 | 同格式 + adminBuildInfo；problem:contest:rank |
| POST `/problem-api/contest/admin/{id}/final-rank/rebuild` | 无 | `{accepted:true}`；结束后且无未应用提交，已有 READY 可保留旧版直到新版提交 |
| POST `/problem-api/log/admin/{submitId}/dispatch-retry` | 无 | `{accepted:true}`；原派发事件FAILED且提交尚未完成，problem:judge:dispatch:retry |
| GET `/internal/judge-submission/{submitId}`（Feign，不给Browser） | 无 | `{resultApplied:boolean,status:JudgeResult}`；仅用于JudgeListener跳过已应用的重复任务 |

FinalRankVo：`contestId,state:'WAITING'|'BUILDING'|'READY'|'ERROR',version,ruleVersion,sourceMode:'CURRENT'|'LEGACY',generatedAt,pendingCount,ranking:PagedResult<RankEntryVo>|null`。WAITING/ERROR 返回安全展示消息，不返回 last_error。RankEntryVo：`rank,rowPosition,user:UserSummaryVo|null,userId,score,correctCount,answeredCount,handedIn`；不暴露代码、答题内容、测试用例和邮箱。历史 mode 的 UI 用“历史成绩”标识，不能伪装成经过新规则重放的成绩。

## 9. 最终比赛榜单规则与实现

### 9.1 从现有代码确认的规则，以及本次明确的缺省决策

* 现有模式是**题单加权分数制**，支持 OJ、填空、选择、多选；不是 ACM 罚时制，不把尝试次数、耗时、提交先后加入主排名。
* 每道题使用最新受理且有效完成的提交，而不是取最大分/第一次 AC。该行为继承每人每题一行覆写的意图，修复异步回调的完成顺序错误。较新的 WA/CE 可以取代较旧 AC 成绩；JUDGE_ERROR 不覆盖之前的有效成绩。
* OJ：通过用例权重 / 用例总权重，比例 scale=8 DOWN，再乘该题冻结 max_score，最终题目分数 scale=2 HALF_DOWN（沿用 RecordsService 最终取整）。用例总权重<=0返回0，不 NPE/除零；有基础设施错误不作为有效题成绩。
* 填空/选择：沿用现有答案正确性/部分得分规则，**落库前**执行同样比例换算，最终scale=2 HALF_DOWN；原始满分<=0得0。correct 独立于 score，不能通过 score>0当作AC。
* 总分 SUM 每题最终两位分数，所有参加用户都列入（包括无提交0分、未交卷）。user_submit 只表示交卷标志，不是参赛资格，也不决定是否计分。
* 现有代码无同分排名规则；本次确定同分同名次，`RANK() OVER(ORDER BY score DESC)`：100、100、90 → 1、1、3。展示顺序同分 `user_id ASC`，仅保证稳定，不是竞技惩罚。correctCount是辅助指标，不作为分数之外的名次条件。
* 仅 CONTEST 提供最终榜单；HOMEWORK 保留原有教师成绩统计、补交机制，不和不同 deadline 的作业混排。

### 9.2 提交受理、题单冻结和交卷边界

新比赛 `scoring_version=2`。题目表/题单继续是内容来源，新增 snapshot 只冻结成员、顺序、名称/题型与题单分值，不复制 Markdown/用例文件。

1. 第一次开始后打开题目/正式受理时，在事务中锁 contest，然后锁对应 problem_list，再按problem_id升序锁题目行，复制当前题目集合到 contest_problem_snapshot 并设置时间。同一次 snapshot 读写不被题单增删/改分/排序或评分内容修改打断。所有题单写入口统一锁 problem_list 行，仅原题单变化，不修改已冻结比赛。
2. snapshot 后如还没有正式 attempt，管理员改 listId 可在 contest 锁内清除并重建 snapshot；已有 attempt 后禁止改 listId/startTime/endTime。其他描述/标题改动不改变成绩数据。已 READY 的比赛不可改赛程/题单。
3. 比赛题目导航、得分 max_score、题目成员校验、统计查询都从 snapshot；新排序/题单更新只影响未冻结活动。禁止修改进行中比赛或已结束但仍有PENDING受理任务引用题目的题型、答案、case/判分权重；显示标题/说明更新可保留。修改入口先查询snapshot引用及未冻结活动的当前题单引用，按contest_id升序锁相关contest，再锁problem和case；锁problem后重新查引用/时间/pending。发现新增未锁的有效contest引用，返回409重试，不倒序从problem追加contest锁。修改文件先上传同bucket下新路径 `{problemId}/{caseId}-{UUID}.in/.out`；事务通过检查后只切换oj_problem_case.input/output与score引用，不能覆盖原对象。实际BuildJudgeCaseImpl按表中路径读取，既有挂载不变。失败清理新对象；旧对象不在请求中立即删除，以免仍被练习判题引用，发布手册列出仅无在途判题且与DB引用对账后的人工回收。锁持有期间不进行网络IO。已结束不自动对旧提交重判。
4. 每次新提交获取 contest 行锁，用服务端 Clock 判断 `start<=now<end`、已参加、未交卷、problem在snapshot。不得用客户端时间、异步完成时间当作是否在比赛内。然后增加 next_attempt_seq，写 contest_attempt；OJ 同事务建立 submit_log。
5. OJ dispatch 用已新增 outbox 的 JUDGE_DISPATCH 分支，把原 JudgeInfo 发给已有 judge-exchange/judge-info，仍由 JudgeListener 接收。提交事务不跨 Feign/MQ，避免“已存 QUEUE、进程崩溃还未发任务”永久卡住最终榜单。该分支保持原 JudgeInfo 消息形状，不把 community envelope 发进判题队列；payload 上限600KiB，固定 exchange/route，不接受浏览器配置。
6. 交卷在同一个 contest 锁内检查并 INSERT user_submit（已存在返回成功）；交卷先提交则之后 attempt 不被受理，attempt 先提交则允许其异步结果在交卷/比赛结束后应用。后台退回交卷仅结束前可行；不改变已提交 accepted_at。
7. HOMEWORK同样统一受理，但结束界限采用该用户有效supplement.deadline（存在且>=end）否则end，要求start<=now<deadline、joined且未交卷；JudgeService不得再次用旧isContestEnable的end检查否决有效补交。最终榜单仍仅CONTEST。
8. 草稿保存 `/record` 只存 contest_answer_records；必要时初始化 score=0/status=false 的记录，不能清空/覆盖已有成绩，也不分配 attempt_seq、不加积分、不产生正确状态。非 OJ `/record` 的旧“保存即判”保持调用行为，但必须通过统一受理/评分服务，不额外算第二次。
9. 所有加入/后台加用户/移除用户入口也锁 contest；结束后 roster不可改，提交过的用户不能移除并删成绩。结束前无attempt用户可移除。PUBLIC 终榜可被登录用户读取，PRIVATE/WhiteList要成员或管理权限。

### 9.3 判题结果原子应用

新增 `JudgeResultApplicationService` 作为唯一终态落库入口。锁顺序 contest（比赛）→submit_log→attempt→成绩行；非比赛只锁 log。校验 submitId 与已存 userId/problemId/contestId，使用存储身份字段，不照搬外部 JudgeScore 任意 ID。

在一个 db_problem 事务内：首次 terminal result 更新 submit_log(score/status/result_applied/completed_at)、写唯一 case日志、结束 attempt、按较大 attempt_seq 更新 contest_records成绩、首次 AC 完成关系/积分事件。OJ正式受理事务保存当次源码到现有答案表，回调不得用旧源码覆盖后来保存的草稿。重复 callback 在 result_applied=true 直接成功，不能删除重建 case 或重加积分。中间状态更新不能把已终态退回 compiling/running。不把 complete 返回false仍当作继续记成绩。

所有真正终态路径（包括编译错误、JUDGE_ERROR）都通过同一入口，不为“终态log、未应用score”留中间提交。outbox派发FAILED仅记录管理侧诊断，原提交仍QUEUE/PENDING供原ID重放，不能把尚可能重投的消息同时宣布终态。case失败细节保留原管理员隔离；用户CE仍可看编译错误，JUDGE_ERROR只见基础状态。Redis submission lock afterCommit按原 token释放，错误由12秒TTL兜底，不修改现有并发/锁TTL业务要求。

首次 AC：`problem_complete` 唯一关系 INSERT IGNORE只有真正插入成功时产生 POINTS_AWARDED（不发用户通知）。既有 JudgeConfig 的固定2分/非固定规则保持。`account.points.v1`→`user-point-award.v1`，用户服务同事务写 user_point_award_receipt、原 sys_user.points+=amount，重复忽略。不要在 db_problem 行锁事务内继续调用旧 addPoint Feign。历史已完成关系不回补奖励，旧不准确的完成标记不由迁移批量删除。

### 9.4 最终生成、缓存与晚结果

周期worker每15秒扫描最多20个已结束、非删除CONTEST；查询接口也可登记/触发待结算，但不会在HTTP请求中同步聚合全体提交。固定生成线程池2、队列容量2，只为可立即执行的空闲槽位认领DB租约，不把20个候选同时claim后排队消耗租约；固定共享心跳调度器1，不为每场比赛新开线程。榜单 worker 存在于 problem-service，不添加独立部署进程。

rank header创建时next_build_at=now，source_mode/rule_version按scoring_version赋CURRENT/WEIGHTED_LATEST_V1或LEGACY/LEGACY_RECORD_V1，不能让NOT NULL字段缺值。READY不会被定时器重复认领；管理rebuild将next_build_at=now登记待生成。

状态：WAITING（结束且还有PENDING）→BUILDING→READY；任何生成失败到ERROR并保存管理侧last_error，build_attempts递增，next_build_at=now+min(15×2^(attempts-1),300)秒；worker每15秒只认领next_build_at已到期项，成功归零。BUILDING租约120秒，过期可以接管；只拥有当前 lease_owner 的worker可发布新版本。

* 在 contest 行锁内检查时间与 pending、取得候选source_seq=next_attempt_seq，再在 rank header认领lease、递增next_version分配从未使用的candidateVersion；不能用已发布version+1反复复用候选。每30秒续约120秒，候选SQL执行前后检查owner和未过期lease，过期owner不可复活。所有写 attempt/result 的路径也锁 contest，因此不会漏掉“已经受理还没插入”的最后提交。header.source_seq只在成功发布时切换，重建期间保留旧版元数据。
* 有 PENDING 就不宣称最终完成。结束后15分钟仍有pending置ERROR、记录待查 submitId/数量、报警，但**不**把运行中判题自动0分/假终态；后续真实结果到达可重试生成。缺失队列/回调需要管理员修复投递或既有判题服务，不用猜测分数。
* 新比赛使用 contest_records 投影聚合，该投影只包含最大有效 attempt_seq；它和全部attempt收据保持可对账。扫描数据量≈参加人数×题数，不是每次榜单API扫描整个 submit_log/case。
* 每次生成只执行一次数据库侧INSERT INTO contest_rank_entry ... SELECT：user_contest LEFT JOIN同contest/user的contest_records，GROUP BY user_id汇总，再用RANK(score DESC)/ROW_NUMBER(score DESC,user_id ASC)写入独立candidateVersion。该SQL单独事务，不占contest/header长行锁，不把全部排行/提交/代码搬进Java内存；连接查询超时60秒，独立心跳每30秒续约。执行前后校验owner/lease，失去租约的独立版本不能发布。批500用于旧版本清理而非重复分页计算全榜。旧版本仍有效；发布时再次锁 contest/header核验owner、source_seq、无pending、时间/roster未变，原子切换version/READY。失败不暴露半成品。
* 正常READY后拒绝新增赛事 attempt，所有原attempt已terminal，因此没有合法晚结果；重复callback不改成绩。若首次真正结果仍未到，state必须一直WAITING/ERROR而不是READY。人工重建不会重新判题或修改成绩，只从既定投影生成新版本。
* 旧version的entries保留最近2版，其余在新版本发布后按batch清理；管理员审计/日志另保留。对前端已显示的旧页不强行混合新版本，需要返回version并刷新分页状态。

Redis只缓存**READY对应的一页ID/分数和总数**：`problem:contest:final-rank:v1:{contestId}:{version}:{page}:{size}`，TTL 6小时；实际对象用StringRedisTemplate + 应用ObjectMapper的普通JSON，不引入全局default typing。每次请求先用DB验证访问资格/活动状态并读当前header version，之后查页缓存；资格检查不能被cacheable包住。用户名/特殊角色由当前页最多50/100ID的批量安全摘要查得，不长期缓存到榜单分数页。

无 WAITING/ERROR/personalized 权限结果缓存，不缓存旧 roster 判定。并发cache miss允许少量相同DB分页查询，数据库snapshot索引足够，第一版不加分布式cache填充锁。新version自然使用新key；旧key靠TTL过期，不用KEYS全量删除。Redis宕机降级DB分页，DB才是真实排名。header.state为BUILDING/ERROR而version>0时，ranking仍返回已发布的version，UI只提示正在重建/重建失败，不能把旧最终榜单清空；新版本发布前不得覆写header的旧generated_at/source_mode/rule_version/total_users。

### 9.5 历史成绩与迁移

旧contest_records无受理时间/顺序，非 OJ 成绩可能未换算，submit_log缺最终score；**不能安全重放恢复所有历史最终分数**。确定策略：迁移不修改历史成绩/排名源，scoring_version=1的已结束赛事从现有投影构建 LEGACY_RECORD_V1，sourceMode=LEGACY，仅加稳定同分排名。UI注明历史成绩，不声称经过新评分规范。

无提交的未开始比赛为version2；有成绩但未开始的异常数据仍version1，需要运维核查不能直接切新。部署预检查发现正在进行比赛、活跃补交写入或在途judge则停止本次成绩链切换，安排无赛事/无任务维护窗口后继续；不让新旧应用混跑结果落库。历史终态 applied标记只维护窗口处理，不把真实pending设置成完成。

新题目snapshot以部署后现有列表作为历史展示快照，不能宣称它就是原比赛时题单；历史榜单分数只取contest_records，不用当前题单max_score重新计算。历史查询显示aggregate，不暴露不可信的旧满分百分比。新赛事按冻结快照严格计算。

## 10. 缓存清单和一致性

| 前缀/范围 | 变化/TTL | 真正依据与失效 |
| --- | --- | --- |
| `problem:profile:v3:` | 新增，2小时，纯练习/比赛/热力图基础数据 | content 单独查询题解最多51条并即时校验；前端组合，problem 不聚合 content；禁止继续缓存完整getActivity |
| `problem:profile:v2:` / v1 | 兼容清理，停止读取 | 发布时受控SCAN+UNLINK旧前缀，不FLUSHDB；仅本功能缓存 |
| `problem:solution:` | 停用个人化详情与recent成品缓存 | 详情/公开列表/recent最多20条从DB查询；用索引避免授权绕过，不在这一版另做viewer级cache |
| `content:comment:write:` | 新增，60秒，Lua写入限频 | 只是流量限制，不是评论真数据 |
| `problem:contest:final-rank:v1:` | 新增，6小时，版本化最终页 | header/访问资格每次DB查，缓存丢失可回源，新版不复用旧key |
| follow / likes / inbox-count / comments | 不新增Redis缓存 | 小范围索引查询/DB计数与事务，避免双写 |
| outbox / fanout / rank lease | 不存Redis | DB行状态和租约，即使Redis清空也不丢事件/资格 |
| 既有judge/session/step-up/cache | 保留 | 不重命名、不清空、不改TTL |

CacheCatalog给v3、content comment限频、final-rank追加稳定类别ID 23/24/25；24 尚未实现，可直接用 content:comment:write:。已有 problem:solution 类别保留为停用旧缓存清理标签，不挪用 ID。reference 是 MySQL 读模型，不新增 Redis 共享详情/权限缓存。v2改为旧版清理标签。新功能不重写已有缓存管理的KEYS/序列化实现，以免扩散任务范围；在大缓存规模下KEYS本身是现存运维风险，单独记录，不当成outbox或最终排名的依赖。

计数与关系更新全部同库事务，缓存只保存可重建页面。read_at/点赞状态不进入共享缓存；私有访问必须走即时DB状态。题解关闭/审核操作和发评论用同锁解决并发，不靠缓存失效时序保证权限。

## 11. 跨模块风险与边界决策

| 问题 | 确定处理 |
| --- | --- |
| DB提交成功、消息尚未发进程退出 | DB outbox，worker重试；不用afterCommit直接发送作为唯一机制 |
| MQ成功但outbox SENT未写成功 | 允许重复；recipient+dedupe、fanout event PK、award receipt唯一键幂等 |
| 重复关注/取消 | 复合PK + 状态型接口；不按角色给社交授权 |
| 点赞并发/反复刷通知 | 资源行锁、持久关系active、首次通知flag、计数原子更新 |
| 评论被关闭/删除期间继续回复 | solution→root→parent锁序，即时状态；根tombstone不级联回复 |
| 管理原因泄漏 | 普通VO无reason，作者处理结果与后台权限入口分离，个人inbox只owner |
| 管理限制被作者覆盖 | 作者private与moderation_state分离，显式 DTO映射，管理员专用命令 |
| 题解经缓存绕过身份/比赛内容泄漏 | 移除授权外层cache，关联PUBLIC题过滤，profile实时题解 |
| 最后提交/交卷/结束并发 | contest同一行锁+统一serverClock；已受理结果允许晚到 |
| OJ乱序/重复结果 | attempt_seq而非完成时间，result_applied事务幂等，case唯一键 |
| 积分跨库回滚/重复 | outbox积分事件+user本地receipt+原积分SQL，首AC唯一插入 |
| 题单修改污染已运行比赛 | 小型冻结snapshot；题单写入锁；运行中的答案/case改动拒绝 |
| 最终榜单半成品/重建冲突 | lease owner+source_seq校验、candidateVersion、原子发布DBheader |
| 循环业务依赖 | problem→user只读摘要；user通知不回查problem；反向结果来自judge既有internal |
| 历史信息不可恢复 | 保留历史来源和LEGACY标记，不猜测/静默重算旧成绩 |
| 服务器8GB内存/有限磁盘 | 固定batch、现有进程scheduler、分页榜单、无正文消息，outbox/通知有保留周期 |

待实施环境风险：服务端时钟/数据库时区需一致；MySQL实际版本须为8（window functions/SKIP LOCKED）；Rabbit账号需新exchange/queue声明权限；生产网络必须阻断直连internal；前后端新增管理删除API需原子发布或用功能开关；新进程调度器须为有限线程，不为每个用户/比赛开线程。

## 12. Implementation Tasks 与依赖

32项，其中T01/T02已完成，不重复执行。新增4项R02 correction与T03的双域relay拆分。各Task15字段见LUNA_TASKS，题解迁移与大任务共用此依赖图。

| Task | 目标 | 前置 |
| --- | --- | --- |
| T01 | 用户侧社交数据基础（已完成，归档） | 无 |
| T02 | 题解互动、审核与事件基础及管理权限（已完成，归档） | T01 |
| R02-A | 题目域只读内容资格契约 | T01, T02 |
| R02-B | T02 forward correction：content schema、模型与安全数据搬迁工具 | T01, T02 |
| R02-C | 现有题解完整服务迁移、资格投影和双路径兼容 | R02-A, R02-B |
| R02-D | 个人主页题解拆域、缓存清单与遗留清除 | R02-C |
| T03 | 共享事件契约与消息拓扑（不含业务relay） | T01, T02 |
| T03-C | content community outbox relay与清理 | R02-B, T03 |
| T03-P | problem积分与判题派发outbox relay | T02, T03 |
| T04 | 关注后端与安全用户摘要 | T01 |
| T05 | 通知消费、可恢复粉丝扇出与积分幂等消费 | T03, T03-C, T03-P, T04 |
| T06 | 本人消息API、已读语义与后台重放 | T05, T03-C, R02-C |
| T07 | 个人主页关注入口与用户列表 | T04, R02-D |
| T08 | 题解可见性、缓存安全、CRUD及管理处理 | R02-D, T03-C, T04 |
| T09 | 题解点赞与首次通知 | T08, T05 |
| T10 | 一级评论创建、分页和文本校验 | T08, T04, T05 |
| T11 | 两层回复与独立分页 | T10 |
| T12 | 评论删除、管理原因与关闭评论区 | T11 |
| T13 | 评论点赞、状态与作者通知 | T12 |
| T14 | 前台题解互动与纯文本评论组件 | T09, T13 |
| T15 | 后台题解审核与评论管理抽屉 | T12, T08 |
| T16 | 个人收件箱、徽标与页面可见轮询 | T06 |
| T17 | 比赛成绩schema、历史来源与题目快照基础 | T03-P |
| T18-A | 统一受理与持久OJ派发 | T17 |
| T18-B | 交卷、赛程与参赛成员原子边界 | T18-A |
| T18-C | 冻结题目读取与题单/评分内容修改隔离 | T18-B |
| T19 | 非OJ统一计分、草稿隔离与首AC积分事件 | T18-C, T05 |
| T20 | OJ结果原子应用、重复/乱序保护与伪造入口清除 | T19 |
| T21 | 版本化最终榜单生成、租约与历史模式 | T20 |
| T22 | 最终榜单API、角色/成员权限与缓存 | T21, T04 |
| T23 | 比赛结束成绩区域与后台重建入口 | T22 |
| T24 | 跨模块集成、迁移预检查与发布手册 | R02-D, T07, T14, T15, T16, T23 |

```text
T01✓ -> T02✓
  ├─ R02-A ─┐
  ├─ R02-B ─┴─> R02-C（旧题解整体迁移）-> R02-D（profile/cache）
  ├─ T03（共享协议）-> T03-C（content relay，另依赖R02-B）
  │                -> T03-P（problem relay）
  └─ T04（follow/摘要）
T03-C + T03-P + T04 -> T05 -> T06 -> T16
R02-D + T04 -> T07
R02-D + T03-C + T04 -> T08
T08 + T05 -> T09
T08 + T04 + T05 -> T10 -> T11 -> T12 -> T13
T09 + T13 -> T14；T12 + T08 -> T15
T03-P -> T17 -> T18-A -> T18-B -> T18-C
T18-C + T05 -> T19 -> T20 -> T21
T21 + T04 -> T22 -> T23
R02-D + T07 + T14 + T15 + T16 + T23 -> T24
```
上图省略T06的R02-C/T03-C边；完整依赖以表及各Task第3字段为准。比赛持久派发/排名不引入content同步业务依赖；T19等T05是为了已确定的user积分consumer，不是把题解放回problem。T03-C只准备worker，真实启用仍需R02-C/D完成并CUTOVER。

### 12.1 交付与发布顺序

先按R02-A～D完成题解整体迁移包，授权后的发布遵§13 inspect/prepare/维护copy/verify/CUTOVER；R02-B/C的中间版本不能单独发布。社交功能在此基础上按依赖完成，先user拓扑/consumer，再content社区producer及前端；比赛分支的problem producer/评分链维持原域，T17～T23必须维护窗口、所有判题在途处理完后成组切换。用户没有要求本阶段上线，任务文档的发布操作只有在之后用户授权部署时才能执行。

运行新worker清单：content现有进程的元信息投影刷新（1秒最多100项、目标5分钟循环）、community relay（1秒20项、发送pool4/queue20）和清理job；problem现有进程的points/judge relay及最终榜单job；user现有进程的fanout与清理job。两域relay均有独立开关，所有线程/批量有界，不新增常驻容器/服务。浏览器隐藏时停止轮询，关闭页面停止页面定时请求。上线文档必须列实际schema、cache keys、权限、queue、周期与关闭方法。

### 12.2 总体验收场景

1. A关注B，B首次公开题解，A收到一条消息；重复发送、worker中途退出、消息重试不重复；晚关注/取消按上述送达语义处理。
2. 私有题解、比赛专用题目题解不向粉丝泄漏；作者先读取再他人请求也不能借缓存读取。
3. 点赞→取消→重赞计数正确，首次消息只一条；多线程重复PUT/DELETE不负数。
4. 评论/任意子回复固定两层展示，独立分页；XSS/Markdown/图片语法只能是字面文字；emoji正常。
5. 根评论删除保留原回复，公共正文/原因不泄漏；作者能查看管理员处理原因。
6. 关评与发表评论竞态遵循事务顺序，已关闭无新增，既有内容与点赞正常。
7. 学生无需新增默认权限即可互动；限制角色禁止评论；管理员/超管真实能看到路由/按钮并管理，越权403/404。
8. 用户只标自己通知；read-all不标记throughId之后的新消息；30秒轮询不可重叠、后台暂停、焦点恢复即时刷新、logout彻底清空。
9. 计分以题单满分统一，较早结果晚到不覆盖新成绩；JUDGE_ERROR兜底不清空原有效分；草稿不算提交。
10. 比赛结束同毫秒提交/交卷受理边界明确；结束前受理结束后返回的结果纳入终榜；未应用结果仍存在时不出现READY。
11. 同分1/1/3、未交卷和0分参与者都在；密码/白名单非成员不得看榜；作业不开放比赛最终榜。
12. 同时两个worker/重复rebuild、Redis不可用、构建半途崩溃均可恢复；旧版榜单不被半成品覆盖。
13. 旧数据无擅自重算；历史三份迁移和新forward迁移按manifest/phase重复验证不丢业务数据/重复grant/重复事件，CUTOVER后不从旧源重导；本地构建且测试不过不得部署。

## 13. 题解域迁移、发布和回滚（必须执行的固定流程）

### 13.1 文件与操作阶段

1. **inspect**：只读检查源/目标 schema、是否执行历史T02/半完成列marker、现有权限、collation、旧solution FK和孤儿/关系计数、outbox类型/状态、进程/维护开关。不凭git记录推断数据库状态；无完整执行journal时将schema证据及历史三文件SHA写入preflight报告，不伪造“已执行”。同一MySQL实例迁移是当前部署假设；发现不同实例中止，另行设计/授权。
2. **prepare**：常规schema-only forward _4在content创建 §3.3完整表及state；初始化SQL只用于新装。manifest显式字段为 migration_id/file/sha256/source_mode/phase/schema/order，文件存在和SHA不一致立即失败；旧迁移_1/_2/_3 SHA固定，_2只对LEGACY且存在problem solution表适用。FRESH跳过旧_2而直接建content最终结构，_1/_3仍正常应用；不重新创建problem solution表。_3依旧真实menu/perms定位，不重新给student权限。当前三个入口 apply.sh/dev/deploy 全用manifest，取消server盲V*.sql遍历；_5不属于普通prepare阶段。
3. **maintenance copy**：需要显式授权和维护确认。冻结旧题解全部写入、后台批处理和相关producer/relay，切换期间不放行新content题解写入。原题目/判题库不混用新content model运行。源缺T02结构时用冻结历史_2规范化（先备份并验证marker），**已存在firstPublished列正常marker时不重新给NULL填值**。按source_mode/manifest创建PREPARED marker。工具_5在同实例仅此维护阶段允许跨schema搬数据；应用运行态仍无跨库SQL。
4. 父表solution→正文→likes/comments/commentlikes/audit→社区outbox，显式列keyset最多500/小事务；保留ID、时间精度、private/del/moderation/计数/version/firstPublished/first_notified/原正文，不重建事件。MySQL输出/输入均utf8mb4，无CAST Long成double、无字符截断。目标同PK仅逐列含NULL/字节相同可跳，不同立即停止，不能UPSERT覆盖用户新数据。历史private、deleted、AUTHOR_ONLY、关联失效题目也全部保留。
5. source problem_event_outbox仅七community事件搬至content，保留eventId/dedupe/payload/status/attempts等，积分/判题留源。停worker且租约过期后将SENDING恢复PENDING再copy，不能把不确定事件标SENT；消费者幂等允许恢复后重发。未知event_type阻止迁移。源社区行暂留只读备份，新problem worker/cleanup均过滤到两合法本域类型，不能继续发旧社区行。源outbox若为空，也必须记录检查证据。
6. **verify**：copy结束COPIED；按表显式列、有序主键流式SHA-256：每字段先写4字节有符号big-endian长度（NULL=-1），非NULL随后UTF-8值；整数十进制不加前导零、布尔0/1、时间统一yyyy-MM-dd HH:mm:ss.SSS、文本逐字保留、JSON用同一ObjectMapper按key排序的确定序列化（不得将JSON ID转double）。列顺序固定在各表copy列清单。不能简单GROUP_CONCAT/易碰撞拼接，检查全部row count、逐行/主键集合、FK/orphan/unique、计数关系、正文byte length。source/target SHA与counts_json保存到state，异值立即失败；不自动删孤儿或修计数。reference是可重建辅助表，不参与legacy字节等价摘要：从全部已关联problem_id一次性初始化元信息，对缺失/失效映射安全不可公开；不触发publication。原正文TEXT上限保持，source越界问题报告而非截断。verified_at/status=VERIFIED。
7. **cutover**：只在VERIFIED且source维护期间没有再写且二次摘要一致时，通过CAS设置CUTOVER/cutover_at。部署同版本problem（删除solution runtime依赖）+content（接管题解）+Gateway legacy alias+新frontend；新content所有题解写入口/relay在marker未CUTOVER时503/disabled，不能通过先启动worker抢旧数据。FRESH无源时copy为零记录并验证新装结构/seed一次，不能省略状态gate。保留旧源表/备份，不运行DROP。
8. CUTOVER后重复 prepare/copy 禁止旧源重导；inspect/verify可执行但不能把新增content数据与冻结旧source不等误判为丢数据，切换后检查target自身完整性。schema脚本可重跑但不能回退marker状态；读取state的故障同样failclosed。没有设计双写、跨库XA或后台持续同步旧solution表。

### 13.2 兼容、缓存与权限

迁移版本需维护窗口，不要求混合旧problem写服务与新content写服务同时在线。新Gateway与content必须作为配套发布，旧problem实例先下线/停止solution写，不能同一URL在两库随机写。legacy alias覆盖现有solution所有CRUD/admin/list/recent，对旧请求保持HTTP/R，不放松授权。前端profile三接口配套，旧bundle仅活动tab可能降级，不再由problem回查content。各角色刷新既有登录权限机制即可，权限_3不重做、不按ID猜授予；保持teacher原grant。content新数据库连接使用现有Nacos/配置风格指向db_content，迁移账户临时有双schema读写权限，运行态不依赖该权限；不把服务器凭据写入仓库。

受控SCAN+UNLINK只清旧problem:solution和profile:v1/v2（CacheAspect实际prefix按冒号检查），新profile:v3纯基础；content:comment:write由T10登记ID24。reference为MySQL投影，非Redis新缓存。不自动清认证、积分、判题或整个Redis。迁移不触碰MinIO的case/头像/文件，题解正文未迁至MinIO。

### 13.3 回滚与验证范围

* cutover前：旧problem仍是唯一业务源；修复prepare/copy失败后可重试，target仅辅助副本，不自动DROP。恢复旧题解写入前退出维护并把预切换新content写/worker禁用。
* cutover后未产生任何新写/事件：维护下用摘要证明两侧仍等价，可整体回切legacy网关与旧problem镜像，不能只改一路由。
* cutover后已有新写/likes/comments/events：**禁止直接换回旧problem应用和旧源库**。首选前滚修复，或回滚到同样使用db_content的兼容题解镜像。若必须反迁移，需新的独立授权、停写停worker、全量双向差异清单、将content数据/状态/事件完整反拷并验证、旧problem schema兼容性检查（旧题目FK可能不能容纳历史孤儿，不能盲重建）、再整体回切；不能仅恢复最初备份丢新数据。此反迁移不在R02任务默认执行范围。
* 旧源表至少保留一次完整发布观察窗口及可恢复备份；物理purge另行授权，绝不由启动/迁移脚本自动执行。
* T24覆盖三个隔离schema、新装/旧T02已执行/未执行/半执行、copy中断及重复、target冲突、CUTOVER拒绝重导、两前缀CRUD/身份、profile无同步环、nonPUBLIC与problem停机、事件去重/无历史通知、投影计数最终一致与正文即时权限、比赛历史/判题/排名不变。文档修订阶段未执行这些新测试，未连接任何真实库或服务器。
