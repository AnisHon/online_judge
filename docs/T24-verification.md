# T24 验收证据记录

记录时间：2026-10-04（Asia/Shanghai）。本记录区分已验证与未验证项；不代表生产发布批准。T24 的 §12.2 全部场景未全部完成前，项目不能标记为已满足上线验收。

## 本地自动化结果

| 检查 | 结果 |
|---|---|
| Java 11 全仓 `mvn -B -Dmaven.jar.forceCreation=true package` | 最终改动后重跑通过：全 8 个 reactor 模块 `BUILD SUCCESS`；默认 Surefire 保持启用，T24 集成测试由显式 `community-integration` profile 执行。 |
| `mvn -B -Dmaven.jar.forceCreation=true -Pcommunity-integration -pl content-service,problem-service,user-service -am verify` | 通过；Failsafe 共 14 项：content 8、problem 2、user 4；0 failure / 0 error / 0 skipped。 |
| `pnpm build` | 通过。Vite 提示既有动态/静态路由重复导入和大 chunk；不阻断构建。 |
| `pnpm exec vitest run` | 通过：21 个测试文件、106 项。 |
| `python3 -m py_compile scripts/migrate_solution_domain.py` | 通过。 |
| `bash -n scripts/check-community-deployment.sh scripts/start-dev-infra.sh deploy/server/deploy.sh resources/sql/migration/apply.sh` | 通过。 |
| `bash scripts/check-community-deployment.sh --manifest-only` | 通过：manifest 文件及 SHA-256 校验通过；未连接数据库。 |

显式集成测试使用本机回环端口上的临时 MySQL 8、RabbitMQ、Redis；只创建/操作 `oj_it_user`、`oj_it_problem`、`oj_it_content`、`/oj-community-it` 和 `oj:community:it:*` 测试键。容器为本轮临时创建，配置了自动删除；Rabbit/Redis 镜像可能使用其默认匿名临时卷，未挂接现有命名数据卷。测试后已停止，并确认这些容器已不存在。测试覆盖迁移 fresh/legacy/T02 已执行/未执行/部分 backfill、批次中断恢复、重复 copy、目标冲突、cutover 后拒绝旧源重导；MySQL 关注/点赞/关评并发和判题回调/榜单竞态；Rabbit confirm/return、重复通知投递、重试耗尽进入测试 DLQ 且 messageId 保持；Redis 评论限频。

集成期间发现并修复了摘要校验对 `content_event_outbox` 错用源表名、隔离夹具按错误顺序清理 FK 子表，以及 legacy unknown-event 夹具漏建冻结 T02 outbox 的问题；随后整套 profile 重新通过。

## §12.2 场景证据状态

| # | 场景 | 当前证据 / 剩余缺口 |
|---|---|---|
| 1 | 关注者收到首次公开题解通知，重复/重试/扇出断点幂等 | 通知重复投递和 Rabbit retry/DLQ 有真实中间件证据；fanout worker 的真实 Rabbit + MySQL 崩溃/游标断点恢复尚未演练。部分。 |
| 2 | 私有/比赛题解不泄漏，读取缓存不能绕过授权 | `SolutionDomainReadTest`、`ProfilePrivacyCacheTest` 有自动化覆盖；跨服务真实 HTTP 与缓存预热顺序场景未运行。部分。 |
| 3 | 点赞/取消/重赞计数和通知幂等 | `CommunityPersistenceIntegrationTest` 对并发重复点赞验证唯一关系、计数和单条 outbox；取消/重赞通知在单独真实 MySQL profile 中未覆盖。部分。 |
| 4 | 评论两层展示、分页、XSS/Markdown/图片字面化、emoji | 评论校验/回复单测及前端测试通过；真实 API 与浏览器渲染联合安全验证未做。部分。 |
| 5 | 删除评论保留回复且不泄漏正文/原因 | moderation/visibility 单测覆盖；真实 MySQL 管理删除及作者读取原因的端到端矩阵未跑。部分。 |
| 6 | 关评与评论并发、已有评论保留 | `CommunityPersistenceIntegrationTest` 使用两个 MySQL 事务验证关闭先获得行锁后新评论返回 409，原评论和计数保留。通过。 |
| 7 | 学生权限、限制角色、管理员/超管实际菜单和管理权限 | 有权限单测/迁移脚本静态检查；真实数据库角色矩阵、浏览器菜单和越权 HTTP 验收未运行。未满足。 |
| 8 | 通知本人隔离、read-all 边界、轮询暂停/恢复/logout | 通知收件人条件、前端轮询 composable 有单测；多账号、多标签真实浏览器会话验收未运行。部分。 |
| 9 | 判题分数、迟到回调、JUDGE_ERROR 和草稿边界 | `JudgeResultRaceIntegrationTest` 在隔离 MySQL 验证重复/乱序回调、事务失败回滚与重试；完整赛制产品路径未做 HTTP E2E。部分。 |
| 10 | 比赛受理/交卷/判题完成边界与最终榜单纳入规则 | 有 contest/rank 单测，但 T24 profile 未以真实 HTTP/数据库时钟边界演练同毫秒提交、交卷与迟到判题链。未满足。 |
| 11 | 同分排名、未交卷/零分、密码/白名单成员访问 | 隔离 MySQL 榜单测试覆盖同分、roster 零行和 pending/recovery；密码/白名单真实访问控制未验证。部分。 |
| 12 | 多 worker、重复 rebuild、Redis 降级及半成品恢复 | 隔离 MySQL 验证并发 lease、失败保留旧快照及恢复；Redis 停机回源、独立多 worker 故障注入未运行。部分。 |
| 13 | migration manifest/重复执行、权限/数据/outbox 保持、cutover 安全 | 五项真实迁移集成场景与 manifest 静态校验通过，覆盖摘要/行稳定和 CUTOVER gate；现有真实业务库状态、真实菜单 grant、旧源/目标生产摘要与发布回滚未验证。部分。 |

## 明确未执行

- 未运行 `scripts/check-community-deployment.sh` 的数据库/容器/Rabbit 全量 preflight：它需要目标部署环境的只读凭据与正在运行的服务；没有使用开发或生产业务凭据替代。
- 未连接或迁移任何真实 `db_user`、`db_problem`、`db_content`，未执行服务器部署、Rabbit policy 变更、真实 DLQ 重放或 Redis 清理。
- 未在目标 Rabbit vhost 演练 broker policy 与 judge 原始 `submitId` 重放；测试 DLQ 是隔离 vhost 下的专用队列。
- 未运行跨 Gateway 的真实服务 HTTP/权限矩阵、普通/作者/受限/教师/管理员/超管手工账号检查、移动/桌面各 viewport 的明暗主题浏览器回归。
- 未对目标库大量 submit 数据运行 EXPLAIN/索引扫描验证，也未验证真实题解 migration 源库 journal、collation、旧数据行和备份恢复。

因此：代码构建及当前自动化测试通过，但 §12.2 全部 13 场景证据尚不齐全，T24 尚未达到完整上线验收；本文档和发布手册均不授权生产操作。
