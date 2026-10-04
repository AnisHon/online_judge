# 社区功能与最终榜单交付/发布手册

本文按 `docs/IMPLEMENTATION_PLAN.md` §12–13 操作。它是发布清单，不会授权生产变更。默认只做只读检查；生产变更需要维护窗口、备份核验和独立操作授权。

## 构建与发布包

在本地使用 Java 11 构建后端：

```sh
JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk PATH=/usr/lib/jvm/java-11-temurin-jdk/bin:$PATH mvn -B -Dmaven.jar.forceCreation=true package
cd oj-vue && pnpm install --frozen-lockfile && pnpm build
```

上传 Maven 生成的各服务 `target/*-1.0.0-beta.jar`、`oj-vue/dist/`、`resources/sql/migration/`、`resources/config/`、`deploy/server/`、`scripts/check-community-deployment.sh` 与 manifest 校验脚本。发布包不得包含 `.env`、生产密钥、MySQL dump、`target/test-classes` 或测试凭据。上传前核对 `resources/sql/migration/manifest.tsv` 的 SHA-256；不要在服务器编译。

服务器只读预检查：

```sh
deploy/server/deploy.sh --preflight-only
```

配置 `OJ_PREFLIGHT_*` 后也可单独运行 `scripts/check-community-deployment.sh`。检查会因缺表/索引、活跃比赛/补交窗口、排队或未应用判题结果、Rabbit 权限/策略、服务端口绑定异常而失败；失败时停止发布，由维护人员查明具体数据链状态。它不会自动结束比赛、标记旧结果已应用、运行迁移、改 Rabbit 策略或清缓存。

## 数据迁移与 cutover

1. 先验证数据库备份可读并完成恢复演练；确认 case 重复备份空间足够。记录三库 schema、表行数、权限关系、outbox 计数、migration manifest 与校验值。
2. 维护窗口开始后暂停题解写入、社区 outbox relay、判题受理/回调变更和可能修改比赛 roster 的管理写入。等待所有正在运行的判题和 final-rank 构建/租约结束。预检查发现任何未应用结果或有效补交窗口都应阻断发布；由业务操作人员决定延期。
3. 对 LEGACY 数据源运行 manifest `standard` 与 `content-prepare` schema phase；确认 `_4` 仅建目标 schema。然后用 `scripts/migrate-solution-domain.sh` 显式执行 `prepare → copy → verify`。copy/verify 需要维护、源停止写入、relay 停止及源备份确认。先做 dry-run/计数比对，正式 copy 使用小批量、支持续跑；冲突即停止并保留源与目标证据。FRESH 环境使用 `FRESH` source mode，不得把旧题解 schema 当成必需初始化步骤。
4. 校验源/目标题解主行、正文、互动关系、审核记录和七类社区 outbox 的摘要与数量；确认 `first_published_at` 的旧值、重复 case 备份、历史私有题解和孤儿引用语义。重复执行 prepare/copy/verify 后行数和摘要必须稳定。迁移程序不得删除旧表或备份。
5. 仅在 source 与 target 二次摘要相同、没有源写入且状态为 `VERIFIED` 时显式 CAS `CUTOVER`。保持题解写入口/relay 关闭，按兼容顺序部署 problem-service（不再承担题解领域）、content-service、user-service、Gateway alias 和前端；确认内部网络与权限后再开启新写入和 worker。刷新受影响账号的菜单/角色缓存，验证管理员与作者权限矩阵。

历史 migration `_1`、`_2`、`_3` 已冻结，不能编辑或重算其 checksum。schema 变化只追加 manifest forward migration。不要直接执行 `resources/sql/db_*.sql`，这些初始化脚本含 DROP。

## RabbitMQ、重试与人工恢复

部署前确认应用能在目标 vhost 声明新增社区/积分拓扑。判题 broker policy 仅匹配原队列：

```sh
RABBIT_VHOST=/judge # 必须先核实与当前服务的 oj.mq.v-host 配置一致
rabbitmqctl set_policy -p "$RABBIT_VHOST" judge-info-dlx '^judge-info-queue$' \
  '{"dead-letter-exchange":"judge-exchange.dlx.v1","dead-letter-routing-key":"judge-info.dead.v1"}' \
  --apply-to queues
rabbitmqctl list_policies -p "$RABBIT_VHOST"
rabbitmqctl list_queues -p "$RABBIT_VHOST" name messages_ready messages_unacknowledged arguments
```

不要给 `judge-info-queue` 增加声明 arguments，已有队列只由 broker policy 添加 DLX。确认 `judge-exchange.dlx.v1`、`judge-info.dlq.v1` 与绑定已经存在，故障演练消息能进入 DLQ，并用原 `submitId` 修复后重放；不得伪造新提交。社区 consumer 的重复投递由 event/dedupe 唯一键收敛；fanout 断点重跑保留 eventId/cursor。仅重放 FAILED 记录，使用对应受限管理 API/操作流程，保留原 eventId。确认消息 publish confirm、mandatory return、消费者重试与 DLQ 指标均可观测。

## 缓存、权限与 worker 清单

| 项目 | 所有者/用途 | 保留/TTL | 发布操作 |
|---|---|---|---|
| `user_follow`、`user_notification`、`notification_fanout_job`、`user_point_award_receipt` | user-service 关系、收件箱、可恢复扇出、积分收据 | 通知 180 天；DONE fanout 30 天；收据长期保留 | 只按计划批量清理到期通知/job |
| `solution_explanation*`、`solution_like`、`solution_comment`、`comment_like`、`solution_moderation_action`、`content_event_outbox`、`solution_problem_reference`、`content_solution_migration_state` | content-service 题解与事件 | outbox SENT 社区事件 30 天；FAILED 不删；reference 持续刷新 | CUTOVER 后由 content worker 接管 |
| `contest_attempt`、`contest_rank_snapshot`、`contest_rank_entry` | problem-service 判题/最终排名 | 排名快照不自动清理 | 发布后比较 candidate version/source sequence |
| `problem:profile:v3:{userId}:*` | 个人主页练习/比赛/热力图 | 2 小时 | 旧 v1/v2 只做受控迁移清理 |
| `content:comment:write:{userId}` | 评论创建限频 | 60 秒 | 不清除活跃限频键 |
| `problem:contest:final-rank:v1:{contestId}:{version}:{page}:{size}` | 最终榜单页 | 6 小时 | 新版本自然换 key；Redis 故障回源 DB |
| 判题锁、JWT refresh/step-up、fanout/outbox/rank lease | 安全状态或持久任务状态 | 沿用当前 TTL/DB lease | 不执行 FLUSHDB，不批量删认证/判题键 |

权限增量由 manifest 中 `_3` 迁移提供。检查 `problem:solution:*`、`content:solution:*`、`content:comment:*`、`user:follow:*`、`user:notification:*`、`problem:contest:rank:*` 相关资源与角色绑定，重载后台菜单权限；普通用户不授予后台审核、删除、重放或榜单重建权限。API 的所有者检查仍由服务端执行。

新进程内 worker：content 元数据投影每秒最多 100 项（目标 5 分钟循环）、content outbox relay 每秒 20 项（发送池 4、队列 20）、content 清理 job；problem points/judge relay 与 final-rank job（每 15 秒扫描至多 20 个结束比赛，固定 2 个构建槽和容量 2 的队列）；user fanout 与清理 job。没有额外常驻服务。每个 worker 都支持独立关闭开关；关闭后检查 SENDING lease 到期并恢复 PENDING，禁止把不确定发送标成 SENT。

旧缓存清理使用 SCAN + UNLINK，严格限定历史 problem solution/profile v1/v2 前缀并逐批抽样核对；不使用 KEYS、FLUSHDB 或 `problem:profile:v3`。缓存目录参见 content-service `CacheCatalog`；账号认证、判题锁和通知状态不属于迁移清理范围。

## 回滚边界

在 `CUTOVER` 前，旧 problem 仍是唯一写源；可停止流程并按迁移状态修复后重试，不能因失败删目标或源。`CUTOVER` 且 content 已产生新写入后，禁止只回滚到旧 problem 二进制/旧源表，否则会丢失题解互动、审核和事件。优先前滚修复，或部署同样读写 `db_content` 的兼容镜像。完整反迁移必须另行授权、停写停 worker、逐表双向差异核对、反拷贝并验证 schema/FK 后整体切回；本手册不执行反迁移。任何情况下都不回滚 volume、不运行 DROP、不删除用户数据。

## 验收留档

记录 Java 11 Maven package、pnpm build/Vitest、显式 community integration profile 的命令/测试数/跳过项；真实中间件测试使用独立 `oj_it_user`、`oj_it_problem`、`oj_it_content`、Rabbit vhost `/oj-community-it` 和唯一 Redis prefix。留存迁移前后摘要、两次重复迁移的行数/权限/outbox 证据、查询计划 EXPLAIN、broker policy/DLQ 原 ID 重放证据、服务绑定检查及普通/作者/受限/教师/管理员/超管权限矩阵。不要把仅编译、mock 或空环境的结果写成端到端通过。未完成全部证据时，标记为“未满足 T24 上线验收”。
