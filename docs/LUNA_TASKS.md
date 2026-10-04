# Coding Agent 执行清单：关注、通知、题解互动与最终比赛榜单

日期：2026-10-03。只在收到对应 Task 的实施指令后改代码。T01/T02已完成，下一批指派R02-A或R02-B，不指派重复T01/T02。本文件不是“立即执行全部任务”的授权。本版共32项：T01/T02 已完成并归档，新增 R02-A～D correction，原 T03 拆为 T03/T03-C/T03-P，T18 为 A/B/C。按前置依赖执行，不按字符串编号盲排。

## A0. T01/T02 已执行后的架构修订

本次修订只改两份计划，没有执行migration/部署/服务器命令。真实DB执行状态未知，旧三migration冻结，SHA与实际实现处理见 plan§0。不允许重做T01、不允许重新按原T02把互动继续建在problem；T01/T02下面15字段是历史验收档案。新的工作从R02-A/B开始，所有现有题解与正文也必须迁，不是只迁新评论/点赞。

* 保留T01 user四表/Mapper/测试和commons/common-api HTTP异常实现、现有安全改动。
* T02四互动审计表/两个枚举/映射迁content，旧Solution CRUD/body/list/recent随R02-C迁；新装SQL归db_content；旧源表只读备份不自动删。
* ProblemEventOutbox留problem，仅points/judge；ContentEventOutbox另建承载community，user通知不反查两服务。
* 已写三历史migration不改；追加_4 schema/_5显式transfer。尚未创建的contest migration改_6，不改排名设计。
* canonical content-api；仅旧problem-api/solution/**在Gateway代理兼容；Vue页面/menu/旧problem:solution/comment权限字符串保留。
* plan§2.1/§13以及下方B～F是固定契约；读模型不是授权缓存，API资格每次fresh。不得自行改成跨库JOIN、problem↔content同步调用、全扫题解或双写。

## A. 执行规则

1. 开始时运行 `git status --short`，保留用户所有现有修改。当前工作区已有禁止权限、Step-up 和热力图，禁止回退。先阅读本任务列出的现有文件，不能凭名字猜实现。
2. 全部确定契约见本文件 B～F 及 [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) 的第3～10节；这些是已定规则，不重新选择表、权限、事件、排名或删除语义。发现实施与文件事实不符时记录具体冲突并停止相关步骤，不擅自换体系。
3. 使用 Java 11、Maven 的现有 target 增量构建；使用 pnpm。不能 npm install、mvn clean、永久关闭测试、建一次性编译目录。不得启动生产容器、修改端口/JVM全局设置或提交部署，除非用户另行授权。
4. 文件编辑使用 apply_patch；Controller 只做身份/验证/委派，SQL 在 Mapper/XML，业务事务在 Service，外部网络调用不在持有行锁的事务中。
5. 不引入新微服务，不恢复 SSE，不传播/持久化任何 Token。不替换 Markdown 编辑/预览、头像裁剪、菜单动画、全局主题或已有面板组件。
6. 新 UI 用 scoped CSS/现有 Element Plus 与设计变量，不用 `!important`，不增加任意全局 z-index。评论只用插值/textContent，严禁 MarkdownPreview/v-html/图片。页面业务文案不得直接抄“短轮询/Redis/幂等/任务/架构要求”。
7. 一个Task提交一个清晰patch；只有用户要求git提交时才commit。每次报告变更文件、命令/测试结果、迁移与新缓存/后台job；没有实际执行的测试写“未执行”，不能把单元测试称为完整端到端。
8. 新分页：默认20、最大50、页码>=1，保持 R/PagedResult。所有ID/长序号/版本 JSON 为string，金额/分数为精确十进制string，前端 IdType=string，不能Number(id)。400/401/403/404/409的HTTP与R.code一致。
9. 身份从AuthUtil，管理用权限字符串，不判断角色名。role_name只用于迁移默认grant。生产异常/内部判题错误不得返回前端或通知。
10. 服务端时间统一Asia/Shanghai，使用可注入Clock测试边界。新SQL不使用cast(ID as char)、不混合collation、不执行初始化DROP脚本到现有数据库。

### 路径缩写（是确定路径，不是文件名占位）

* U = `user-service/src/main/java/com/anishan/user`
* P = `problem-service/src/main/java/com/anishan/problem`
* D = `content-service/src/main/java/com/anishan/content`（C仍指commons，不能混用）
* J = `judge-server/src/main/java/com/anishan/judge`
* A = `common-api/src/main/java/com/anishan/api`
* C = `commons/src/main/java/com/anishan/commons`
* V = `oj-vue/src`
* UX/PX/DX = 对应user/problem/content-service的 `src/main/resources/mapper`
* UT/PT/JT/AT/CT = 对应user/problem/judge/common-api/content-service的 `src/test/java/com/anishan/{user|problem|judge|api|content}`
* S = `resources/sql`
* `entity/Foo.java` 等相对路径均与本项注明U/P/A等前缀合并。禁止把缩写本身当成目录创建。

## B. 固定数据库契约

实施必须逐字段落实 IMPLEMENTATION_PLAN 第3节。以下为执行索引，不允许删字段/换名。

* 已完成T01迁移 `S/migration/V20261004_1__user_social.sql`：
  * user_follow(follower_id,followee_id,created_at)，PK两ID；反向索引(followee_id,follower_id,created_at)，正向索引(follower_id,created_at,followee_id)，不允许self。
  * user_notification(notification_id,recipient_id,actor_id nullable,event_id,dedupe_key,type,solution_id nullable,comment_id nullable,action nullable,reason nullable,occurred_at,created_at,read_at nullable)，UNIQUE(recipient_id,dedupe_key)，索引(recipient_id,read_at,notification_id)、(recipient_id,notification_id)。
  * notification_fanout_job(event_id,actor_id,solution_id,occurred_at,cursor_user_id=0,status=PENDING（仅PENDING/SENDING/DONE/FAILED）,lease_owner nullable,lease_until nullable,attempts=0,next_attempt_at,last_error nullable,created_at,updated_at)，PK event_id，索引(status,next_attempt_at,lease_until)。
  * user_point_award_receipt(user_id,problem_id,event_id,amount DECIMAL(10,2),created_at)，PK(user_id,problem_id)、UNIQUE(event_id)。
* 已完成T02历史迁移 `S/migration/V20261004_2__solution_interactions.sql`（原db_problem；仅旧装源库规范化，不重新写历史文件）：
  * solution_explanation增加del_flag=false、moderation_state=NORMAL、comments_open=true、like_count/comment_count BIGINT=0、first_published_at nullable、version BIGINT=0；索引(del_flag,moderation_state,private,create_time,solution_id)。
  * solution_like(solution_id,user_id,active=true,first_notified=false,created_at,updated_at)，PK(solution_id,user_id)、索引(user_id,solution_id)。
  * solution_comment(comment_id,solution_id,user_id,root_id nullable,parent_id nullable,reply_to_user_id nullable,content VARCHAR(4000),state=VISIBLE,deleted_by nullable,deleted_at nullable,like_count/reply_count BIGINT=0,client_request_id,created_at,updated_at)，UNIQUE(user_id,client_request_id)，索引(solution_id,root_id,created_at,comment_id)、(root_id,created_at,comment_id)、(user_id,created_at)。
  * comment_like(comment_id,user_id,active=true,first_notified=false,created_at,updated_at)，PK(comment_id,user_id)、索引(user_id,comment_id)。
  * solution_moderation_action(action_id,solution_id,target_type,target_id,author_id,operator_id,action,reason VARCHAR(500),created_at)，索引(target_type,target_id,action_id)、(author_id,action_id)。
  * problem_event_outbox(event_id,dedupe_key,event_type,exchange_name,routing_key,payload JSON,status=PENDING,attempts=0,next_attempt_at,lease_owner nullable,lease_until nullable,last_error VARCHAR(1000) nullable,created_at,sent_at nullable)，UNIQUE(dedupe_key)，索引(status,next_attempt_at,lease_until)。
* 所有新ID BIGINT；实体主ID ASSIGN_ID，复合键用显式Mapper SQL，不用ServiceImpl.getById假装单列PK。event_id/client_request_id CHAR(36)，dedupe_key VARCHAR(160)，这些ASCII标识用ascii_bin。时间DATETIME(3)。状态字符串枚举。未注明nullable则NOT NULL；created/next时间由应用显式赋值。新表InnoDB+utf8mb4_unicode_ci。新增同库资源外键RESTRICT，不跨库建用户FK，不级联删除审计/榜单。
* R02-B forward schema/transfer：新增_4 content_solution_schema、_5 content_solution_transfer，详细约束见plan§3.3/§13及R02-B；题解两旧表+四互动表在db_content，ContentEventOutbox另建，同结构但只七社区type；ProblemEventOutbox留db_problem只points/judge。
* 必须逐字段落实plan§3.3的solution_problem_reference（仅元信息投影）和content_solution_migration_state（一次性迁移阶段/摘要/CUTOVER保护）；不换字段名，迁移phase不能随普通启动执行。
* T17迁移 `S/migration/V20261004_6__contest_final_rank.sql`：
  * contest增加scoring_version INT=2,next_attempt_seq BIGINT=0,problem_snapshot_at nullable。
  * submit_log增加score DECIMAL(10,2) nullable,result_applied=false,completed_at nullable；索引(contest_id,status,submit_time,submit_id)。
  * contest_records增加applied_attempt_seq BIGINT=0,applied_attempt_id nullable；保留原每人每题唯一键。
  * judge_case_log先备份/检查旧重复，然后UNIQUE(submit_id,case_index)，保留最新update_time/最大id一行。仅清理明确重复，不删整个日志表。
  * contest_problem_snapshot(contest_id,problem_id,problem_order,title VARCHAR(255),problem_type INT,max_score DECIMAL(10,3),created_at)，PK两ID、UNIQUE(contest_id,problem_order)。
  * contest_attempt(attempt_id,contest_id,user_id,problem_id,attempt_seq,submit_id nullable,kind VARCHAR(16),accepted_at,state=PENDING,judge_status nullable,score DECIMAL(10,2) nullable,correct nullable,completed_at nullable)，UNIQUE(contest_id,attempt_seq)、UNIQUE(submit_id)、索引(contest_id,state,attempt_seq)、(contest_id,user_id,problem_id,attempt_seq)。
  * contest_rank_snapshot(contest_id,state=WAITING,version=0,next_version BIGINT=0,build_attempts INT=0,next_build_at DATETIME(3),source_seq=0,source_mode,rule_version,total_users=0,pending_count=0,lease_owner nullable,lease_until nullable,generated_at nullable,last_error VARCHAR(1000) nullable,updated_at)，PK contest_id、索引(state,next_build_at,lease_until)。
  * contest_rank_entry(contest_id,version,user_id,rank_no,row_position,score DECIMAL(18,2),correct_count INT,answered_count INT,handed_in BOOLEAN,created_at)，PK前三ID、UNIQUE(contest_id,version,row_position)、索引(contest_id,version,rank_no,row_position)。
* migration必须information_schema检查可重跑，同步对应db_user.sql/db_content.sql/db_problem.sql。first_published_at只给初次加列时已有有效公开题解赋create_time，不能重跑把新事件吞掉。
* 旧赛事存在成绩/提交则scoring_version=1，保留旧分数；其余无提交赛事为2。不重算历史非OJ分数、不用旧submit_log猜分数。不得部署时把真实PENDING标完成。迁移入口统一，不能漏掉既有20261003迁移。

## C. 固定API与权限执行约定

按 IMPLEMENTATION_PLAN 第8节逐路径/字段实现，不改前缀或R结构。Controller路径不含/user-api、/content-api或/problem-api网关前缀，frontend使用现有http工具。

* 普通关注/互动/本人消息无需新student权限，只要求登录、资源访问资格和所有权。policy:comment:deny只阻止新评论/回复，不阻止阅读/本人删除/取消点赞。题解写入保留policy:solution:deny优先。
* 普通可见题解本域del_flag=0，且 [作者本人 OR(private=0 AND moderation_state=NORMAL AND reference候选PUBLIC)]；全部筛选在括号外。返回公共正文/公开recent/列表、触发公开互动须plan§2.1本次problem批量fresh资格，不能跨库JOIN或相信reference放行。分页totalRecords是本地投影快照候选计数最终一致；最多3轮refresh/repair仍变化503；正文即时校验。不存在/不可见404，远程故障503。
* 仅作者/现有solution:edit管理权限能设置commentsOpen；后台审核/删除API专门填写reason。作者不能提交审核状态、计数、作者ID/删除字段。
* 新B权限problem:comment:list/remove挂solution-edit I路由，problem:judge:dispatch:retry挂judge-submit-log I路由；只默认grant admin/super_admin并补相应路由祖先及system:backend:access。role_name定位，不固定线上menu_id。比赛重建复用既有problem:contest:rank，补其所需祖先，不给所有教师按钮。
* public摘要只userId,userName,nikeName,specialRoles；internal批量POST /internal/user-summaries最大100 ID，不用管理UserVo、不逐个Feign查。
* 个人通知/{id}/read与page/unread-count/read-all只recipient=self；read-all带throughId，限定已看到的最大ID，别人ID404。
* solution detail/list追加likeCount/likedByMe/commentCount/commentsOpen/moderationState/effectiveVisibility；有效可见性固定PUBLIC/AUTHOR_ONLY/DELETED，由关联题目/审核/private/删除状态计算，UI不用private单字段误标公开。普通/管理新增编辑关联非PUBLIC题却private=false返回400，不暗中转换；历史非PUBLIC题解仅作者或管理读取。管理原因单独接口。公共评论VO不可包含reason/deleted_by/被删正文。
* 普通评论DELETE幂等；管理员评论POST /comment/admin/{id}/delete与题解POST /solution/admin/{id}/delete强制reason1～500。旧admin无原因DELETE明确400，配套更新前端。
* /record/judge-save与两个Feign无调用声明必须删除；同样删除POST /log/queue、/log/log-judge、/log/update、/log/change-status及SubmitLogClient无调用mutation声明，保留log读/轮询。/internal/**继续Gateway阻断，部署验证服务内网访问。新增状态只读GET /internal/judge-submission/{submitId}返回resultApplied/status给JudgeListener，不能对浏览器放行。
* 新API错误使用T01的ApiStatusException（400/401/403/404/409/429/503），对应handler返回HTTP/R.code一致并保持生产消息脱敏；不能只抛普通BusinessException却验收宣称HTTP409。
* 最终榜单仅CONTEST：PUBLIC登录可见，PRIVATE/WhiteList要求成员；管理走admin路径/problem:contest:rank。HOMEWORK返回400，未结束409。结果/快照不含代码/邮箱/错误原因。

### C1. 域迁移后的固定接口

* content-api所有solution/comment端点，现有CRUD/detail/list/recent一并迁移；内部Controller仍/solution与/comment。旧problem-api/solution/**仅Gateway转发content兼容，未实施comment不做旧域alias。
* POST /internal/content-problems/read是problem只读服务：请求problemIds最多100，返回每个ID的exists/deleted/auth/publicReadable/privateWritable/title，精确规则见plan§2.1。无viewerId/权限请求字段，不用既有cached题目detail。worker无用户身份时只获取PUBLIC元信息，privateWritable=false；非PUBLIC标题null。
* reference/state schema字段与分页最多3轮修复固定，plan§3.3；reference不是资格cache。资格检查在远程服务响应时线性化，本域mutation在本地锁提交时线性化，不做XA/反向content RPC。
* GET content-api/profile/{userId}/solutions返回userId/solutions/solutionsTruncated，50+1上限；本人由AuthUtil判定，普通他人不能因管理权限读取私有。problem profile移除题解字段，前端组合三个API。
* 保留problem:solution:*、problem:comment:list/remove作为RBAC字符串；不得因域归属换为content:*或迁移菜单component。权限迁移_3原样保留。

## D. 固定消息契约与并发

* community envelope：schemaVersion=1,eventId UUID,eventType,dedupeKey,occurredAt偏移ISO时间,actorId string,solutionId/commentId nullable string,recipientIds string[]最多2,action/reason nullable。SOLUTION_PUBLISHED无直接recipient，由用户本地关注表扇出。payload<=16KiB，不能含代码/正文/私有标题/Token/邮箱。
* 七种type和dedupe：SOLUTION_PUBLISHED=solution-published:{sid}；SOLUTION_LIKED=solution-liked:{sid}:{uid}；SOLUTION_COMMENTED=solution-commented:{cid}；COMMENT_REPLIED=comment-replied:{cid}；COMMENT_LIKED=comment-liked:{cid}:{uid}；SOLUTION_MODERATED=solution-moderated:{actionId}；COMMENT_MODERATED=comment-moderated:{actionId}。routing key依次solution.published.v1、solution.liked.v1、solution.commented.v1、comment.replied.v1、comment.liked.v1、solution.moderated.v1、comment.moderated.v1。
* 首次公开一生只一次；首次liked也仅一次（active取消不删除关系，first_notified永久标记）。新回复只COMMENT_REPLIED，对parent作者+题解作者取集合、剔除自己。普通评论作者=题解作者不自己通知。管理原因只送目标作者。
* content_event_outbox仅七community，problem_event_outbox仅POINTS_AWARDED/JUDGE_DISPATCH，两域各自relay/清理，不互相读表/调用Service。outbox同业务事务；worker每秒claim最多20，FOR UPDATE SKIP LOCKED，租约60秒，持锁事务提交后MQ send。固定4发送线程、队列容量20，上批未完成不继续claim，5秒确认超时使一批最多约25秒；不逐条同步等待导致租约被耗尽。dedicated communityRabbitTemplate不覆盖既有RabbitTemplate callbacks；correlated ack且无return才SENT。retry退避5秒倍增封顶300秒，20次FAILED，允许原ID重放。
* community.events.v1 topic exchange；user-community-notification.v1 durable queue绑定solution.*.v1/comment.*.v1；direct community.events.dlx.v1+durable user-community-notification.dlq.v1，dead routing key及DLQ binding固定user-community-notification.dead.v1。专用AUTO listener，事务提交才返回；失败抛出，3次后DLQ。未知schema/type不得吞掉。
* fanout job事件ID唯一，consumer只注册job就ACK；worker每秒claim1，游标follower ID，每批200，创建notifications与cursor原子提交。created_at<=事件时间且当前仍关注才投递，过滤已删除/封禁接收者。每次claim仅处理一批200；有剩余回PENDING/next=now/清lease/失败次数归零，完成DONE，避免长任务过期。失败lease/retry同outbox。没有new follow历史补发。
* 所有solution/comment mutation锁顺序solution→root（如果存在）→parent/target→like。根parent重复ID只锁一次，按拓扑锁不会递归。评论子回复在删除root后仍能存在/显示，只可回复VISIBLE parent。
* 评论normalize CRLF/NFC/trim，1～2000 code points且<=8192 UTF8bytes，拒绝NUL/非法代理对/除TAB和LF外控制符。UUID clientRequestId同用户唯一，同内容目标重试返回原ID，不同409。幂等成功记录优先于当前关评/限频检查；唯一冲突在事务回滚后外层查询原记录校验，不吞事务错误并继续增计数。
* solution.comment_count只计VISIBLE全部根+回复；root.reply_count只计其VISIBLE回复。创建/删除只增减一次，deleted tombstone根即使回复数0也保留。
* comment state=VISIBLE/AUTHOR_DELETED/ADMIN_DELETED；deleted content保留DB但公共为null。删除不级联、不支持edit/restore。关评禁止new comments/replies但保留显示/likes/删除本人。
* 点赞PUT/DELETE是设置状态，不toggle；计数变化、关系active、first_notified、outbox一个事务。自赞可记数不通知。取消旧关系即使目标隐藏也可操作，但不返回隐藏count/content。
* POINTS_AWARDED为内部非通知事件：eventId,dedupeKey=points-awarded:{uid}:{pid},schemaVersion=1,occurredAt,userId/problemId string,amount精确decimal string；account.points.v1 direct exchange，key points.awarded.v1，durable queue user-point-award.v1，独立direct account.points.dlx.v1/durable user-point-award.dlq.v1，dead routing key及DLQ binding固定user-point-award.dead.v1。用户事务INSERT receipt成功才调用原积分SQL，重复不加。
* JUDGE_DISPATCH为outbox固定分支，原JudgeInfo JSON，exchange=judge-exchange,key=judge-info,payload<=600KiB，dedupe=judge-dispatch:{submitId}。不得把community envelope投到该队列，不改judge并发和Rabbit全局配置。FAILED派发不假造结果/标完成；QUEUE/PENDING保留等待重放。
* T03-P声明judge-exchange.dlx.v1 direct、judge-info.dlq.v1 durable并绑定judge-info.dead.v1，不修改已有judge-info-queue声明arguments。T24在既有vhost通过仅匹配^judge-info-queue$的broker policy配置dead-letter-exchange=judge-exchange.dlx.v1、dead-letter-routing-key=judge-info.dead.v1；验证retry耗尽进DLQ，原ID重放。
* 通知consumer/fanout丢弃occurredAt早于180天的社区事件并记录指标；积分收据永久保留且积分事件没有该过期规则，不能把历史应加积分当通知丢弃。
* 社区replay仅FAILED且七种社区type，solution:edit；fanout replay仅FAILED、user:user:edit；重置attempts=0/next_attempt_at=now/lease为空，保留eventId/dedupe/cursor；judge dispatch-retry须新权限、原outbox FAILED且原submit未终态/未applied，不生成新submit。
* 后台job是原服务有限线程内scheduler，不是新容器/每用户线程。日志只ID/阶段/错误类型，不log payload、code或token。POINTS_AWARDED的FAILED仅手册受控运维SQL以明确eventId/type恢复PENDING并归零重试/清lease，不提供浏览器任意事件重放接口。

## E. 固定比赛规则

1. 不实现ACM/罚时；每题取最新受理且有效结果（非JUDGE_ERROR），按题分SUM；新WA/CE可覆盖旧AC。JUDGE_ERROR不覆盖旧有效分。attempt state=PENDING/COMPLETED/INFRA_ERROR，kind=OJ/NON_OJ，judge_status使用既有JudgeResult值，不新增另一套AC枚举。
2. OJ/非OJ比例scale8 DOWN，乘冻结题单max_score，再scale2 HALF_DOWN；原满分<=0则0，校验原分/满分/题单分非负且最终不超过max。选择填空提供重复index拒绝，不允许利用重复答案加分。
3. 总分相同同排名1/1/3；展示同分按user_id ASC。全部roster用户含0分、未交卷。correctCount仅辅助、不做tie-break。
4. contest锁→problem_list锁（创建snapshot时）→problem行升序锁（创建snapshot时）→submit_log→attempt→contest_records。所有受理/交卷/成员调整/回调用同contest锁，CONTEST用ServerClock start<=now<end，HOMEWORK用有效用户补交deadline否则end；二者均已joined未handin、题目在snapshot。attempt_seq通过锁内next_attempt_seq递增，不用ID时间排序。
5. snapshot冻结题目成员/顺序/名称/题型/max_score，不复制正文/case。题单mutation只锁list，冻结比赛不随题单变；有正式attempt后不得改赛程/listId。无attempt改listId可清空重建snapshot。进行中或结束后仍有PENDING任务引用题目的case/答案/题型/权重不可改。评分修改先查snapshot及未冻结比赛当前题单引用，锁相关contest升序→problem→case；锁problem后重查引用/时间/pending，新发现未锁有效contest则409重试，不能反向追加contest锁。先向同bucket新路径{problemId}/{caseId}-{UUID}.in/.out上传；通过TX后切换oj_problem_case.input/output引用，禁止覆盖原对象和锁内网络IO。失败回收新对象，旧对象不立即删除，手册仅无在途判题且DB对账后人工回收。
6. 草稿只保存答案，可初始化0分投影但不覆盖已有score/status、没有attempt、不加分。OJ正式受理保存当前答案，回调不覆写后来草稿；正式nonOJ先normalize再落库。RecordsServiceImpl.score从contest_records查询，不读废弃records.contest_id。首次AC才INSERT problem_complete成功并产生一次POINTS_AWARDED，不把WA也finish。
7. JudgeResultApplicationService原子log/score/case/attempt/投影/完成关系/outbox；result_applied true重复callback直接成功。回调身份用现有log验证，terminal不可退回中间态。Redis原submission token afterCommit释放，12秒TTL与sandbox并发不改。
8. 最终worker每15秒扫描20已结束CONTEST；固定生成线程2/队列2、共享心跳调度器1，仅空闲执行槽位claim租约，不能claim20后长时间排队。pending存在WAITING，超过15分钟ERROR报警但不自动0分。无pending在contest/header锁下认领BUILDING lease120秒，保存候选source_seq=next_attempt_seq并递增next_version分配全新candidateVersion。ERROR退避按build_attempts递增和next_build_at=now+min(15×2^(attempts-1),300)秒，成功归零；初始next_build_at=now。每30秒续约120秒，SQL执行前后校验未过期owner；过期owner不可复活，不同owner候选版本不得复用。只执行一次DB INSERT ... SELECT（GROUP BY+RANK/ROW_NUMBER）生成candidate，查询超时60秒，不把全榜读入Java；批500只用于旧版清理。发布再校验owner/source/pending/时间，原子header切version/source_seq；重建不能提前覆写已发布source_seq。新header source_mode/rule_version按scoring_version显式赋值，READY定时器不得重复claim。
9. DB聚合contest_records与user_contest，MySQL8 RANK/ROW_NUMBER。不是每API扫描submit_log/case。保留2个已发布版本；未发布候选/失去lease的碎片由同worker在确认不是当前版本/活动lease后清理。
10. header BUILDING/ERROR且version>0继续提供原版ranking，直到新版本成功；失败不改旧version的generated_at/rule/source/total。首版version=0时ranking=null。READY后没有合法新attempt，晚于end但accepted早于end的结果必须等待并计入，重复结果不重建。
11. cache=problem:contest:final-rank:v1:{contestId}:{version}:{page}:{size}，6小时；StringRedisTemplate+普通ObjectMapper JSON仅score/ID页。每次资格/DBheader检查先于cache；安全摘要实时批量补全。Redis失败DB降级，cache填充允许少量重复，不增加锁体系。
12. 历史scoring_version=1采用原contest_records、sourceMode=LEGACY、ruleVersion=LEGACY_RECORD_V1；新为CURRENT/WEIGHTED_LATEST_V1。不能从缺失历史受理数据推定新规则。上线成绩链前必须无正在进行比赛/补交写入/在途judge，否则preflight退出。

## F. 缓存、UI和测试公共验收

* 移除solution详情/recent cache成品；problem ProfileService只返回practice/contest/heatmap缓存基础（新problem:profile:v3:两小时）；content GET /profile/{userId}/solutions即时授权最多51条取50。前端三路聚合，不在problem实时拼content，不接受includePrivate。缓存方法放独立ProfileActivityCacheService，返回不含owner/solutions的ProfileActivityBaseVo，再组装新VO。EnableCache的name写problem:profile:v3（无尾冒号），CacheAspect自动加冒号生成problem:profile:v3:前缀；禁止同类self invocation和修改缓存共享实例。
* 新cache目录ID23=v3，24=content:comment:write:（60秒限频，Lua10次首次尝试/分钟，Redis失败503），25=final-rank:v1。旧v1/v2/solution缓存只受控SCAN+UNLINK清理，不flush，不删认证/积分/判题缓存。迁移不会自动操作Redis。
* inbox轮询全App一个timer，登录初始化完成才启动，可见每30秒，隐藏/deactivated/logout停止，焦点恢复立即请求，single-flight+sessionVersion/generation丢弃旧响应。刷新read/用户操作后主动刷新。不加SSE/BroadcastChannel消息内容。
* 纯文本emoji集合🙂 😄 😂 😅 😍 🤔 👍 👎 🎉 ❤️ 😢 😡；评论列表独立回复加载，固定两层，组件之间复用API/composable，不复制巨型页面。现有Avatar支持用户ID、主题/字号/抽屉层级保持。
* finalrank只在结束后结果区域加载；WAITING/BUILDING且没有结果可见每30秒查询，后台停止/焦点回来立即，READY停止。不抢答题空间，不破坏折叠侧栏/全屏编辑器/原题目新窗口设计。
* 统一增量构建：`JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk mvn -B -Dmaven.jar.forceCreation=true package`；frontend `pnpm run build`、`pnpm exec vitest run`。可先-pl相应模块-am test，不clean。
* 集成测试新增显式profile community-integration，需要单独测试数据库oj_it_user/oj_it_problem/oj_it_content、独立Rabbit vhost /oj-community-it、Redis测试prefix，默认不连接线上/开发真实业务数据库；参数缺失或目标不在allowlist直接失败。测试不得执行代码生成器/全库drop/flushdb，不能吞SQL/MQ失败作为测试通过。

## G0. 修订后的执行依赖

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

## G. 逐项任务

每项下面的15个字段均为本项执行要求。新增文件可补同名Test/DTO关联类型，但不可借此扩写任务范围。
### T01：用户侧社交数据基础

状态：已完成，禁止重复实施。以下15字段仅保留原验收档案；T01实际保留清单见A0/plan§0。

1. 任务名称：用户侧社交数据基础

2. 目标：建立关注、通知、扇出和积分消费收据的可迁移数据库基础；本项不实现API或消费业务。

3. 前置依赖：无。

4. 涉及模块：user-service、resources/sql、commons/common-api（新API状态异常基础）。

5. 需要阅读的现有文件：S/db_user.sql；S/migration/V20260924__role_display_special.sql；A/domain/entity/SysRole.java；U/mapper/SysUserMapper.java；U/domain/vo/UserProfileVo.java；common-api的MybatisConfig、RedisConfig。

6. 需要新增的文件：S/migration/V20261004_1__user_social.sql；U/domain/entity/UserFollow.java、UserNotification.java、NotificationFanoutJob.java、UserPointAwardReceipt.java；U/mapper/UserFollowMapper.java、UserNotificationMapper.java、NotificationFanoutJobMapper.java、UserPointAwardReceiptMapper.java；UX/UserFollowMapper.xml、UserNotificationMapper.xml、NotificationFanoutJobMapper.xml、UserPointAwardReceiptMapper.xml；UT/mapper/UserSocialMapperTest.java。 C/exception/ApiStatusException.java；AT/advice/ApiStatusExceptionAdviceTest.java。

7. 需要修改的文件：S/db_user.sql（仅新增结构，保留用户/角色种子数据）；user-service/pom.xml仅在测试需要时加已有版本test依赖。 A/advice/GlobalExceptionAdvice.java新增ApiStatusException的精确ResponseEntity handler复用safeBusinessMessage，已有AccessDenied handler补HTTP403状态，IllegalToken handler补401；未知异常隔离不变。

8. 数据库变化：严格B节T01四表，按IMPLEMENTATION_PLAN §3.2类型/索引。action必须是通知表字段；user外键不跨服务；复合主键不设任意单ID。

9. API变化：无外部API。

10. 具体实现要求：实体只数据库字段，不返回entity作为API；注册Mapper、XML namespace对应。提供显式唯一关系插入/删除、分页/COUNT、按接收者ID限制read SQL和job claim SQL的基础映射；XML参数用#{...}，不使用${...}排序字段。迁移可重跑，时间/状态默认遵照B；不要生成历史通知/历史积分。 ApiStatusException extends BusinessException，只允许400/401/403/404/409/429/503与固定安全业务消息；新接口使用它明确传输状态，不能用ResponseStatusException被通用handler吞成500。

11. 不应该修改的内容：不变更密码、JWT、Token、角色权限、不执行迁移到当前开发或生产DB、不写业务controller。

12. 边界条件：ID=0虚拟用户不产生关系；自关注CHECK及服务预留；nullable actor/目标字段能正确读；超过2^53-1的Long不能经Double/Number。

13. 并发/事务/幂等要求：复合PK/UNIQUE是数据库约束，不用先count后无约束insert；claim查询带FOR UPDATE SKIP LOCKED但本项不启动worker。

14. 测试要求：JUnit解析四个XML与绑定参数；检查read/read-all带recipient条件；后续T24真实MySQL迁移两次与唯一键验证。运行mvn -B -pl user-service -am test。 验证新异常HTTP与R.code一致且生产不返回SQL/堆栈，不改变旧普通BusinessException分支。

15. 验收标准：四表/索引与契约一一对应；Mapper编译/XML解析通过；无API/运行态改动；迁移不含DROP现有表/库。

### T02：题解互动、审核与事件基础及管理权限

状态：已完成的旧架构任务，禁止继续按本段建problem题解功能。以下是实际历史契约，迁移/清理只能执行R02-A～D；原权限与problem outbox保留。

1. 任务名称：题解互动、审核与事件基础及管理权限

2. 目标：补齐互动schema和真实管理权限，不能用按钮冒充页面路由。

3. 前置依赖：T01。

4. 涉及模块：problem-service、db_problem/db_user migrations。

5. 需要阅读的现有文件：S/db_problem.sql；S/db_user.sql；S/migration/V20261003_1__account_policies.sql；S/migration/V20260920__judge_admin_pages.sql；P/domain/entity/SolutionExplanation.java；P/mapper/SolutionExplanationMapper.java；U/service/impl/SysMenuServiceImpl.java；V/utils/menu/permissionDependencies.ts。

6. 需要新增的文件：S/migration/V20261004_2__solution_interactions.sql、V20261004_3__interaction_permissions.sql；P/domain/entity/SolutionLike.java、SolutionComment.java、CommentLike.java、SolutionModerationAction.java、ProblemEventOutbox.java；P/mapper/SolutionLikeMapper.java、SolutionCommentMapper.java、CommentLikeMapper.java、SolutionModerationActionMapper.java、ProblemEventOutboxMapper.java；PX对应五个XML；P/domain/enumeration/CommentState.java、SolutionModerationState.java；PT/mapper/InteractionSchemaTest.java。

7. 需要修改的文件：P/domain/entity/SolutionExplanation.java；S/db_problem.sql；S/db_user.sql；文档记录新增权限；不启动业务功能。

8. 数据库变化：B节五新表、solution增加七字段及索引；首次迁移标记历史有效公开first_published_at。权限新增comment:list/remove和judge:dispatch:retry，按router定位真实I节点与祖先，grant admin/super_admin。复用contest:rank并补所需路径；保留teacher已有grant，不全赋。

9. API变化：无新业务端点，本项只有模型/权限。

10. 具体实现要求：执行类型/约束契约；LIKE inactive保留first_notified，comment删除为state不加另一套del_flag。迁移新旧数据不修改内容/作者；所有新增B不占一级菜单、不创建缺component的I菜单；policy:comment:deny名称取消‘预留’但不授给任何默认角色。权限存在按perms复用，祖先关系递归到根仅用于迁移grant，不改变前端过滤算法。

11. 不应该修改的内容：不删除现有权限/路由，不改user安全和资料UI，不改变sys_menu菜单类型定义(I/M/B)，不得硬编码线上role_id/menu_id。

12. 边界条件：当前数据库已存在同perms时不产生重复节点；找不到指定父路由则迁移显式失败且指出缺少路由，不把B挂根凑数。PUBLIC=1，不能误用ContestAuth PUBLIC=0。

13. 并发/事务/幂等要求：迁移重跑不重置moderation/计数/first_published；DDL检查与DML分开，不能宣称DDL可事务回滚；权限关联INSERT IGNORE。

14. 测试要求：XML/实体映射测试；迁移静态/集成检查未授deny/student；大Long JSON测试留到VO任务；运行mvn -B -pl problem-service -am test。

15. 验收标准：新schema齐全；管理员/超管默认的真实路由链/按钮权限明确；无需student新默认权限；旧数据不重发通知。

### R02-A：题目域只读内容资格契约

1. 任务名称：题目域只读内容资格契约

2. 目标：为迁移后的题解提供最小 problem 元信息/资格检查，不读取题目正文或答案。

3. 前置依赖：T01、T02（均已完成，不重新实现）。

4. 涉及模块：common-api、problem-service、Gateway 测试；不引入 content 写接口。

5. 需要阅读的现有文件：P/controller/InternalController.java；P/service/impl/ProblemServiceImpl.java 的 doGetProblem/getDetailProblem/removeByIds；P/controller/ProblemController.java；P/domain/entity/Problem.java；C/enumeration/ProblemAuth.java；A/client/problem/client/ProblemInternalClient.java；A/config/FeignConfig.java、SecurityConfig.java；A/util/AuthUtil.java；gateway-server 的 AuthFilter/SecurityFilter。

6. 需要新增的文件：A/client/problem/client/ProblemContentReadClient.java；A/client/problem/domain/dto/ContentProblemReadRequest.java；A/client/problem/domain/vo/ContentProblemReadVo.java；P/service/ProblemContentReadService.java；PT/service/ProblemContentReadServiceTest.java；AT/client/ContentProblemReadSerializationTest.java。

7. 需要修改的文件：P/controller/InternalController.java 委派只读服务；P/mapper/ProblemMapper.java 和 PX/ProblemMapper.xml 增加包含逻辑删除行的批量元信息 SQL，明确不带答案字段。

8. 数据库变化：无 schema 改动，不连接 db_content。

9. API变化：仅 POST /internal/content-problems/read，request/response 精确按 plan§2.1；Feign value=problem-service，独立 contextId=problem-content-read，path=/internal，沿用 FeignDecoderConfig。不新增 browser 题目管理能力。

10. 具体实现要求：正ID1～100、去重后批查；从 AuthUtil 得身份与权限。每个请求ID都返回 exists/deleted/auth/publicReadable/privateWritable/title；missing 不缺项。PUBLIC=1，CONTEST=2；未删除PUBLIC允许公开/私有写；CONTEST只已有problem:problem:list者可创建私有题解，title也只对该权限或PUBLIC返回。没有权限标记来自body。查询数据库真实状态，不用缓存DetailProblem，不调用content/user/MQ。入口仅内部信任边界，Gateway必须拒绝所有外部internal变体。

11. 不应该修改的内容：不改比赛报名/密码/成员/时间/榜单规则，不把题解/评论写入problem，不复制认证逻辑，不添加body viewerId或允许浏览器绕Gateway直连。

12. 边界条件：missing/deleted/CONTEST/无用户身份时只提供PUBLIC元信息且privateWritable=false/虚拟0/超100/重复ID/超过2^53的ID；TITLE不因使用管理接口而无条件返回。

13. 并发/事务/幂等要求：只读无锁长事务；不调用其他服务，不在远程方法里修改任何业务数据；稳定地恢复请求顺序。

14. 测试要求：Java11 mvn -B -pl problem-service -am test；测试公开、赛题管理/普通用户、删除缺失、边界100/101、伪造caller字段拒绝、Long字符串、内部路由拦截。追踪现有ProblemController读/管理回归；实际Gateway HTTP测试没有环境则报告未执行，不能用mock假称通过。

15. 验收标准：契约所有字段及资格矩阵通过；不泄露内容/答案；新增服务没有反向content依赖，Gateway不开放内部API。

### R02-B：T02 forward correction：content schema、模型与安全数据搬迁工具

1. 任务名称：T02 forward correction：content schema、模型与安全数据搬迁工具

2. 目标：为旧题解和T02互动数据建立db_content归属，准备可重复验证的维护迁移；保留原problem outbox。

3. 前置依赖：T01、T02（均已完成）。

4. 涉及模块：content-service、problem-service基础实体/测试拆分、resources/sql、迁移入口；本Task不迁业务Controller也不部署。

5. 需要阅读的现有文件：S/migration/V20261004_1__user_social.sql、V20261004_2__solution_interactions.sql、V20261004_3__interaction_permissions.sql全文；S/db_problem.sql/db_content.sql/db_user.sql；P/domain/entity路径按plan§0.1的T02文件与旧SolutionExplanationContent；PX五个新XML；PT/mapper/InteractionSchemaTest.java；S/migration/apply.sh、scripts/start-dev-infra.sh、deploy/server/deploy.sh；content-service现有实体/Mapper注册及pom.xml。

6. 需要新增的文件：S/migration/V20261004_4__content_solution_schema.sql、V20261004_5__content_solution_transfer.sql；S/migration/manifest.tsv；scripts/migrate-solution-domain.sh；D/domain/entity/SolutionExplanation.java、SolutionExplanationContent.java、SolutionLike.java、SolutionComment.java、CommentLike.java、SolutionModerationAction.java、ContentEventOutbox.java、SolutionProblemReference.java；D/domain/enumeration/CommentState.java、SolutionModerationState.java；D/mapper 对应实体Mapper；DX 对应XML；CT/mapper/InteractionSchemaTest.java；CT/integration/SolutionDomainMigrationIntegrationTest.java。

7. 需要修改的文件：S/db_content.sql 新装增加两旧表、四互动审计表、content outbox、reference和迁移状态；S/db_problem.sql 新装移出solution块但保留problem_event_outbox。本项先在D复制并验证模型/枚举/Mapper，P的旧副本暂留到R02-C引用清零后再删除；尤其P SolutionExplanation仍import SolutionModerationState，不能在本项先删此枚举。PT InteractionSchemaTest本项保留，content版增加迁入表验证；R02-C再拆为P仅outbox、D完整interaction，不能丢测试。旧历史迁移三份字节不改。三处迁移入口改用显式manifest/phase，禁止server遍历V*.sql自动copy；不自动执行本Task新migration到真实库。本项schema/model中间版本不得独立部署，须等R02-C/D成组切换。

8. 数据库变化：严格plan§3.3所有列/unique/check/RESTRICT，正文保留TEXT和字节；移除题解→problem跨域FK。新content outbox和reference/state按确定字段建表。problem outbox原表/实体/Mapper留P。_5只显式维护copy，不是常规启动迁移；原尚未创建的contest迁移编号留_6。新装跳过仅适用于旧problem题解的历史_2，保留_1/_3；旧装若缺T02字段，冻结_2只在维护源库规范化阶段运行一次并验证backfill marker。

9. API变化：无产品API；工具固定 --phase inspect|prepare|copy|verify|cutover，phase及LEGACY/FRESH语义按plan§13。执行真实库阶段必须另获授权；默认inspect只读，不从脚本隐式升级。

10. 具体实现要求：源/目标按同一MySQL实例两个schema处理；实例不同显式停止，不自行换迁移方案。显式列搬solution/正文/likes/comments/commentlikes/audit及仅七类community outbox；ID/timestamps/flags/counts/version/firstPublished/body不重算，events不再造。分批keyset最多500；PK冲突仅全字段相同可跳，不同立即失败。verify行数、逐行有序摘要、所有孤儿/关系/计数，保存hash与state；发现既有脏数据报告，不擅自修计数/丢行。迁移state SHA保存manifest，CUTOVER后永远不允许从旧源重导。source community SENDING在停止worker且lease失效后转PENDING，保留ID/attempts，SENT/FAILED原样；problem relay严格不再认community分支。source UNKNOWN type中止检查，不硬删。迁入实体映射已存在七字段；仅Long version作为并发版，移除其updateTime @Version，显式SQL CAS。无外部用户FK。初始化seed搬迁如存在也保留ID，但生产不执行初始化SQL。

11. 不应该修改的内容：不回滚T01、不删或重命名历史_1/_2/_3、不删真实db_problem旧表、不启worker、不改其他数据库/公告FAQ结构/MinIO、不同步双写，不把problem EventOutbox整体搬走。

12. 边界条件：旧T02已执行/未执行/半执行、fresh空库、NULL firstPublished、emoji/大正文、deleted题关联、19位ID、源目标不一致、迁移中断重跑、target有新写入、CUTOVER旧源陈旧；源FK若仍存在不复制跨域约束。

13. 并发/事务/幂等要求：维护copy必须停旧题解写入和所有相关producer/relay；本Task工具检查维护确认和state。DDL非事务，copy按父表先子表后/小事务提交；失败不能声称全库回滚。行级比较含NULL与精确字节，不能用INSERT IGNORE吞不同内容。copy可恢复，CUTOVER CAS固定单向，旧源只读备份不自动清理。

14. 测试要求：Java11 mvn -B -pl content-service,problem-service -am test；独立白名单oj_it_content/oj_it_problem/oj_it_user MySQL8测试 fresh、legacy两版本、反复copy/verify、不同目标冲突、停在第2批恢复、CUTOVER再copy拒绝、FK/check/unique、字节和first_notified/SENT事件保持。没有测试MySQL则不执行真实库替代，报告未满足集成验收。检查三历史SHA不变；shell bash -n，manifest覆盖20261003，不能恢复危险测试。

15. 验收标准：实体/XML均可编译；迁移可检查、复制、验证、封闭重复导入；三历史迁移不变；题解data/schema目标content、problem outbox仍problem；真实库无操作，所需集成验证均有事实证据。

### R02-C：现有题解完整服务迁移、资格投影和双路径兼容

1. 任务名称：现有题解完整服务迁移、资格投影和双路径兼容

2. 目标：将当前已可用的题解业务完整搬到content，实现资格边界、移除problem的同步content依赖风险；暂不实现T08审核/通知和T09+互动。

3. 前置依赖：R02-A、R02-B。

4. 涉及模块：content-service、problem-service、common-api只读契约、Gateway、oj-vue solution API/config与相关测试。

5. 需要阅读的现有文件：P/controller/SolutionController.java；P/service/SolutionExplanationService.java、SolutionExplanationContentService.java及impl；P/domain/entity/两个Solution实体、DTO DetailSolutionDto/PagedSolution、VO SolutionVo/DetailSolutionVo；P/mapper两个SolutionMapper和PX两个XML；P/service/impl/ProblemServiceImpl.java删除检查；PT/service/impl/SolutionPolicyTest.java；V/api/solution/index.ts及调用组件；gateway-server/src/main/resources/application.yml、filter/AuthFilter/SecurityFilter；resources/nacos及部署配置同步脚本（先搜索实际入口，不虚构已存在content yaml）；A/config/SecurityConfig/FeignConfig.java；D/ContentApplication.java。

6. 需要新增的文件：D/controller/SolutionController.java；D/service/SolutionExplanationService.java、SolutionExplanationContentService.java；D/service/impl 对应实现；D/domain/dto/DetailSolutionDto.java、PagedSolution.java；D/domain/vo/SolutionVo.java、DetailSolutionVo.java；D/service/SolutionAccessService.java、SolutionProblemReferenceService.java；D/job/SolutionProblemReferenceRefreshJob.java；D/config/SolutionDomainProperties.java；CT/service/impl/SolutionPolicyTest.java、SolutionDomainReadTest.java；gateway-server/src/test/java/com/anishan/gateway/SolutionRouteCompatibilityTest.java；V/api/solution/solutionDomain.test.ts；如没有content-service配置文件，新建resources/config/content-service.yaml并纳入实际Nacos同步入口。

7. 需要修改的文件：移走P旧SolutionController/Services/DTO/VO/Mapper/XML及实体，ProfileSolutionVo相关临时保留到R02-D。D/ContentApplication.java保证@EnableScheduling已注册（已有则不重复），reference job按开关与CUTOVER gate启用。D迁入代码删除MPJ对Problem实体的JOIN与Unused UserClient；P ProblemServiceImpl仅移除本地solution count及其import，保留全部submit/records/activity guards和逻辑删除。V/api/solution统一content-api；Gateway/实际Nacos配置加order=-10 legacy route；D已具备R02-B模型，本项删除旧P四互动实体/两个枚举/四Mapper/XML副本（先确认无调用）；留P ProblemEventOutbox/Mapper/XML。PT InteractionSchemaTest改为仅P outbox映射，其余验证已经在CT，不能删覆盖。PT SolutionPolicyTest迁CT测试，断言保留。

8. 数据库变化：读写仅db_content两旧题解表、reference。projection初始化来自R02-B维护阶段；不在启动时读旧源或迁移数据。题目逻辑删除不触碰content数据；不再定义跨服务FK。

9. API变化：普通/后台CRUD/detail/list/recent服务内路径及R保持既有；canonical content-api，legacy仅problem-api/solution由Gateway内部代理。同源Cookie/Access认证不改，/solution浏览器路由和solution-edit菜单/permission字符串原样。comment端点尚未实现，不能新增别名。

10. 具体实现要求：所有content题解写入口在migration marker未CUTOVER时503，worker同gate；严格plan§2.1候选SQL+fresh batch+最多3轮修复分页；count按本地projection快照最终一致，正文逐请求资格即时验证。Reference worker每秒一批<=100、按checked_at/problem_id keyset循环，最近5分钟已查的周期可跳；失败不推进成功checked_at、不忙等、5→300秒退避，仅本实例上一批完毕才下一批，默认开关false直到cutover确认。请求issuedAt作为requested_at fencing，旧响应不覆盖新；worker服务身份只请求元信息，非PUBLIC title不写入投影。新/编辑事务前fresh，事务内重核version/owner/private/删除状态，network不在锁中。先迁现有AccountPolicy与add拒绝ID，adminadd同样拒绝ID，白名单字段更新、禁止改problemId/author。详情/recent停止共享成品cache，DTO不暴露entity/内部错误。删除改逻辑删，正文保留；旧admin无原因删除本迁移版本先返回400，T08接入有原因流程前不能物理删除。守护所有流到PUBLIC都需要fresh资格。author历史不查不到题就丢正文，title安全fallback。只有CAPABILITY迁移不改RBAC树。

11. 不应该修改的内容：不实现点赞/评论/审核事件/通知worker、不改题目正文/比赛资格/判题/榜单，不移动前端后台菜单或换权限字符串，不改Markdown CSS/编辑器/菜单动画/Token。Profile拆分由R02-D，不用P→D RPC暂时补。

12. 边界条件：已登录普通/作者/管理、有deny权限、旧ID更新、非PUBLIC新建公开拒绝400、nonPUBLIC普通私有创建拒绝403、missing/deleted404、已有孤儿作者自读、problem停机503、公开转CONTEST并发、projection陈旧/恢复PUBLIC、legacy带body/query与401/403一致。

13. 并发/事务/幂等要求：所有题解写content本库TX，Long version显式CAS/行锁，仅updateTime作时间；资格检查时点遵plan§2.1不声称跨域原子。局部projection UPSERT用requested_at条件；慢请求不能回写过时许可，读资格仍fresh。projection刷新不生成published通知，不清firstPublished。任何过期source DB表只备份不参与运行态。

14. 测试要求：Java11 mvn -B -pl content-service,problem-service,gateway-server -am test；pnpm build及solutionDomain vitest；普通/后台CRUD、policydeny、adminadd安全、旧路由/new路由同结果、过滤不绕条件、19位ID、分页repair3次、停机failclosed/无长事务网络、删除题时剩余完整性guards回归。测试并发响应投影fencing、fresh title隐藏；T24运行真实三服务HTTP验证。本Task最终移除P引用要全局rg但允许R02-D剩余Profile相关，须记录清单。

15. 验收标准：现有题解功能归content并能从两种网关前缀访问，旧页面路由/菜单权限不变；P不再有题解CRUD与SQL JOIN；无缓存授权绕过、公开赛题泄漏和后台物理删除；只留下待R02-D明确清除的profile查询。

### R02-D：个人主页题解拆域、缓存清单与遗留清除

1. 任务名称：个人主页题解拆域、缓存清单与遗留清除

2. 目标：彻底移除problem profile对题解实体/表的查询，组合前端数据并清理所有运行态题解旧归属。

3. 前置依赖：R02-C。

4. 涉及模块：content-service、problem-service、oj-vue、content CacheCatalog。

5. 需要阅读的现有文件：P/controller/ProfileController.java；P/service/impl/ProfileServiceImpl.java；P/mapper/ProfileMapper.java、PX/ProfileMapper.xml；P/domain/vo/ProfileActivityVo/ProfileSolutionVo；PT/service/impl/ProfileServiceImplTest.java、mapper/ProfileMapperTest.java；V/api/profile/index.ts、ProfileActivityPanel.vue、views/profile/Profile.vue；D已迁SolutionAccessService/Service；A/aspect/CacheAspect.java；D/service/CacheCatalog.java。

6. 需要新增的文件：D/controller/SolutionProfileController.java；D/service/SolutionProfileQueryService.java；D/domain/vo/ProfileSolutionVo.java、ProfileSolutionsVo.java；P/service/ProfileActivityCacheService.java；P/domain/vo/ProfileActivityBaseVo.java；CT/service/SolutionProfilePrivacyTest.java；PT/service/impl/ProfilePrivacyCacheTest.java；V/api/profile/profileAggregation.test.ts。

7. 需要修改的文件：P ProfileService/Impl/Mapper/XML/ActivityVo删除solutions字段/selectSolutions/MAX_SOLUTIONS并保留其他统计/热力图；删除P ProfileSolutionVo；D查询采用已迁solution本库SQL与fresh资格；V/api/profile三路组合，局部题解错误状态；CacheCatalog稳定ID23标profilev3、旧v1/v2/solution标停用清理，未来ID24 contentcomment/25 rank预留但不提前实现业务。已有Profile测试保留并改断言。

8. 数据库变化：无新业务表，用D solution/reference；不再引用db_problem.solution*；维护脚本清旧Redis是明确授权的运维步骤而非启动动作。

9. API变化：新增content-api GET /profile/{userId}/solutions，字段和50/51上限按plan§2.3；problem现有GET /profile/{userId}（网关/problem-api/profile/{userId}）保持，不新增/activity后缀，但去掉solutions与solutionsTruncated；user profile不改。前端ProfileData依旧供原组件。query不能接受includePrivate。

10. 具体实现要求：只AuthUtil.id==target才给本人私有/受限项；访客仅effectivePUBLIC，不因管理权限自动放开普通profile。新content profile不整体cache，实时batchgate；删题历史本人可展示title安全fallback；所有其他页保留UserSummary机制。P cache独立bean纯BaseVo，EnableCache name=problem:profile:v3（无尾冒号），两小时，不能同类自调用，组装新对象不污染共享实例。前端并行三个请求，返回generation/sessionVersion防串账号；局部content503不能让其他活动丢失或跳login，也不能将错误显示为正常空集合。全局rg确认P已无Solution实体/SQL/service/import，历史migration/测试fixture中旧名仅带归档说明；删除旧Cacheable路径，不迁移私有缓存到新prefix。

11. 不应该修改的内容：不改个人主页tab/布局/安全设置/热力图性能、不新建第二个路由、不改Auth restore、不调用content于P、不FLUSHDB或删认证/积分/判题缓存，不自动删除旧库备份。

12. 边界条件：本人/访客/管理员看他人仍公共语义；authoronly、刚审核隐藏、19位ID、51上限、内容服务离线、快速换账号/路由、旧前端字段缺失容错、hot cache旧profile实例。

13. 并发/事务/幂等要求：activity缓存仅可重建只读投影；content列表资格始终fresh，本域状态查询每次；前端不持久化他人私有数据。网络均无数据库行锁，隔离请求失败。

14. 测试要求：Java11 mvn -B -pl content-service,problem-service -am test；pnpm build+vitest profileAggregation；缓存内容无solution/owner，缓存命中仍看不到已隐藏题解、50/51、private隔离/旧response丢弃、旧bundle normalization空字段不报错；全局搜索P solution引用、API前缀、Redis前缀，每个遗留必须注明历史用途。

15. 验收标准：problem不读content表也不同步调用content；profile题解均由content提供并即时授权；前端原界面不变且局部失败可见；缓存类别/键/清理列表完整，P剩余唯一solution字符串是明确历史文件/fixtures/legacy route相关。

### T03：共享事件契约与消息拓扑（不含业务relay）

1. 任务名称：共享事件契约与消息拓扑（不含业务relay）

2. 目标：定义跨服务固定event协议，供两个域各自outbox及user消费者使用，不把业务设施放在错误域。

3. 前置依赖：T01、T02（已完成）。

4. 涉及模块：common-api、user-service消息拓扑配置；不修改题解/提交业务。

5. 需要阅读的现有文件：A/config/RabbitConfig.java；resources/config/shared-rabbit.yaml；J/mq/JudgeListener.java；A/client/judgeserver/domain/JudgeInfo.java；T01异常；本文件D、plan§5消息格式全文。

6. 需要新增的文件：A/event/CommunityEvent.java、CommunityEventType.java、PointAwardEvent.java；A/event/FixedEventRoutes.java（技术常量白名单，不注册业务Service）；U/config/CommunityTopologyConfig.java；AT/event/CommunityEventSerializationTest.java；UT/config/CommunityTopologyTest.java。

7. 需要修改的文件：仅现有配置注册必需的拓扑配置与test依赖，不覆盖全局RabbitTemplate/listener设置。

8. 数据库变化：无；不创建第三张公共outbox。

9. API变化：仅Java契约，community IDs与points金额均string，具体payload按D，不对外接受任意exchange/type。

10. 具体实现要求：七类community+POINTS_AWARDED的DTO、schemaVersion/dedupe/16KiB/纯文本reason验证。user-service声明社区和积分durable exchange/queue/DLX/DLQ/binding，consumer后续T05添加。Judge DLQ由T03-P声明，旧judge队列arguments不改。messageId=eventId，不信任任意Rabbit类型header。公共常量只描述固定路由，不注入某个域业务，不允许content调用problem relay或反之。

11. 不应该修改的内容：不移动ProblemEventOutbox、不实现题解/通知业务、不改Global Redis/JWT/判题并发，不注册第二个全局Rabbit converter。

12. 边界条件：schema未知、缺字段、错误recipient数量、超16KiB、19位ID、金额精确小数；不含源码/标题/正文/Token/email。

13. 并发/事务/幂等要求：本项不持业务TX或发事件；各域后续独立使用固定路由，拓扑声明幂等且不能修改现存队列属性触发PRECONDITION_FAILED。

14. 测试要求：Java11 mvn -B -pl user-service -am test；DTO roundtrip、JSON字符串/非法类型/大小检测、所有routing/binding断言；真实MQ声明兼容在T24，不假称本项mock为真实broker测试。

15. 验收标准：协议与D一致，两个域引用公共小契约而非彼此实体/Service；topology不改已有判题consumer。

### T03-C：content community outbox relay与清理

1. 任务名称：content community outbox relay与清理

2. 目标：在内容域可靠发送七类社区消息，后续题解/互动操作只写本库outbox。

3. 前置依赖：R02-B、T03。

4. 涉及模块：content-service，common-api仅事件类型引用。

5. 需要阅读的现有文件：D/ContentApplication.java；D/mapper/ContentEventOutboxMapper.java、DX/ContentEventOutboxMapper.xml；T03协议与拓扑；A/config/RabbitConfig.java；R02-B迁移state；本文件D及plan§5。

6. 需要新增的文件：D/config/CommunityMessagingConfig.java、CommunityWorkerProperties.java；D/service/ContentEventOutboxService.java、impl/ContentEventOutboxServiceImpl.java；D/job/ContentOutboxRelay.java、ContentOutboxCleanupJob.java；CT/service/ContentEventOutboxServiceTest.java、job/ContentOutboxRelayTest.java。

7. 需要修改的文件：D/ContentApplication.java @EnableScheduling（若已有则不重复）；DX outbox XML实现claim/fencing；resources/config/content-service.yaml新开关及其实际Nacos同步入口，无硬编码凭据。

8. 数据库变化：仅content_event_outbox；新业务写入必须加入调用者content本库TX，不能REQUIRES_NEW脱离业务。

9. API变化：Java内部recordCommunity API；仅允许七类，不接受任意exchange/payload。replay管理接口T06接入。

10. 具体实现要求：专用communityRabbitTemplate共享现有connection和JSON converter，correlated confirm+mandatory+return；消息persistent/messageId=eventId；dedupe重复仅同内容接受，不能覆盖。claim与send分TX，严格D pool4/queue20/1s/20条/lease60s/confirm5s/backoff/max20；owner且未过期lease才更新状态。没有broker业务可提交outbox，worker退避。schema/type校验拒绝points/judge，本域job必须受enabled与迁移CUTOVER gate控制，测试默认disabled，部署准备配置但不得在未切换期间默认抢消费。SENT社区30天每小时最多500清理，FAILED不删；日志不输出payload。

11. 不应该修改的内容：不读取db_problem、不调用problem relay、不修改judge模板或全局listener、不发送points/dispatch，不自动执行数据copy。

12. 边界条件：claim后崩溃/发送后更新DB失败/nack/return相邻/timeout/租约接管/UNKNOWN类型/超payload/迁移尚未CUTOVER。

13. 并发/事务/幂等要求：业务+community outbox同TX；worker claim提交后send。所有状态CAS带owner/有效lease；上批完成再claim，不能租约被队列耗尽；at-least-once预期，user唯一键兜底。

14. 测试要求：Java11 mvn -B -pl content-service -am test；ack/nack/return/timeout/DBsent更新失败/leaseexpired/重复dedupe/differentpayload拒绝/越域类型拒绝/CUTOVERgate；T24真实Rabbit验证；新增序列化验证无token正文Long精度。

15. 验收标准：内容域PENDING可恢复发送，越域事件不派发，不在行锁等待MQ，worker有限且保留原ID。

### T03-P：problem积分与判题派发outbox relay

1. 任务名称：problem积分与判题派发outbox relay

2. 目标：复用T02原problem outbox，仅处理POINTS_AWARDED/JUDGE_DISPATCH，保持比赛判题域独立。

3. 前置依赖：T02（已完成）、T03。

4. 涉及模块：problem-service、RabbitMQ判题DLQ配置；不依赖content业务或R02-C/D。

5. 需要阅读的现有文件：P/domain/entity/ProblemEventOutbox.java；P/mapper/ProblemEventOutboxMapper.java、PX XML；A/config/RabbitConfig.java；T03 PointAwardEvent/FixedEventRoutes；J/mq/JudgeListener.java；J/service/impl/JudgeServiceImpl.java；A/client/judgeserver/domain/JudgeInfo.java；P/ProblemApplication.java。

6. 需要新增的文件：P/config/ProblemMessagingConfig.java、ProblemOutboxProperties.java；P/service/ProblemEventOutboxService.java、impl/ProblemEventOutboxServiceImpl.java；P/job/ProblemOutboxRelay.java、ProblemOutboxCleanupJob.java；PT/service/ProblemEventOutboxServiceTest.java、job/ProblemOutboxRelayTest.java。

7. 需要修改的文件：P/ProblemApplication.java调度配置；PX/ProblemEventOutboxMapper.xml claim仅固定两类及owner CAS；resources/config/problem-service.yaml加入outbox开关（默认false到配套版本发布）、batch/lease等；判题DLX/queue/binding声明配置。

8. 数据库变化：留用db_problem.problem_event_outbox，绝不迁整表；源community历史备份不被新worker/清理claim。无新表。

9. API变化：Java内部recordPointAward/recordJudgeDispatch，不提供recordCommunity；dispatch-retry由T18-A实现，不提供任意replay。

10. 具体实现要求：固定points拓扑由T03声明，判题branch原JudgeInfo JSON<=600KiB、原judge-exchange/judge-info/dedupe原submitId；不把新envelope塞judge队列。使用本域独立确认模板，不覆盖原回调；D完整relay pool4/queue20/1s/20条/lease60s/5s/max20并fencing。声明新direct judge-exchange.dlx.v1、durable judge-info.dlq.v1、bind judge-info.dead.v1，不能改旧judge-info-queue arguments；policy运维T24。SENTpoints30天/dispatch24小时，每小时500，FAILED与历史community不自动删。这里只支持branch设施，T18-A/T19才接业务；没有broker事务可入账等待，不伪造终态。

11. 不应该修改的内容：不改judge listener并发/确认/语言/timeout/Redis锁TTL、不接题解事件、不需要content Feign、不复用空JudgeResultListener作为新consumer。

12. 边界条件：send成功DB失败重复、confirm与return相邻、timeout租约接管、提交长期QUEUE等待修复、旧community row存在不能误发/误删、未来unknown类型报警不发送。

13. 并发/事务/幂等要求：派发/积分入账必须随problem原业务TX；网络等待锁外；相同dedupe不同payload失败并告警，所有更新owner/有效lease。publisher不宣称exactly-once，消费者收据/判题result_applied兜底。

14. 测试要求：Java11 mvn -B -pl problem-service -am test；mock ack/nack/return/timeout/DB失败/lease重试/类型隔离/保持JudgeInfo序列化/600KiB边界/清理过滤；T24真实DLQ policy与duplicate交付；不运行现有危险生成器。

15. 验收标准：原outbox可靠服务于judge/points，保留旧判题协议与队列，content迁移不让比赛流程反向依赖content。

### T04：关注后端与安全用户摘要

1. 任务名称：关注后端与安全用户摘要

2. 目标：提供关注/取消、计数、关注/粉丝分页，并为互动与榜单提供安全批量身份信息。

3. 前置依赖：T01。

4. 涉及模块：user-service、common-api。

5. 需要阅读的现有文件：U/controller/UserController.java、InternalController.java；U/service/SysUserService.java、impl/SysUserServiceImpl.java；A/util/AuthUtil.java；A/client/user/client/UserInternalClient.java、UserClient.java；U/domain/vo/UserProfileVo.java；C/domain/dto/PagedQuery.java。

6. 需要新增的文件：U/controller/FollowController.java；U/service/FollowService.java；U/service/impl/FollowServiceImpl.java；U/domain/dto/FollowPageQuery.java；U/domain/vo/FollowSummaryVo.java；A/client/user/domain/vo/UserSummaryVo.java；A/client/user/domain/dto/UserSummaryRequest.java；UT/service/FollowServiceTest.java。

7. 需要修改的文件：U/controller/InternalController.java增加POST /internal/user-summaries；U/service/SysUserService.java、impl/SysUserServiceImpl.java；U/mapper/SysUserMapper.java和UX/SysUserMapper.xml安全摘要批查；A/client/user/client/UserInternalClient.java加同名契约；T01的UserFollowMapper/XML完善分页。

8. 数据库变化：user_follow是唯一事实，无计数缓存/用户计数字段。

9. API变化：实现plan§8.1全部follow端点以及internal批量摘要，精确字段遵C；body不能带followerId；列表R<PagedResult<UserSummaryVo>>。

10. 具体实现要求：Service获取AuthUtil ID、验证target>0/exists/normal/notdeleted且不self；重复关注/取消返回真实关系summary，关系创建/删除有事务。摘要投影仅四字段，最多100ID去重/批查。列表按时间/ID降序并过滤删除用户；公开资料不扩大到游客；system root0不参加社交。

11. 不应该修改的内容：不使用UserClient.listUser、不增加user:user:list给普通用户、不改用户ID规则/邮箱/Token/RBAC祖先校验。

12. 边界条件：非存在/删除目标404，自关注400；已取消DELETE幂等；摘要缺失返回缺项，前端不得用本人资料补他人。分页size0或>50拒绝400。

13. 并发/事务/幂等要求：复合PK重复插入捕获仅DuplicateKey，返回已存在；不存在先SELECT不替代唯一键。COUNT索引查询，取消不会负数；外部摘要调用方不可加入持锁TX。

14. 测试要求：自关注、0、无目标、删除/封禁目标、重复PUT/DELETE、两个并发关注；安全摘要无email/auths/remark、100上限、19位ID；SQL参数绑定与分页稳定。

15. 验收标准：普通账号无需新permission即可正确关注；不可冒用别人ID；批量摘要供评论/排行一次查，不N+1。

### T05：通知消费、可恢复粉丝扇出与积分幂等消费

1. 任务名称：通知消费、可恢复粉丝扇出与积分幂等消费

2. 目标：将已提交事件可靠转换为用户侧消息或积分，无跨库同步回调。

3. 前置依赖：T03、T03-C、T03-P、T04。

4. 涉及模块：user-service、common-api；不修改业务producer。

5. 需要阅读的现有文件：T03事件类/拓扑、T03-C/T03-P双域producer格式；U/UserApplication.java；resources/config/shared-rabbit.yaml；U/mapper/SysUserMapper.java和其addPoints SQL；T01 mapper/entities；U/service/impl/SysUserServiceImpl.java；A/util/AuthUtil.java。

6. 需要新增的文件：U/config/CommunityConsumerConfig.java、NotificationWorkerProperties.java；U/mq/CommunityNotificationListener.java、PointAwardListener.java；U/service/NotificationDeliveryService.java、NotificationFanoutService.java、PointAwardApplicationService.java；U/job/NotificationFanoutJobRunner.java；UT/service/NotificationDeliveryTest.java、NotificationFanoutTest.java、PointAwardApplicationTest.java。

7. 需要修改的文件：T01 Notification/Fanout/Receipt mapper/XML；resources/config/user-service.yaml增加本功能worker开关与batch，不能写生产密码。

8. 数据库变化：notification UNIQUE接收者+业务key，fanout PK事件ID，receipt PK用户题目；point累加与receipt同用户库事务。

9. API变化：无Browser API，community来源content、points来源problem，consumer不向任一服务同步反查正文/标题/可见性。

10. 具体实现要求：专用listener factory AUTO+retry3+DLQ；Listener委派独立@Transactional Service并在返回后ACK，不能同类self调用或catch失败后正常返回。七类型确定收件人：self过滤/集合去重，事件格式最大值校验。SOLUTION_PUBLISHED只注册job。worker 200/keyset批，current follow且created<=occurred才送，每批notif+cursor同TX。当前正常真实账号接收消息；直接业务摘要只本库。积分INSERT receipt新行才原addPoints SQL，USER消失记录消费完成/跳过日志，不制造无限毒消息。

11. 不应该修改的内容：不调用UserClient管理接口、不同步回查题目或内容服务、不创建跨服务FK/新微服务、不把POINTS_AWARDED加入用户通知列表；不改变固定2分奖励配置。

12. 边界条件：actor=recipient点赞/自己评论不通知；管理自己操作仍通知；同reply两收件人相同只一条；fanout中途unfollow/晚follow语义按D；旧通知不因重投变unread；未知schema/type拒绝DLQ。

13. 并发/事务/幂等要求：通知插入重复只ignore指定unique冲突，不覆盖已读；fanout lease+owner+cursor，与DB事务；receipt+sys_user分数一事务，不在Redis里计积分，不写db_problem。

14. 测试要求：重复事件10次/不同eventId同dedupe一次消息；已读后重复消息保持read_at；batch边界200/201，崩溃前后cursor、unfollow/latefollow；reply去重/self；积分重复/回滚仅一次；malformed消息到DLQ测试留T24。

15. 验收标准：producer端故障/重复投递不会重复消息或积分；每次消费者工作有上限；无需反向同步服务调用。

### T06：本人消息API、已读语义与后台重放

1. 任务名称：本人消息API、已读语义与后台重放

2. 目标：提供安全收件箱数据与读状态，落地保留周期和明确故障恢复。

3. 前置依赖：T05、T03-C、R02-C。

4. 涉及模块：user-service、content-service（社区replay）。

5. 需要阅读的现有文件：U/controller/UserController.java、U/service/impl/SysUserServiceImpl.java；A/util/AuthUtil.java、AccountPolicy.java；C/domain/R.java；T03-C content outbox与T05 delivery/fanout；A/advice/GlobalExceptionAdvice.java。

6. 需要新增的文件：U/controller/NotificationController.java；U/service/NotificationQueryService.java；U/domain/dto/NotificationPageQuery.java、NotificationReadAllRequest.java；U/domain/vo/NotificationVo.java、UnreadCountVo.java；U/job/NotificationRetentionJob.java；D/controller/CommunityEventAdminController.java；UT/service/NotificationQueryTest.java；CT/service/CommunityReplayTest.java。

7. 需要修改的文件：T01 notification/fanout Mapper/XML；T03-C content outbox service补失败重放方法。

8. 数据库变化：无新表。read_at只从NULL到当前时间；保留180天通知、30天DONE job，batch500；删除不重复造历史事件。

9. API变化：GET /notification/page、/unread-count；PUT /{id}/read、/read-all（throughId）；POST /notification/admin/fanout/{eventId}/replay与/solution/admin/events/{eventId}/replay。严格plan§8.1/8.2。

10. 具体实现要求：所有普通SQL带recipient=AuthUtil，计数indexed COUNT；排序notification_id DESC，UNREAD筛选。actor摘要本地批查，无正文快照。reason/action仅合法管理type接收者返回。两个replay分别权限user:user:edit/solution:edit，FAILED才改PENDING，原ID/dedupe不改，fanout保留cursor；社区接口拒绝points/judge分支。

11. 不应该修改的内容：不把NoticeController迁到这里、不让管理员读任意用户收件箱、不提供全库标读接口、不把内部event payload/last_error进VO。

12. 边界条件：他人ID和不存在ID404；已有read重复成功；throughId之后并发收到消息仍未读；清理后的分页空集合法；极大ID string；未知消息类型安全兜底，不渲染HTML原因。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；read update WHERE read_at IS NULL AND recipient限定；read-all有throughId上界；replay条件status=FAILED CAS；清理有限批次，不持大事务锁全表。

14. 测试要求：越权、重复已读、read-all race、size限制、管理原因隔离、wrong replay type/非FAILED409、保留周期边界/19位ID序列化。

15. 验收标准：消息只有本人可读/修改；重放不会重建ID或重发已读状态；公告接口/游客访问不受影响。

### T07：个人主页关注入口与用户列表

1. 任务名称：个人主页关注入口与用户列表

2. 目标：在他人主页加入小型关注入口/统计，不破坏一体化主页和设置。

3. 前置依赖：T04、R02-D。

4. 涉及模块：oj-vue。

5. 需要阅读的现有文件：V/views/profile/Profile.vue；V/api/profile/index.ts；V/components/Avatar/Avatar.vue；V/components/ProfileActivityPanel/ProfileActivityPanel.vue；V/api/common.ts；V/stores/useUserStore.ts；V/utils/http.ts。

6. 需要新增的文件：V/api/follow/index.ts；V/components/FollowButton/FollowButton.vue；V/components/FollowListDialog/FollowListDialog.vue；V/composables/social/useFollow.ts；V/composables/social/useFollow.test.ts。

7. 需要修改的文件：V/views/profile/Profile.vue侧栏计数/按钮与dialog，必要的profile类型补充但不改账号安全交互。

8. 数据库变化：无。

9. API变化：只调用T04端点，用户列表仍以page/size，ID string。

10. 具体实现要求：本人隐藏关注按钮，ID0/无效对象隐藏；他人按钮单flight、固定最小宽度/disabled加载不抖。统计点击轻量dialog分页，复用Avatar和摘要角色标签；不新增主menu。route.params.id变更重置state/request generation，旧响应不能覆盖新用户；关系API失败显示当前面板提示且保留旧状态。

11. 不应该修改的内容：不新建第二个账号设置路由、不改变profile纵向tab/左右高度/热力图缓存、不修改登录/菜单/头像全局CSS。

12. 边界条件：从A主页切B快速返回、自关注、follow操作失败、空followers、已注销摘要、大昵称、窄屏/暗色/键盘dialog。

13. 并发/事务/幂等要求：按钮pending覆盖PUT/DELETE直到finally，请求结果未成功前不永久乐观改count；route/sessionVersion generation清除旧结果；组件卸载取消订阅。

14. 测试要求：Vitest mock API测试重复点击仅一请求、ID切换丢旧响应、失败保留状态、本人不出现；手动宽屏/移动/暗色验证，pnpm run build与vitest run。

15. 验收标准：主页小范围增强，关注/取消/分页正确；无重复轮询、无Token存储、本人安全tab保持原状。

### T08：题解可见性、缓存安全、CRUD及管理处理

1. 任务名称：题解可见性、缓存安全、CRUD及管理处理

2. 目标：统一访问边界，支持强制仅作者可见与处理原因，修复已有缓存/新增覆盖漏洞。

3. 前置依赖：R02-D、T03-C、T04。

4. 涉及模块：content-service；common-api仅安全摘要契约。

5. 需要阅读的现有文件：D/controller/SolutionController.java；D/service/impl/SolutionExplanationServiceImpl.java；DX/SolutionExplanationMapper.xml；D/service/SolutionProfileQueryService.java；D/domain/dto/DetailSolutionDto.java；D/domain/vo/SolutionVo.java、DetailSolutionVo.java；A/config/MvcConfig.java、aspect/CacheAspect.java；content-service/src/main/java/com/anishan/content/service/CacheCatalog.java；R02-A ProblemContentReadClient与R02-C reference/access设施。

6. 需要新增的文件：D/service/SolutionModerationService.java；D/domain/dto/SolutionModerationRequest.java、ModerationDeleteRequest.java、ModerationPageQuery.java；D/domain/vo/ModerationActionVo.java；CT/service/impl/SolutionVisibilityTest.java、SolutionModerationTest.java。

7. 需要修改的文件：既有SolutionController/Service接口/实现/Mapper/XML/DTO/VO；SolutionProfileQueryService沿用R02-D无成品cache边界；T02 moderation mapper/XML；普通admin delete链。 保留R02-C/D测试，不能删。

8. 数据库变化：使用R02-B已迁到db_content的moderation/del_flag/firstPublished/version；逻辑删除题解，保留评论/审计。不重算历史私有/公开。

9. API变化：保留既有CRUD返回；增加POST /solution/admin/{id}/moderation、/delete，GET /solution/{id}/moderation、/admin/{id}/moderation-history。detail增加互动状态（likedByMe可先false，T09补齐）。无原因旧admin DELETE400。

10. 具体实现要求：移除普通get的@Cacheable，recent成品cache停用；访问先沿用R02-C的即时批量题目资格与本域SQL/锁，全部筛选在可见性括号外。普通userId筛选只筛公开或自己的项，不能清空参数；关联PUBLIC题才能外公开/互动。非PUBLIC题却请求private=false（后台同样）返回400；有效可见性VO按C节计算，前端只用effectiveVisibility标识公开/本人可见。作者受限可看未删除本人，不能自己解除。管理员ONLY/RESTORE reason/audit+outbox同TX；创建POST(admin也一样)拒绝已有solutionId，编辑ID/author/problem归属不变，字段显式赋值。首次有效公开NULL→time+事件一次。profile由R02-D三路组合，content题解查询每次访问即时校验，审核限制立即生效；不得恢复problem的solutions字段或两小时成品cache。未知不可见404，不缓存个人授权结果。

11. 不应该修改的内容：不改MarkdownPreview/编辑器CSS、不改变题目/竞赛访问逻辑，不将private作者设置混同管理员审核，不在普通接口因为有管理权限自动查他人私有。

12. 边界条件：作者先访问私有再其他用户请求、recent旧公开转restricted、profile两小时缓存中含旧题解、管理员add仅权限覆盖已有ID、题目软删除/CONTEST、批量删除混入他人ID全量失败。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；solution行锁 + version递增，审核/首公开/删除同事务outbox；重复相同状态操作不造新audit；author编辑不能绕moderation，afterCommit不是可靠发消息唯一手段。

14. 测试要求：至少覆盖上述缓存越权/CRUD/筛选分组/作者限制/新增ID拒绝/管理reason与通知/firstPublished重试、profile基础cache不包含solutions、public大ID；运行Java11 mvn -B -pl content-service -am test。

15. 验收标准：任何缓存命中不能跳过可见性；强制限制立即不再公开显示；作者可收到/查看处理信息；普通题解/比赛题泄露路径堵住。

### T09：题解点赞与首次通知

1. 任务名称：题解点赞与首次通知

2. 目标：实现可重复设置的点赞状态、正确计数和一次性作者通知。

3. 前置依赖：T08、T05。

4. 涉及模块：content-service。

5. 需要阅读的现有文件：T08 SolutionAccessService/SolutionController；T02 SolutionLikeMapper/XML；T03-C content outbox；A/util/AuthUtil.java；D/domain/vo/DetailSolutionVo.java、SolutionVo.java；D/service/impl/SolutionExplanationServiceImpl.java。

6. 需要新增的文件：D/service/SolutionLikeService.java；D/service/impl/SolutionLikeServiceImpl.java；D/domain/vo/LikeResultVo.java；CT/service/SolutionLikeServiceTest.java。

7. 需要修改的文件：D/controller/SolutionController.java增加PUT/DELETE /{id}/like；T02 like mapper/XML；SolutionExplanationServiceImpl detail/list/recent批量viewer-liked装配；VO types。

8. 数据库变化：使用solution_like.active/first_notified及solution.like_count；关系取消不物理删除。

9. API变化：PUT/DELETE /solution/{id}/like返回liked/likeCount；不可见资源取消自己的旧关系likeCount=null；无需额外普通角色permission。

10. 具体实现要求：先身份，锁外fresh problem资格，再锁solution核本域即时公开状态；PUT只能有效公开，DELETE允许取消自己已存关系。只有active改变才改count。第一次active=true且actor!=author才插SOLUTION_LIKED outbox并置first_notified；self首次也置flag但不发消息，禁止后来改author导致回补消息。detail/list/recent最多50/20IDs批查liked，不逐题查询、不缓存viewerstate。

11. 不应该修改的内容：不修改题解内容/private/moderation、不加Redis点赞计数，不给管理员替用户操作接口，不将点赞改成toggle或POST每次加1。

12. 边界条件：重复PUT、已取消DELETE、取消后重赞、自赞、目标变private/deleted、隐藏对象likeCount不泄露、无旧关系DELETE返回false/null且不生成新关系。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；solution→like行锁；count SQL加减且不能负数；unique主键兜底；资源count/active/flag/outbox同TX。读响应取事务内最终计数。

14. 测试要求：并发100次PUT仍count1；取消不负数；重复事件/重赞通知一次；self无通知；隐藏后只能取消旧关系；19位ID与计数JSON测试。

15. 验收标准：关系和数据库计数一致，首次通知最多一次；list/detail个性化liked没有授权缓存污染。

### T10：一级评论创建、分页和文本校验

1. 任务名称：一级评论创建、分页和文本校验

2. 目标：实现独立可测试的一级评论功能以及后续回复可复用的输入/限频/幂等设施。

3. 前置依赖：T08、T04、T05。

4. 涉及模块：content-service（含本域CacheCatalog）。

5. 需要阅读的现有文件：T08 SolutionAccessService；D/controller/SolutionController.java；T02 comment Mapper/XML；A/util/AccountPolicy.java；C/domain/dto/PagedQuery.java；D/service/impl/SolutionExplanationServiceImpl.java；content-service/src/main/java/com/anishan/content/service/CacheCatalog.java。

6. 需要新增的文件：D/controller/CommentController.java（后续T11～13扩展）；D/service/CommentService.java、CommentCreationService.java、CommentContentValidator.java、CommentRateLimiter.java；D/service/impl/CommentServiceImpl.java；D/domain/dto/CommentCreateRequest.java、CommentPageQuery.java；D/domain/vo/CommentVo.java；CT/service/CommentCreationTest.java、CommentContentValidatorTest.java、CommentRateLimiterTest.java。

7. 需要修改的文件：SolutionController增加GET/POST /{id}/comments（委派CommentService）；SolutionCommentMapper/XML根分页及请求ID查找；CacheCatalog加ID24 comment限频。

8. 数据库变化：根root_id/parent_id/reply_to=null；client_request_id UUID全用户唯一；comment_count只计活跃评论。

9. API变化：GET/POST /solution/{id}/comments，body仅content/clientRequestId；CommentVo字段按C/plan8.2。一级页包含被删根tombstone（作为回复容器），不得只WHERE state=VISIBLE筛掉回复入口。

10. 具体实现要求：建立validator D节全部normalize/长度/控制字符规则；Service actor=AuthUtil.require且无comment deny。外层先查已有requestId比较规范化content/solution/parent，已成功返回原记录（仍遵访问资格，不返回现在不可见目标）；新创建校验有效公开+commentsOpen。通过Redis Lua10/min后进入独立TX创建bean，锁solution再次核本域状态（远程资格在锁外取得，时点遵plan§2.1），写root/count/outbox。唯一冲突必须回滚后外层查原对象，比较不同则409。roots分页desc，deleted content/null原因不返回，批量usersummary+liked组装。

11. 不应该修改的内容：不加入Markdown解析/HTML过滤渲染/上传组件，不实现无限tree、评论编辑、前端页面，不改现有GlobalExceptionAdvice生产隔离。

12. 边界条件：纯空白、emoji+组合符、中文codepoints、2000/2001、8KiB、NUL、非法surrogate、<script>字面内容；重复请求现在关评仍可返回先前已成功记录但不能泄漏被隐藏目标。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；solution锁串行关评/审核/新增，count/outbox同TX；限频首次请求attempt消耗、成功幂等retry不消耗；Redis不可用503、无身份401，closure409。

14. 测试要求：精确边界validator；同UUID同content返回1条/不同409；关评check二次核验竞态、restricted/其他作者不能读、公开tombstone无正文原因、批量昵称不是N+1、Redis计数TTL脚本unit。

15. 验收标准：一级分页/创建可靠，所有输入纯文本，禁止角色即使直接HTTP也不能创建；断网重试不重复评论/通知。

### T11：两层回复与独立分页

1. 任务名称：两层回复与独立分页

2. 目标：扩展一级评论为固定两层展示的数据模型，不增加递归树复杂度。

3. 前置依赖：T10。

4. 涉及模块：content-service。

5. 需要阅读的现有文件：T10 CommentController/Service/CreationService/Validator；SolutionCommentMapper/XML；T05消息收件人规则；T08 access。

6. 需要新增的文件：D/domain/dto/CommentReplyRequest.java（字段仅content/clientRequestId，可复用父DTO）；CT/service/CommentReplyServiceTest.java。

7. 需要修改的文件：D/controller/CommentController.java；CommentService/Impl、CommentCreationService；SolutionCommentMapper/XML回复分页/根parent锁查询；CommentVo补replyTo/root/parent/replyCount装配。

8. 数据库变化：用已有root/parent/reply_to列；根reply_count+1与solution.comment_count+1同创建TX；无新表。

9. API变化：POST /comment/{parentId}/replies；GET /comment/{rootId}/replies，默认20最大50按created_at/comment_id升序；不能将parent为回复当成root分页。

10. 具体实现要求：从parent读取并锁定同solution，parent VISIBLE且root真实根；parent自己是root则rootId=parentId，否则沿其已存rootId取根，只读取固定两行不递归。reply_to从parent.userId，不接受客户端author/root。允许parent为任意可见回复，仍一个root列表。发一条COMMENT_REPLIED事件，recipient={parent作者,题解作者}-actor，去重；不能再发SOLUTION_COMMENTED。复用T10validator/limit/idempotency。

11. 不应该修改的内容：不返回嵌套children无限JSON，不新增回复深度表/路径树，不改root分页顺序、Markdown或用户头像。

12. 边界条件：跨solution伪造、不存在parent/root、parentdeleted、root已删但parent可见仍允许、parent=root锁只一次、回复自己无自身通知，root列表找错id400/404。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；solution→root→parent固定锁序；root已删仍保留reply_count并可增加；clientReq全用户唯一防root与reply复用UUID不同目标；记录/counts/outbox一个TX。

14. 测试要求：root回复、回复子项、depth100仍两层查询、删除根后回复visible child、已删除parent失败、wrong root/solution拒绝、两通知收件人相同只一次。

15. 验收标准：不会递归查询/构建无限树；用户可回复可见回复而布局固定两层；通知无重复。

### T12：评论删除、管理原因与关闭评论区

1. 任务名称：评论删除、管理原因与关闭评论区

2. 目标：实现作者删除、管理删除审计及关评的清晰并发语义。

3. 前置依赖：T11。

4. 涉及模块：content-service。

5. 需要阅读的现有文件：T10/T11 comment service/controller；T08 solution moderation/access；T02 moderation mapper；A/util/AccountPolicy.java；D/controller/SolutionController.java。

6. 需要新增的文件：D/service/CommentModerationService.java；D/domain/dto/CommentsStateRequest.java、AdminCommentPageQuery.java；D/domain/vo/AdminCommentVo.java；CT/service/CommentModerationServiceTest.java、CommentsStateTest.java。

7. 需要修改的文件：CommentController/CommentServiceImpl；SolutionController；SolutionCommentMapper/XML与SolutionModerationActionMapper/XML；T08 moderation history查询包含comment动作。

8. 数据库变化：评论state AUTHOR_DELETED/ADMIN_DELETED与deleted_by/time；只首次VISIBLE→deleted更新counts；审计追加不可被author改；不级联回复。

9. API变化：DELETE /comment/{id}只本人；POST /comment/admin/{id}/delete必填reason；GET /comment/admin/page需comment:list；GET /comment/{id}/moderation只原作者；PUT /solution/{id}/comments-state作者/管理；管理history按plan8.2。

10. 具体实现要求：普通删除只user_id相等，denycomment仍可删本人；题解作者不得删别人。管理员remove权限和必填纯文本reason，record审计+COMMENT_MODERATED同TX。公共正文/reason均null，后台page单独投影包括保留内容/处理信息，分页最大50。关评solution锁下仅更新comments_open；作者需无solution deny，管理edit继续既有deny规则；close/open重复不造audit通知。

11. 不应该修改的内容：不删除根的已有回复、不禁关闭区的阅读/点赞、不把‘作者’视为管理员、不允许评论编辑/恢复；不放开普通接口的管理正文。

12. 边界条件：已删除重复操作成功不计数；第三方删除404/403；root/child删除counts正确；reason空/超长/HTML仅字面文本；comment.parent rootdeleted不会丢入口。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；锁solution→root→target；delete/counts/audit/outbox一个事务；close与post共solution锁，先close提交新post失败，先post提交则保留。

14. 测试要求：owner/admin/thirdparty权限矩阵；删除根回复保留；重复delete计数/消息1次；关闭race并发两个连接真实MySQL T24；普通响应无reason/正文，作者reason限定本人。

15. 验收标准：关评无新增而原内容可看；所有管理删除作者可获原因；公共read不泄露被删内容。

### T13：评论点赞、状态与作者通知

1. 任务名称：评论点赞、状态与作者通知

2. 目标：为已存在可见评论提供可靠点赞，不把点赞与发表评论限制混为一谈。

3. 前置依赖：T12。

4. 涉及模块：content-service。

5. 需要阅读的现有文件：T09 SolutionLikeService；T12 CommentModerationService；T02 CommentLikeMapper/XML；T10 CommentService查询装配；T03-C content outbox。

6. 需要新增的文件：D/service/CommentLikeService.java；D/service/impl/CommentLikeServiceImpl.java；CT/service/CommentLikeServiceTest.java。

7. 需要修改的文件：CommentController PUT/DELETE /{id}/like；CommentLikeMapper/XML；CommentServiceImpl分页批量liked信息。

8. 数据库变化：comment_like active/first_notified，comment.like_count原子变化，无新表/Redis缓存。

9. API变化：PUT/DELETE /comment/{id}/like返回LikeResultVo，own取消不可见目标返回null count。

10. 具体实现要求：复用LikeResultVo和公共正确规则，但目标可见检查/根锁是评论自己的服务，不抽一个万能动态SQL点赞框架。PUT仅VISIBLE+有效公开，commentsOpen=false仍可以点赞；self可计数不发消息。第一次他人like产COMMENT_LIKED；后续重赞不发。同页一次批查用户like关系。

11. 不应该修改的内容：不新增like普通用户permission、不把comment deny扩大到取消/阅读、不加异步Redis计数、不修改评论内容。

12. 边界条件：deleted comment newlike拒绝、隐藏solution不能newlike、旧ownlike能取消、rootdeleted childVISIBLE可like、关闭区可like。

13. 并发/事务/幂等要求：远程problem/UserSummary查询均在行锁TX外，公共互动先fresh资格，跨域竞态时点遵plan§2.1；solution→root→target→like固定锁，counts/state/flag/outbox同TX；删除与点赞竞争serialized；清除likecount不做删评副作用（历史关系保留但公共隐藏）。

14. 测试要求：重复100次like/cancel，删除竞争不出现负数，self无消息、取消重赞一通知，closed区与policy deny行为、批量liked无N+1。

15. 验收标准：评论数量与状态可靠、无重复作者消息、关闭区不误禁点赞。

### T14：前台题解互动与纯文本评论组件

1. 任务名称：前台题解互动与纯文本评论组件

2. 目标：接通点赞/评论/回复/删除/表情，实现高质量可复用两层UI。

3. 前置依赖：T09、T13。

4. 涉及模块：oj-vue。

5. 需要阅读的现有文件：V/views/solutions/solution/solution.vue；V/views/solutions/component/SolutionsComponent/SolutionsComponent.vue；V/components/SolutionCard/SolutionCard.vue；V/api/solution/index.ts；V/components/Avatar/Avatar.vue；V/utils/http.ts、authUtil.ts；V/components/MarkdownPreview.vue（只了解边界，不修改）。

6. 需要新增的文件：V/api/comment/index.ts；V/components/SolutionInteractions/SolutionInteractions.vue；V/components/CommentThread/CommentThread.vue、CommentItem.vue、CommentComposer.vue、EmojiPicker.vue；V/composables/social/useCommentThread.ts、useLikeAction.ts；V/composables/social/useCommentThread.test.ts、useLikeAction.test.ts；V/utils/commentText.ts、commentText.test.ts。

7. 需要修改的文件：solution.vue放互动+CommentThread；api/solution/index.ts补字段/like/comments-state/moderation查询；SolutionCard视情况仅展示计数，不引入每卡轮询。

8. 数据库变化：无。

9. API变化：严格T09～13端点；root/replies各page state，clientRequestId由composer生成一次，失败重试复用到成功/用户编辑后新UUID。

10. 具体实现要求：不把评论做Markdown。文本插值/pre-wrap/overflow-wrap；表情popover固定12项插入textarea光标，按现有ElementPlus popover，不另写定位zIndex。根列表及每根回复独立懒加载，回复button显示回复对象/取消、两层缩进；deleted根tombstone保留回复按钮容器，只对visible parent可reply。关评已有列表/likes保持、composer替为简短关闭提示；owner关闭按钮权限用服务端owner+已有hasPerm且后台同样校验。将管理原因放作者可见小面板，不露在公共页。loading按钮尺寸稳定、single-flight、列表请求generation/组件dispose防串题。

11. 不应该修改的内容：不改Markdown公式/Mermaid/代码条、profile路由、主题全局、OJ布局、avatar fallback，不重写http401/Token。

12. 边界条件：长纯文本/URL/script/图片语法必须只文字；窄屏、暗色、缩放、2000字符表情、已有评论删除、reply parentdeleted、关评后composer在请求中；保存失败不得清输入。

13. 并发/事务/幂等要求：按资源请求锁；route solutionId切换旧response不写新state；空状态与错误状态分离，HTTP失败恢复按钮；重复click不发新UUID，不全页闪刷新。

14. 测试要求：Vitest请求state/UUID重用/generation/replies分页/文本normalize/likepending；浏览器DOM验证无img/script/v-html，视口390/768/1440及dark，pnpm build/test。

15. 验收标准：用户可完成所有互动/查看本人处理原因；前台无后台原始错误；评论不会损坏题解Markdown或布局。

### T15：后台题解审核与评论管理抽屉

1. 任务名称：后台题解审核与评论管理抽屉

2. 目标：将管理员审核/评论删除接到现有题解管理，保留清晰权限与操作原因。

3. 前置依赖：T12、T08。

4. 涉及模块：oj-vue。

5. 需要阅读的现有文件：V/views/backend/problem-module/solution-edit/SolutionEdit.vue；V/views/solutions/edit-solution/EditSolution.vue；V/api/solution/index.ts；V/utils/hasAuth/index.ts、authUtil.ts；V/components/MarkdownPreview.vue；V/views/backend/problem-module/component/ProblemModuleShell.vue。

6. 需要新增的文件：V/components/SolutionModeration/SolutionModerationDialog.vue、SolutionModerationHistory.vue；V/components/CommentManagement/CommentManagementDrawer.vue；V/api/comment/admin.ts；V/components/SolutionModeration/moderationForm.test.ts。

7. 需要修改的文件：SolutionEdit.vue action入口、管理详情drawer；EditSolution.vue不再以private编辑代替审核，管理add rejects ID；api/solution/index.ts管理moderation/delete；删除API避免simpleCRUD false仍报成功；补正题目名称查询只在真实problem:problem:list权限时管理API。

8. 数据库变化：无新增；权限来自T02迁移。

9. API变化：管理员详情GET /solution/admin/{id}而非跳普通/private详情；moderation/delete/history/comment-admin明确各权限。旧无原因DELETE不调用。

10. 具体实现要求：管理‘查看’独立drawer使用getSolutionAdmin，禁止公共route query参数偷偷绕权限；评论抽屉由comment:list控制，删除由comment:remove控制，审核/恢复由solution:edit，题解delete需remove。reason必填与长度反馈，提交时single-flight；成功局部刷新当前行/页、失败保留reason。history按同题解筛；删除自己的管理评论仍明确选择普通/管理语义。管理private选项仅作者设置，不展示成审核权限。

11. 不应该修改的内容：不新增无路由菜单、不按admin字符串过滤按钮、不重写backend菜单/动画/全局zindex、不将后台可见性赋给所有普通solution接口。

12. 边界条件：只有solution:add不能修改已有ID；只有list没有edit按钮；评论权限叶子缺祖先迁移/权限组件应提示；大Markdown管理预览限制在drawer容器原样式、dark/手机。

13. 并发/事务/幂等要求：每dialog pending与请求generation，关闭后不写旧状态；删除false/冲突不能显示成功；各组件复用API与评论文本显示，不复制整页编辑器。

14. 测试要求：权限组合UI测试/手动账号矩阵，reason验证、repeatclick、403保留表单、删除失败不报成功；pnpm build/test，SQL真实菜单授予验收T24。

15. 验收标准：管理员/超管可按迁移权限找到功能；普通用户不能进入管理；原因送达作者、后台不破坏Markdown布局。

### T16：个人收件箱、徽标与页面可见轮询

1. 任务名称：个人收件箱、徽标与页面可见轮询

2. 目标：新增个人消息入口和页面，与平台公告完全分离，控制后台请求。

3. 前置依赖：T06。

4. 涉及模块：oj-vue。

5. 需要阅读的现有文件：V/router/index.ts；V/components/AccountMenu/AccountMenu.vue；V/stores/useToken.ts、useUserStore.ts；V/utils/authSession.ts、authBridge.ts；V/layout/component/FloatingBall/index.vue（现有30秒可见轮询参考）；V/App.vue；V/api/notice/index.ts；V/views/notification/Notification.vue。

6. 需要新增的文件：V/api/notification/index.ts；V/stores/useInboxStore.ts；V/composables/social/useInboxPolling.ts、useInboxPolling.test.ts；V/views/inbox/Inbox.vue；V/components/NotificationList/NotificationItem.vue；V/utils/notificationDisplay.ts、notificationDisplay.test.ts。

7. 需要修改的文件：router/index.ts在已登录container加静态/inbox route（不加constMenu主导航）；AccountMenu加‘消息’项与小未读badge；App.vue只挂一次poller；logout/user clear时清InboxStore（用现有状态watch，不改Token设计）。

8. 数据库变化：无。

9. API变化：T06 page/unread/read/read-all。route=/inbox/name=inbox仅requireAuth；/notification依旧公告。

10. 具体实现要求：所有type用固定普通用户文案/目标路径，理由纯文本；点击评论消息可带定位commentId但访问仍由普通solution API检查。read-all使用当前加载中已观察最大notificationId，禁止用随机巨大throughId。poller全App单实例、setTimeout在await后安排下次，visible30秒、hidden/logout/deactivated停止，focus/visibility回来立刻一次，single-flight；sessionVersion+generation防logout旧响应重写。只内存存inbox状态，无localStorage/Token/channel内容；用户标读后主动刷新counter。

11. 不应该修改的内容：不改公告API/权限/原主menu、不新增SSE或WebSocket、不广播消息/Token、不让多个AccountMenu各自开timer、不显示技术实现文案。

12. 边界条件：初始unknown不先闪‘没登录’，silent refresh后启动；logout/login换账号不留上一位收件箱；后台tab0请求；目标隐藏/删除在消息页面提示不可访问，reason仍仅本人；网络离线安全退避不无限toast。

13. 并发/事务/幂等要求：共享inboxRequestPromise防并发timer/focus（不是改写认证refreshPromise），卸载/用户变更清timer和DOMlistener，返回code failure不重复刷新401（现有HTTP负责）；read不影响并发新通知。

14. 测试要求：fake timers验证30秒、hidden停止、focus即刻singleflight、logout旧响应失效、read-all throughId、新消息不误读、文案无HTML；pnpm build+vitest。

15. 验收标准：个人通知可读/标读/导航；公告保持游客可见；后台不持续轮询，账号切换无私人数据串号。

### T17：比赛成绩schema、历史来源与题目快照基础

1. 任务名称：比赛成绩schema、历史来源与题目快照基础

2. 目标：为可靠最终榜单建立最小持久来源/版本结构，并保留不可重建的历史成绩。

3. 前置依赖：T03-P。

4. 涉及模块：problem-service、resources/sql。

5. 需要阅读的现有文件：S/db_problem.sql比赛/成绩/log表；S/migration/V20260912__judge_pipeline.sql；P/domain/entity/Contest.java、ContestRecords.java、SubmitLog.java、JudgeCaseLog.java；P/mapper/ContestMapper.java、RecordsMapper.java；PX/RecordsMapper.xml、ContestMapper.xml、ProblemListMapper.xml；P/service/impl/ContestServiceImpl.java、ProblemListServiceImpl.java。

6. 需要新增的文件：S/migration/V20261004_6__contest_final_rank.sql；P/domain/entity/ContestProblemSnapshot.java、ContestAttempt.java、ContestRankSnapshot.java、ContestRankEntry.java；P/domain/enumeration/ContestAttemptState.java、ContestRankState.java；P/mapper/ContestProblemSnapshotMapper.java、ContestAttemptMapper.java、ContestRankSnapshotMapper.java、ContestRankEntryMapper.java；PX对应四个XML；P/service/ContestProblemSnapshotService.java；PT/mapper/ContestRankSchemaTest.java。

7. 需要修改的文件：既有Contest/ContestRecords/SubmitLog字段；其Mapper/XML锁与新字段；JudgeCaseLogMapper/XML唯一键支持；S/db_problem.sql修复错误contest_records索引目标并同步新表；S迁移只新增不DROP原表。

8. 数据库变化：执行B/E所有赛事字段、四表、唯一键和索引；旧终态applied标记要维护窗口preflight，旧业务数据评分不改；duplicatecase先备份再确定性清理；历史scoring_version=1，新无数据=2。

9. API变化：本项无Browser API，snapshot Java内部create/read/reset，无controller直写表。

10. 具体实现要求：SnapshotService用直接Mapper而非注入ContestService/RecordsService造成循环；在外层TX contest锁→list锁→problem IDs升序锁批读/插入、记录snapshotAt。order按原problem_order/ID稳定排序，写snapshot的order统一1..N防旧重复order冲突；max_score非负、题型/题目未删除校验。对已有正式attempt不能reset，legacy查询不得用当前题单重算原成绩。Mapper有同contest不limit全提交的聚合基础；Long ID对象不可toDouble。

11. 不应该修改的内容：不改判题语言/参数/秒限、不把legacy猜为CURRENT，不执行新迁移到线上/本地业务库、不重写所有records数据。

12. 边界条件：空题单新正式提交拒绝、空比赛终榜可全部0分；旧重复case只有真实重复清理；题单重复顺序稳定标准化；多contest共list快照互不污染；schedule为空preflight指出。

13. 并发/事务/幂等要求：snapshot须同contest→list→problem IDs升序锁事务与list写入口一致；重复snapshot create返回已存不重做；新entities先不启动榜单job。case唯一键迁移不可假装DDL可回滚。

14. 测试要求：所有XML解析、新字段映射、snapshot稳定排序/重复创建/旧评分不变/历史modeflag测试；T24两个MySQL连接及迁移重复执行验证。

15. 验收标准：新比赛与历史数据来源明确，所有成绩/冻结/最终version结构可编译；无历史静默重算。

### T18-A：统一受理与持久OJ派发

1. 任务名称：统一受理与持久OJ派发

2. 目标：建立正式提交的唯一受理与序号分配路径，受理与消息持久化原子关联。

3. 前置依赖：T17。

4. 涉及模块：problem-service、common-api契约无语义扩大；resources配置。

5. 需要阅读的现有文件：P/controller/JudgeController.java、SubmitLogController.java；P/service/impl/JudgeServiceImpl.java、SubmitLogServiceImpl.java；A/util/RedisJudgeSubmissionLock.java；A/client/judgeserver/domain/JudgeInfo.java；T03-P relay、T17 snapshot/attempt；P/service/impl/ContestServiceImpl.java；T01新增的C/exception/ApiStatusException.java。

6. 需要新增的文件：P/service/ContestSubmissionCoordinator.java、JudgeSubmissionService.java；P/domain/ContestSubmissionContext.java；P/config/ContestClockConfig.java；PT/service/ContestSubmissionBoundaryTest.java、JudgeDispatchOutboxTest.java。

7. 需要修改的文件：JudgeServiceImpl.judgeOj委派queued事务/outbox；SubmitLogService createQueued配合TX；Contest/Attempt Mapper/XML锁与seq；SubmitLogController新dispatch-retry。非OJ统一受理由T19接入。

8. 数据库变化：next_attempt_seq、snapshot/attempt、submit_log与JUDGE_DISPATCH outbox同事务；user_submit仍原表；不新增用户表、不改Redis锁TTL。

9. API变化：原/judge、/contest/submit、join/problems/status、/record保持路径；新增POST /log/admin/{submitId}/dispatch-retry需problem:judge:dispatch:retry；跨越时间等失败HTTP409安全消息。

10. 具体实现要求：Coordinator直接Mapper/Clock/snapshot/outbox，不依赖ContestService/RecordsService以免循环。每次正式活动受理锁contest：CONTEST校验Clock start<=now<end；HOMEWORK从start起到用户有效supplement.deadline（存在且>=end）否则end，严格now<deadline。二者均已joined未handin且snapshot题成员，分配seq。OJ在锁外验证语言/code/tryAcquire12秒名额，随后TX写submit_log/attempt/outbox；无contest也log+outbox同TX但无attempt。成功返回submitId/QUEUE，不再Feign直接投递；MQ故障保留已受理，worker重试。失败rollback释放原token。dispatch-retry只原FAILED未terminal任务，权限字符串严格控制，不生成新submitId。提交context携带冻结max_score。T18-B/18-C前不得对外发布成绩链。

11. 不应该修改的内容：不改sandbox并发、Redis12秒TTL、现有判题参数/语言、Token、原接口路径；本项不修改全部活动管理/题单写入口。

12. 边界条件：start等于now允许/end等于now拒绝；非member/handed-in/wrong problem拒绝；正交练习无contest；受理commit前后崩溃由outbox兜底；HOMEWORK补交真实deadline在统一coordinator处理，不用单isContestEnable盖掉。

13. 并发/事务/幂等要求：contest→list(初建snapshot)→log→attempt；seq锁内增；outbox同TX、网络在锁外；释放原Redis token不会误释放新任务；snapshot creation复用T17。

14. 测试要求：Clock边界/无成员/已交卷/不在snapshot；create log后TX失败outbox/attempt全回滚；brokerdown仍QUEUE且有PENDINGdispatch；重复replay/完成AC不可重派；19位submitID。

15. 验收标准：每条正式OJ都有可恢复派发记录，每赛事attempt都有稳定seq；本项只完成受理核，不提前发布未完成的边界修复。

### T18-B：交卷、赛程与参赛成员原子边界

1. 任务名称：交卷、赛程与参赛成员原子边界

2. 目标：让加入、交卷、退回、调整成员、赛程与统一受理使用同一比赛锁。

3. 前置依赖：T18-A。

4. 涉及模块：problem-service、common-api契约无语义扩大；resources配置。

5. 需要阅读的现有文件：P/controller/ContestController.java、JudgeController.java、RecordController.java；P/service/impl/ContestServiceImpl.java、UserContestServiceImpl.java、SupplementContestServiceImpl.java；P/domain/entity/UserSubmit.java、UserContestRelation.java；T18-A Coordinator/Clock；T17 snapshot。

6. 需要新增的文件：P/service/ContestParticipationService.java、ContestScheduleService.java；PT/service/ContestParticipationRaceTest.java、ContestScheduleTest.java。

7. 需要修改的文件：ContestController全部手写Db加人/交卷/退回委派Service；ContestServiceImpl.join/status/update/remove/addUser/addUserByClass/lateSubmission；JudgeController与RecordController不再重复不一致时间check。

8. 数据库变化：沿用user_contest/user_submit/supplement_contest；所有CONTEST操作在contest行锁内，已结束roster不变；无正式attempt可reset snapshot并改list，已有attempt禁止改赛程/list。

9. API变化：原contest API保持；重复交卷幂等；无效边界409。HOMEWORK补交仍原路径与规则，不开放作业finalrank。

10. 具体实现要求：Clock/资格判断委派小型无循环helper，Controller不可直接Db.save绕锁。join/adminadd在CONTEST end后拒绝；删除已有attempt用户拒绝且不删成绩，end前零attempt可删；end后退回交卷拒绝；实际受理与handin锁序决定可否提交。已有READY赛程/list不可改；无attempt改list原子reset snapshot，新check不依赖cached ContestVo。补交只作业、已joined、deadline>=end，补交受理统一Clock start<=now<deadline（有有效补交则用其deadline，否则end），joined/handin照常检查，不能让JudgeService旧isContestEnable再次按end拒绝有效补交。

11. 不应该修改的内容：不删除历史参赛人/成绩、不改变教师现有角色/权限、不让admin加人自动拥有题目/榜单权限、不大改contest list UI。

12. 边界条件：两个并发handin、一边handin一边judge受理、成员删除与受理同时、到end同毫秒、补交deadline边界、无contest资源404、旧password/whiteList规则保留。

13. 并发/事务/幂等要求：所有路径contest锁，不只Controller加@Transactional；事务内无Feign班级查人，classUser IDs先锁外获取再TX校验/加入；source roster与最终生成不会反序变化。

14. 测试要求：真实MySQL双连接handin/accept与remove/accept、repeatjoin、endclock、已READY改赛程、classbulk加入有界与原权限、补交真实deadline验证。

15. 验收标准：管理和用户入口都不能绕过提交/交卷/结束边界；成绩数据不会因成员管理被无声删除。

### T18-C：冻结题目读取与题单/评分内容修改隔离

1. 任务名称：冻结题目读取与题单/评分内容修改隔离

2. 目标：让冻结比赛与可编辑题单真正隔离，并禁止运行中评分输入被修改。

3. 前置依赖：T18-B。

4. 涉及模块：problem-service、common-api契约无语义扩大；resources配置。

5. 需要阅读的现有文件：P/controller/ListController.java、ProblemController.java、ContestController.java；P/service/impl/ProblemListServiceImpl.java、ProblemProblemListServiceImpl.java、ProblemServiceImpl.java、OjProblemCaseServiceImpl.java；PX/ProblemListMapper.xml、RecordsMapper.xml；T17 snapshot、T18-A coordinator；J/judge/impl/BuildJudgeCaseImpl.java（读取DB input/output路径）。

6. 需要新增的文件：P/service/ContestMutationGuard.java；PT/service/ContestProblemSnapshotIsolationTest.java、ContestMutationGuardTest.java。

7. 需要修改的文件：ListController.updateProblem直接写改为事务Service；题单加/删/分值/排序/删除统一锁list；ProblemProblemListServiceImpl.delByListIds补.eq/in作用域；ContestService.listProblemInContest与现有statistics读取snapshot；ProblemService/OjProblemCase评分相关写入加guard。

8. 数据库变化：snapshot成员/分值/顺序是冻结比赛唯一题单源；原题单仍可用于未冻结比赛。snapshot外键/历史成绩不随物理删除级联。

9. API变化：保持既有GET /contest/problems/{id}及所有题单/题目/case修改API；比赛列表/统计读取冻结数据，无新Browser API。

10. 具体实现要求：所有list mutation先锁对应problem_list行；bulk多list按listId升序锁，不反向锁contest。snapshot按contest→list读取，冻结后显示/成员/满分/统计不随list变化。title/type/score来源snapshot，正文仍原problem；题目软删不重新算历史score。对仍在进行且snapshot引用的题目，拒绝type/答案/case权重/文件修改或删用例，只允许显示内容不影响判分的改动；HOMEWORK有效补交窗口也视为仍在使用评分内容。delByListIds现有无条件remove必须修成仅目标list IDs；physical列表删除被有关联RESTRICT阻止并返回明确业务提示。

11. 不应该修改的内容：不重写resizable/导航前端，不锁住所有普通练习题单直到永远，不复制Markdown/case文件到snapshot，不修改当前case存储桶/挂载。

12. 边界条件：共享list两场比赛、不存在list、旧重复order规范化、快照空/题软删、允许title说明但不能改type答案、批量删除不可误删其他list、补交用户实际期限未到。

13. 并发/事务/幂等要求：list mutation仅list锁，snapshot contest→list→problem IDs升序；评分内容mutation预查snapshot引用和未冻结活动当前list引用，锁被影响contest IDs升序→problem→case，再重查当前引用/Clock/pending/补交窗口。发现新增未锁有效contest引用时409回滚重试，不从problem反向追加contest锁。进行中、HOMEWORK有效补交窗口和结束后仍PENDING均拒绝修改。MinIO新文件先上传新UUID路径，事务内仅改DB引用，不能原路径覆盖；失败清理新对象；原对象不请求内删除，以免练习判题仍引用，手册列人工对账回收。不锁内网络IO。guard与修改同TX，freeze持有problem行锁，覆盖查后开始/freeze的窗口。

14. 测试要求：snapshot一边创建一边改list不混合集合；运行赛事拒绝case mutation与freeze竞态；独立题仍可编辑；existing题单排序batch完整性回归；删除list IDs不会全表清空；统计join以snapshot与user_id正确限定。

15. 验收标准：冻结比赛不被共享题单后改污染，运行中的评分条件一致；题单正常编辑与历史显示保持。

### T19：非OJ统一计分、草稿隔离与首AC积分事件

1. 任务名称：非OJ统一计分、草稿隔离与首AC积分事件

2. 目标：修复落库前后分数不一致，区分草稿和成绩，保证完成关系只因真实AC产生。

3. 前置依赖：T18-C、T05。

4. 涉及模块：problem-service、common-api小型计分helper、judge-server仅统一计算调用。

5. 需要阅读的现有文件：P/service/impl/JudgeServiceImpl.java(judgeFill/judgeChoice/record顺序)；P/service/impl/RecordsServiceImpl.java、ProblemCompleteServiceImpl.java；P/controller/RecordController.java；P/config/JudgeConfig.java；J/judge/impl/DefaultJudgeImpl.java(judgeFinalScore)；T18 Coordinator和ContestSubmissionContext；A/client/user/client/UserInternalClient.java(addPoint)。

6. 需要新增的文件：A/util/WeightedScoreCalculator.java；P/service/ContestAnswerSubmissionService.java、ContestDraftService.java、ProblemCompletionAwardService.java；PT/service/ContestAnswerScoringTest.java、ContestDraftTest.java、ProblemCompletionAwardTest.java；AT/util/WeightedScoreCalculatorTest.java。

7. 需要修改的文件：JudgeServiceImpl非OJ路径先统一计分再record；RecordsServiceImpl分离graded/draft与取消内部远程addPoint；ProblemCompleteMapper支持insertIgnore受影响行数；RecordController.save改Draft或已有nonOJ受理服务；ContestRecords/Answer mapper/XML实现投影与draft更新；DefaultJudgeImpl调用同WeightedScoreCalculator，不改变grade内容/并发。 T18-A JudgeSubmissionService在OJ受理事务内保存当前提交答案，OJ回调不再覆写草稿。RecordsServiceImpl.score改查contest_records而非废弃records.contest_id。

8. 数据库变化：正式nonOJ分配attempt、写COMPLETED及统一score/correct；成绩applied_attempt_seq更新，答案仍原表；草稿没有attempt，只更新答案；首次AC插problem_complete及POINTS_AWARDED outbox。

9. API变化：原非OJ /judge和/record返回兼容；正确性结果仍比赛隐藏答案，非比赛保留原feedback；新Helper仅内部Java。

10. 具体实现要求：E§1-3换算BigDecimal，在写成绩前完成，选择/填空duplicate index拒绝。nonOJ是同步COMPLETED同受理事务，成绩最新seq才更新；draft初始化0/statusfalse但已存在不能覆盖score/status/seq，仅answer UPSERT。所有题型与练习只有status=true才finish，insertIgnore=1才outbox积分，amount按现有JudgeConfig并scale2，virtualID0不发用户积分。保留原练习Records和user统计接口的分数语义，不把所有练习强制题单换算。已有problem_complete不批量修历史，不重复奖励。 OJ正式受理保存当次code/language到现有答案表；后来更改的草稿由独立draft写保留，OJcallback只改成绩，不把旧提交源码覆写当前草稿。

11. 不应该修改的内容：不创建另一套评分标准、不改fixedAwardPoint/awardPoint，不回补旧积分、不将draft当作一次OJsubmit，不在本地事务直接调用user.addPoint。

12. 边界条件：原fullMark为0/0.000比较signum、部分得分、分母负值拒绝、maxScore0、原始重复index、正确true但score0可为AC、草稿覆盖已AC不得清成绩、WA不finish。

13. 并发/事务/幂等要求：受理seq/成绩/答案/完成关系/outbox同事务；首AC unique插入防两提交竞态；TX回滚积分事件同步回滚，用户consumerreceipt防重复。

14. 测试要求：1/3×10应3.33、fullMark0、浮点不存在、比分相同可稳定；非OJ记录与返回均统一；草稿不触finish/points，WA不finish、两个并发AC一事件；共享Helper对OJ/非OJ一致。

15. 验收标准：新赛事DB分数与统一规则一致，草稿不影响成绩，首AC完成/积分最多一次，历史数据不改。

### T20：OJ结果原子应用、重复/乱序保护与伪造入口清除

1. 任务名称：OJ结果原子应用、重复/乱序保护与伪造入口清除

2. 目标：把判题最终状态、case和成绩落库变为可靠事务，使终榜的pending判定可信。

3. 前置依赖：T19。

4. 涉及模块：problem-service、common-api遗留契约、gateway；judge-server只回调配合。

5. 需要阅读的现有文件：P/controller/InternalController.java、RecordController.java；P/service/impl/SubmitLogServiceImpl.java、RecordsServiceImpl.java；P/service/impl/JudgeCaseLogServiceImpl.java；J/mq/JudgeListener.java；J/judge/impl/DefaultJudgeImpl.java；A/client/problem/client/ProblemInternalClient.java、RecordClient.java；A/client/judgeserver/client/JudgeClient.java；gateway-server/src/main/java/com/anishan/gateway/filter/SecurityFilter.java；T18/T19新服务。 A/client/problem/client/SubmitLogClient.java与SubmitLogController四个写端点，先全局确认无业务调用。

6. 需要新增的文件：P/service/JudgeResultApplicationService.java；PT/service/JudgeResultApplicationTest.java、JudgeStatusTransitionTest.java；JT/mq/JudgeListenerCallbackTest.java。

7. 需要修改的文件：InternalController.judgeResult/judgeStatus委派service；SubmitLogService及Impl终态/状态方法；ContestAttempt/Records/Case mapper/XML；JudgeServiceImpl的dispatch失败路径使用同application入口（不得伪造已投任务失败）；RecordController移除judge-save方法，RecordClient及JudgeClient删除无调用judgeSave声明（RecordClient无剩余方法则删文件）；gateway block规则临时加外部旧/record/judge-save兼容防护，不放行/internal；JudgeListener重复任务可先查既有internal提交状态避免已applied重跑（只读契约见下面）。 删除/log/queue、/log/log-judge、/log/update、/log/change-status四个遗留公开写路由与SubmitLogClient对应无调用mutation声明；保留get/poll/case/admin读取及真正/internal结果链。ProblemInternalClient新增只读状态契约。

8. 数据库变化：result_applied/score/completed_at、caseunique、attempt COMPLETED/INFRA_ERROR与applied_seq、首AC/outbox同TX。

9. API变化：保留POST /internal/judgeResult、/judgeStatus；新增仅internal GET /judge-submission/{submitId}返回{resultApplied,status}供JudgeListener跳已完成任务，不含code；Gateway阻断。删除外部伪造保存。

10. 具体实现要求：比赛锁contest→log→attempt→投影；身份从存log核对request字段（userid/problem/contest必须一致）；不存在submit404，拒绝不匹配；第一次final result状态/日志/case/attempt/score全事务，resultApplied=true重复相同结果成功，不删除重插case/成绩/points。case_index唯一且非负，callback totalCount与结果校验；CE允许没有case日志，基础设施失败只通用公开状态、internal保留原详细原因。较大accepted seq覆盖成绩，小seq只记录历史attempt；JUDGE_ERROR终止pending但不覆盖旧有效成绩。中间status无法降级terminal。afterCommit安全释放名额，处理失败仍让Feign异常触MQretry。LEGACY旧已应用回调无操作；不是把还未完成设置applied。 OJ回调不更新答案草稿表，代码正式历史在submit_log；准备判题DLQ policy避免结果Feign重试耗尽后任务被丢弃，T24实施运维步骤。

11. 不应该修改的内容：不将JudgeCaseLog内部原因暴露用户，不替换JudgeResult枚举，不改sandboxCPU/并发，不用finally释放失败回调的新任务锁，不建立另一套result MQ落库体系。

12. 边界条件：旧A迟于B回调、B WA覆写A AC、B infra不覆写A有效，重复callback，DB在case写入后失败整体rollback、终态以后running、submitId与owner不符、standalone没有contest_attempt。

13. 并发/事务/幂等要求：DB事务result_applied是业务幂等，不能只在Controller先if查询；tx内唯一case UPSERT或仅首次insert，不删除日志做幂等。保持第一有效终态不可被不同结果覆盖；不匹配duplicate记录告警不改。

14. 测试要求：两个连接同时callback一次应用、A/B反序、失败rollback全部、积分一次、caseunique、假actor/contest拒绝、terminal降级、Redisrelease afterCommit/rollback不释放、gateway旧入口403/404且内部结果调用正常。 普通用户直接POST四个/log写路由必须拒绝，不能改任意submit.status/result_applied。

15. 验收标准：没有final log但score未应用的可见中间态；重复/乱序不污染成绩，公开端不能提交JudgeScore伪造成绩。

### T21：版本化最终榜单生成、租约与历史模式

1. 任务名称：版本化最终榜单生成、租约与历史模式

2. 目标：以现有成绩投影为源一次生成可恢复的最终成绩快照，正确等待晚判题。

3. 前置依赖：T20。

4. 涉及模块：problem-service。

5. 需要阅读的现有文件：P/service/impl/RecordsServiceImpl.java；PX/RecordsMapper.xml；P/service/impl/ContestServiceImpl.java；T17 RankSnapshot/Entry/Attempt Mapper；T18 Coordinator；T20 application；P/ProblemApplication.java。

6. 需要新增的文件：P/service/ContestFinalRankService.java；P/service/impl/ContestFinalRankServiceImpl.java；P/job/ContestFinalRankJob.java；P/config/ContestRankProperties.java；PT/service/ContestFinalRankServiceTest.java、ContestFinalRankRaceTest.java。

7. 需要修改的文件：RankSnapshot/Entry Mapper/XML实现claim/聚合分批/发布；AttemptMapper pending/source查询；resources/config/problem-service.yaml增加rank job enabled/interval15秒/batch20/lease120s/cleanupBatch500/queryTimeout60s。

8. 数据库变化：snapshot header state WAITING/BUILDING/READY/ERROR，已发布version与candidate隔离；entries aggregate数据。新CURRENT/WEIGHTED_LATEST_V1，历史LEGACY/LEGACY_RECORD_V1。

9. API变化：内部ensureScheduled/rebuild只登记/claim，HTTP不执行大聚合；Browser API在T22。

10. 具体实现要求：按照E§8-12；扫描有索引条件，只处理已结束CONTEST。pendingcount以attempt PENDING以及相关未应用log验证（异常不一致ERROR记录）；超过15分钟不假0，ERROR等待实际结果/重试。claim contest+header锁取source_seq；单独DB事务执行一次INSERT INTO contest_rank_entry ... SELECT聚合和窗口排名到candidateVersion，不能每500人重新扫描并计算全榜；查询timeout60s，单独定时心跳续约，Java不载入全榜。RANK按score唯一主指标、ROW_NUMBER按score desc userid asc，roster LEFT JOIN含0分未交卷，new answered_count只有效applied成绩（不是draft）。发布再验证lease owner/source/pending/currenttime、原子更新header，no pending前不得READY；losslease候选禁止发布。rebuild version>0服务旧结果。清理只非当前/非活跃候选与超过保留2版的数据。 ERROR退避使用build_attempts/next_build_at，按E节固定公式，claim仅已到期项；header.next_version在每次claim锁内递增分配全新candidateVersion，失去lease不得再写该版本；候选SQL执行前后检查owner/lease并独立每30秒心跳续约120秒，过期不可复活。候选source_seq只在成功publish时写header，重建期间不改旧元数据。

11. 不应该修改的内容：不新增ACM罚时、不遍历submit_log全部code/case作为榜单，不为每contest开线程、不在接口直接run聚合、不自动终态pending。

12. 边界条件：0用户/0题0分、全部同分、合法结果在end后很晚才到、重复BUILDING租约接管、部分candidate写入、legacy缺满分、clock边界、已删除比赛不生成。

13. 并发/事务/幂等要求：DBlease owner fencing+contest source序号校验，Redis不做唯一锁；同seq重复rebuild最多一次active，旧entry版本完整保留；批查询不offset扫全部提交。

14. 测试要求：同分1/1/3、0分/未交卷、latestscore source、超时pending不READY、late callback后READY；双worker只一个publish、lease失效旧owner不publish、半途失败旧version仍有效、LEGACY不重算原分。 旧owner/newowner必须不同candidateVersion，证明旧owner写入无法混入新快照。

15. 验收标准：最终状态真实可信、数据源与原统计一致；内存/查询有界；任何失效worker不能覆盖新版榜单。

### T22：最终榜单API、角色/成员权限与缓存

1. 任务名称：最终榜单API、角色/成员权限与缓存

2. 目标：提供最终成绩分页和管理重建，缓存只能加速数据不能绕过权限。

3. 前置依赖：T21、T04。

4. 涉及模块：problem-service、content-service cache目录。

5. 需要阅读的现有文件：P/controller/ContestController.java；P/domain/vo/ContestVo.java；T21 rankservice；P/service/impl/ContestServiceImpl.java；A/config/RedisConfig.java、util/CacheUtil.java；A/client/user/client/UserInternalClient.java；content-service/src/main/java/com/anishan/content/service/CacheCatalog.java。

6. 需要新增的文件：P/controller/ContestRankController.java；P/domain/dto/FinalRankPageQuery.java；P/domain/vo/FinalRankVo.java、RankEntryVo.java；P/service/ContestRankQueryService.java、ContestRankPageCache.java；PT/service/ContestRankPermissionTest.java、ContestRankPageCacheTest.java。

7. 需要修改的文件：RankEntry/Snapshot Mapper/XML分页；CacheCatalog加ID25 final-rank；共用summary调用；无改通用CacheAspect。

8. 数据库变化：无新表。每请求先DB contest资格+header version，排名页索引DB查；用户摘要不存缓存。

9. API变化：GET /contest/{id}/final-rank；GET /contest/admin/{id}/final-rank；POST /contest/admin/{id}/final-rank/rebuild，精确响应§8.3；admin额外buildinfo内部原因仅授权者看，普通仅安全状态。

10. 具体实现要求：PUBLIC登录、PRIVATE/WhiteList成员、admin版rankauthority；时间未结束409、HOMEWORK400、缺资源404。分页max50、ID/score string；等待首次ranking=null，已发布version则BUILDING/ERROR仍返回旧版ranking。触发schedule是轻量登记，不聚合。缓存F约定6h普通JSON与版本页，不@Cacheable包鉴权。批查摘要最多50/100IDs、不用管理UserVo。cache不可用fallback DB，不产生全局错误；重建只已结束+no pending，single active返回accepted原任务；headerlastError不普通输出。

11. 不应该修改的内容：不把公开排名绑student新permission、不用角色判断、不缓存成员判定/not-auth结果、不加redis分布式锁、不清空原contest缓存prefix。

12. 边界条件：private未joined、已交卷仍可看、成员移除不得旧cache继续开放、version变页号重置、摘要删号fallback、Redis JSON坏数据删除该key回源、pending非0重建409。

13. 并发/事务/幂等要求：资格每次DB检查，cache key绑定当前version；badcache单key删除；重建CAS与T21lease，不等待worker；允许相同cachemiss DB query重复但不得有重复排名计算。

14. 测试要求：权限矩阵与匿名401、HOMEWORK/未结束、cachehit仍核members、Redisdown/badjson、page size和Long、安全VO无源码/邮箱、重建重复/oldversion结果。

15. 验收标准：用户能安全分页读取最终结果；缓存不导致私有赛名单泄漏；管理员可无重复触发重建。

### T23：比赛结束成绩区域与后台重建入口

执行状态（2026-10-04）：已完成。用户追加授权修复既存全屏高度覆盖后，比赛/作业三个屏宽及明暗主题回归通过；前端构建、106项测试通过。详见 `docs/T23-verification.md`（真实环境端到端测试仍未执行）。不要重复实施；依赖图下一项为 T24，本轮未执行。

1. 任务名称：比赛结束成绩区域与后台重建入口

2. 目标：展示最终排名和结算状态，保持原做题页面空间与侧栏行为。

3. 前置依赖：T22。

4. 涉及模块：oj-vue。

5. 需要阅读的现有文件：V/views/contest/ContestProblems/ContestProblems.vue；V/views/contest/Contest.vue；V/components/activity/ActivityList.vue；V/api/contest/index.ts；V/views/backend/teacher/ContestManagement.vue；V/views/backend/teacher/contest-manage/user-statistic/UserStatistic.vue；V/components/ActivityResizablePanel/ActivityResizablePanel.vue；V/utils/contest/index.ts；V/components/Avatar/Avatar.vue。

6. 需要新增的文件：V/api/contest/rank.ts；V/components/ContestFinalRank/ContestFinalRank.vue；V/composables/contest/useFinalRank.ts、useFinalRank.test.ts；V/utils/contest/rankDisplay.ts、rankDisplay.test.ts。

7. 需要修改的文件：ContestProblems.vue ended overview/result区接小入口/排名panel；后台UserStatistic.vue现有statistics区加有rank权限的最终结果/rebuild入口；ActivityList已结束CONTEST提供结果入口但HOMEWORK不改；不新增主menu。

8. 数据库变化：无。

9. API变化：T22 finalrank/admin/rebuild，不将旧record/statistic代替新稳定最终数据。

10. 具体实现要求：活动已结束overview现有正文区域直接嵌入ContestFinalRank结果组件；不新增路由、巨型顶部状态栏或第二个侧栏。排名表只在现有正文剩余高度内滚动。列表用rank/userAvatar/nikeName/specialRole/score/correct/handedIn，同分不自行改rank；legacy标‘历史成绩’，pending仅‘成绩正在结算’，error基础提示+可重试。有原版ranking则显示并提示更新，不闪空。可见WAITING/BUILDING/首版ERROR每30秒，hidden/deactivated停止、焦点即刻、READY停；对同contest共享请求singleflight、不创建永久interval。关闭panel清timer，ID切换清state/generation，分页保留直到version变化。

11. 不应该修改的内容：不改变比赛答案提交/全屏编辑器/可折叠竖向侧栏/resizable样式、不把作业补交排名、不在结果展示内部error/sql/polling文案，不删题目新窗口能力。

12. 边界条件：0用户、同分、19位ID、未知摘要默认头像不能用自己、暗色、窄屏横向结果表允许内部滚动但页面不无限高、WAITING切另比赛、rebuild权限缺少。

13. 并发/事务/幂等要求：route/sessionGeneration丢旧数据、单flight重建按钮固定宽度/disabled；重复focus不用连续叠请求，关闭/退出取消轮询；score仅服务端返回不JS加总rank。

14. 测试要求：fake timers visible/pending/ready/hidden与late结果、version更新page重置、rolepermissions、legacy/noresult、stabletie rendering；手动390/768/1440+dark+比赛做题全屏回归，pnpm build/test。

15. 验收标准：结束比赛可以看真最终成绩/结算状态；管理员可重建；正常做题布局不被新大状态栏或后台timer挤占。

### T24：跨模块集成、迁移预检查与发布手册

1. 任务名称：跨模块集成、迁移预检查与发布手册

2. 目标：验证整套功能在故障/并发/老数据下可运行，并提供不会误改线上数据的一套交付流程。

3. 前置依赖：R02-D、T07、T14、T15、T16、T23（及其全部前置）。

4. 涉及模块：全项目相关测试、migration scripts、docs；只在授权时部署。

5. 需要阅读的现有文件：docs/IMPLEMENTATION_PLAN.md、本任务文件；pom.xml/test profile；现有UT/StepUpRedisTest.java；S/migration/apply.sh；scripts/start-dev-infra.sh；deploy/server/deploy.sh；S/db_user.sql；S/db_problem.sql；S/db_content.sql；content-service CacheCatalog；冻结历史三份+forward _4/_5+contest _6/所有新增worker；oj-vue/package.json。

6. 需要新增的文件：集成包：CT/integration/CommunityPersistenceIntegrationTest.java、JudgeResultRaceIntegrationTest.java、ContestRankIntegrationTest.java；UT/integration/NotificationIntegrationTest.java、UserSocialMigrationIntegrationTest.java；scripts/check-community-deployment.sh（只读preflight）；docs/community-and-final-rank-rollout.md；按现有maven目录放src/test/resources/application-community-integration.yaml和受控schemafixtures。

7. 需要修改的文件：扩展R02-B已有CT/integration/SolutionDomainMigrationIntegrationTest.java（不重新创建/覆盖已有验证）；pom.xml增加显式community-integration测试profile/目标白名单（不得关闭默认测试）；S/migration/apply.sh、scripts/start-dev-infra.sh、deploy/server/deploy.sh统一manifest/phase迁移顺序且纳入既有20261003（沿用R02-B，不恢复自动V*.sql遍历），_5 copy/cutover必须显式维护授权；部署脚本仅准备安全preflight调用，不自动启动/部署线上；必要package测试配置不添加npm依赖或大型browser框架。

8. 数据库变化：测试只独立oj_it_user/oj_it_problem/oj_it_content，真实迁移两次验证结构/行数不重复；业务库执行仍必须另获授权。生产备份+schema检查+无活跃赛事/补交/在途判题后升级，保存case duplicate备份。

9. API变化：跨service完整API与权限验收矩阵；对非法/匿名/伪造internal/旧judge-save验证拒绝；不新增产品接口。

10. 具体实现要求：从B～F写集成测试，不仅mock宣称通过。至少真实MySQL并发follow/like/commentclose/callback、Rabbit duplicate/confirm/return/消费者retry→DLQ/fanout断点、Redis限频/缓存降级；isolatedvhost固定/oj-community-it、allowlist DB、Redis prefix必须配置且验证。Maven默认tests全部运行，集成profile缺资源失败而不是偷偷skip。preflight只读检查MySQL8/schema/collation/index/正在进行CONTEST/有效补交写入窗口/QUEUE compiling running/未applied/服务内网暴露/MQ声明权限；存在活跃成绩链阻止发布，不能‘自动关赛’。手册说明本地构建target、上传现有发布流程、备份/显式题解copy-verify-CUTOVER/服务版本切换/前端与legacy alias发布/角色refresh/受控旧缓存清理/replay/开关与故障回滚，遵plan§13回滚：CUTOVER产生新写后绝不能只回滚legacy problem binary到陈旧源表；优先前滚/兼容content旧镜像，反迁移需停写、逐表校验及独立授权，绝不down-volume或回滚DROP。 声明judge-exchange.dlx.v1与judge-info.dlq.v1，在既有vhost设置仅匹配^judge-info-queue$的policy dead-letter-exchange=judge-exchange.dlx.v1、dead-letter-routing-key=judge-info.dead.v1；不能修改原队列声明arguments。用Rabbit管理命令验证policy，演练失败judge消息进入DLQ并原ID重放。

11. 不应该修改的内容：不直接部署ssh oj-server、不清理生产容器/卷、不执行初始化DROP DATABASE/全库flush、不删除用户数据/密钥、不让测试代码生成器或危险测试恢复。

12. 边界条件：空库与历史生产形状、迁移重复、旧private题解firstPublished、重复case、缺父menu、不正常时区、MQ权限不足/停机、Redis停机、服务重启中间lease/半榜单；测试全部事务只在白名单schema。

13. 并发/事务/幂等要求：故障注入必须覆盖业务commit→publish与publish→sent、消费commit→ACK、扇出批commit→cursor、判题log→score TX失败、finalcandidate→headerpublish；所有重复结果数据不增，read状态不回退。

14. 测试要求：运行Java11 mvn -B -Dmaven.jar.forceCreation=true package、pnpm build与vitest；显式community-integration真实中间件测试；记录环境/测试数量/命令/跳过；手动普通/限制/教师/管理/超管权限及各viewport/dark回归；EXPLAIN关注反向查询/根评论/未读/attemptpending/rank分页走新索引，大量submit数据rank API不扫描code。

15. 验收标准：plan§12.2全部13场景有证据；两次迁移结构/权限/业务行稳定；清单列完新cache keys/表/权限/exchange/queue/jobs/TTL，无敏感日志；未获授权时只交付patch/手册，不部署。

## H. 每项交付报告格式

完成Task编号；变更文件；数据库/权限/API/cache/MQ/job变化；实际测试命令与结果；边界/失败场景证据；尚未运行的验证及原因；不扩大下一项范围。T24通过之前不得把整套功能称为已可上线。
