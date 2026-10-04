# R02-D 个人主页拆域记录

本轮只调整主页读模型，无 schema、事件或权限变更；未执行数据库迁移、Redis 清理或部署。

## 接口与数据

- 保留 `GET /problem-api/profile/{userId}`：练习、比赛、热力图、统计与本次调用的 owner；删除题解字段。
- 新增 `GET /content-api/profile/{userId}/solutions`：最多 50 条，通过第 51 条确认截断；本人可看私有/受限历史，其他人（包括管理员）仅有效公开。
- content 不缓存完整题解列表。候选每批最多 51 条、最多三轮修复；远程资格检查后重读本域版本/可见性。主页不读取 Markdown 正文。
- 前端并行请求 user/problem/content；题解失败是可重试的局部错误，不回退到旧 problem 响应中的题解。请求 generation 与认证 sessionVersion 丢弃过时结果。

## Redis 清单

|类别 ID|前缀|行为|
|---|---|---|
|23|`problem:profile:v3:`|新建，两小时，仅与查看者无关的练习/比赛/热力图；不含 owner 或题解|
|9|`problem:profile:v2:`|停用，保留清理入口|
|21|`problem:profile:v1:`|停用，保留清理入口|
|12|`problem:solution:`|停用，保留清理入口|

实际新 key 为 `problem:profile:v3:com.anishan.problem.service.ProfileActivityCacheService:getBase:<参数 MD5>`，沿用 CacheAspect。旧版本含双冒号的键仍由上述旧前缀覆盖。

发布后只有经明确授权的维护操作才能用 SCAN + UNLINK 清理上述三个旧前缀；本轮没有执行、不在启动时执行、不迁移旧私有缓存、不 FLUSHDB。认证、积分、判题及其他缓存不变。类别 24/25 留给后续评论限频与榜单任务，本轮未登记或实现。

## problem 残留引用审计

- `ProblemEventOutboxMapper.xml`：七类旧 community 字符串只用于隔离历史待迁移事件，不参与本域发送或清理；保留。
- `ProblemEventOutboxServiceTest`：历史 community fixture 用于验证越域事件不能被 problem 派发/清理；保留。
- `ProfileMapperTest`、`ProfilePrivacyCacheTest`：旧字段/SQL 名仅是“不再存在”的回归断言；保留。
- `InteractionSchemaTest`：说明本域不拥有题解互动，非业务引用；保留。
- 历史 migration、初始化/搬迁工具和文档中源 `db_problem.solution*` 名是旧数据迁移依据，不是运行态读写。历史文件未修改，旧数据库备份未删除。

没有新增 problem → content RPC，也没有恢复本域题解实体、Service 或 SQL JOIN。

## 本轮文件清单

新增：

- content 的 `controller/SolutionProfileController.java`、`service/SolutionProfileQueryService.java`、`domain/vo/ProfileSolutionVo.java`、`domain/vo/ProfileSolutionsVo.java`。
- problem 的 `service/ProfileActivityCacheService.java`、`domain/vo/ProfileActivityBaseVo.java`。
- `content-service/src/test/java/com/anishan/content/service/SolutionProfilePrivacyTest.java`。
- `problem-service/src/test/java/com/anishan/problem/service/impl/ProfilePrivacyCacheTest.java`。
- `oj-vue/src/api/profile/profileAggregation.test.ts` 和本记录。

修改：

- problem 的 ProfileService、ProfileServiceImpl、ProfileActivityVo、ProfileMapper、ProfileMapper.xml；原 ProfileServiceImplTest、ProfileMapperTest。
- content 的 SolutionQueryService、SolutionExplanationMapper、SolutionExplanationMapper.xml、CacheCatalog。
- `oj-vue/src/api/profile/index.ts`、`views/profile/Profile.vue`、`components/ProfileActivityPanel/ProfileActivityPanel.vue`、该组件的 `heatmap.test.ts`。
- problem 的 ProblemEventOutboxMapper.xml、ProblemEventOutboxServiceTest 只增加历史用途注释。

删除：problem 的 `domain/vo/ProfileSolutionVo.java`。T01、已有 R02-A/B/C 的数据模型与迁移均保留，不改历史 migration。

## 验证结果

- Java 11：`mvn -B -pl content-service,problem-service -am test` 成功。common-api 11 项、problem 32 项、content 35 项通过，共 78 项通过、1 项跳过。
- 跳过 `SolutionDomainMigrationIntegrationTest`：没有显式配置 `OJ_SOLUTION_MIGRATION_IT=1` 及隔离 MySQL 凭据；没有连接真实开发/生产库补跑。
- `pnpm exec vitest run src/api/profile/profileAggregation.test.ts src/components/ProfileActivityPanel/heatmap.test.ts src/api/solution/solutionDomain.test.ts`：14 项通过。
- `pnpm run build` 返回成功；另直接执行 `pnpm exec vue-tsc --build --force` 与 `pnpm exec vite build`，均成功。Vite 有既存大 chunk 警告。
- `git diff --check` 通过；搜索确认没有 problem 运行态题解实体/SQL 查询或旧 profile/solution cache 注解。
- 隐私测试包含本人/管理员看他人/匿名、AUTHOR_ONLY、刚隐藏、版本变化、孤立题目、50/51 上限、503、HTTP/R.code 对齐、19 位 ID；缓存测试经过 CacheAspect 代理验证命中、TTL、无 owner/题解、共享实例深拷贝。前端测试覆盖三路并发、局部失败、旧字段兼容、sessionVersion/generation 丢弃。

未执行真实三服务/浏览器端到端、真实 Redis 验证、migration、Redis 清理、部署或 git commit。数据库迁移与跨服务端到端由后续获授权的集成/发布阶段验证，不能以本轮单元测试代替。

R02-D 之后，按计划优先级下一项是 T03-C（content community outbox relay 与清理）；R02-B/T03 的现有实现已存在，尚未存在 content relay/service/job 实现。
