# T23 比赛结束成绩区域与后台重建入口：实施与验证记录

日期：2026-10-04。

## 状态与依赖

按最新 IMPLEMENTATION_PLAN / LUNA_TASKS 选择 T23；前置 T22 的 Controller、权限服务、版本页缓存及对应测试均已存在。没有重复执行 T01/T02，也没有迁移、删除其实现。本轮未实现 T24。

T23 已完成。初次回归发现既存全屏高度问题，原先因任务禁止改动编辑器样式而停止相关修复；用户随后明确允许先修复该问题。本记录保留初次失败及追加授权后的修复、复测结果。T24 本轮未执行。

## 本轮文件

新增：

- `oj-vue/src/api/contest/rank.ts`、`rank.test.ts`
- `oj-vue/src/components/ContestFinalRank/ContestFinalRank.vue`
- `oj-vue/src/composables/contest/useFinalRank.ts`、`useFinalRank.test.ts`
- `oj-vue/src/utils/contest/rankDisplay.ts`、`rankDisplay.test.ts`
- `problem-service/src/test/java/com/anishan/problem/service/ContestDetailTypeTest.java`
- 本记录。

修改：

- `oj-vue/src/views/contest/ContestProblems/ContestProblems.vue`
- `oj-vue/src/components/activity/ActivityList.vue`
- `oj-vue/src/views/backend/teacher/contest-manage/user-statistic/UserStatistic.vue`
- `problem-service/src/main/java/com/anishan/problem/service/impl/ContestServiceImpl.java`
- `problem-service/src/main/java/com/anishan/problem/controller/ContestController.java`
- `docs/LUNA_TASKS.md`：仅补充执行状态，不调整架构或后续任务契约。

## 实现

结束比赛在原正文嵌入最终成绩面板，默认收起原侧栏，不添加路由或另一侧栏；关闭后有重新打开的入口。作业不接入最终榜单。公开比赛的未参赛观众不调用需要参赛资格的题目/作答状态接口，榜单资格仍由 T22 后端校验。

后台用户统计的结果入口、管理查询和重建仅使用 `problem:contest:rank`；没有角色名判断。组件会再次检查权限，失去权限则停止请求。成绩表使用服务器排名、字符串分数、用户摘要、头像与特殊角色；未知摘要显式向 Avatar 传 null，不回退为当前用户头像。

控制器以会话/比赛/分页维度共享在途请求，丢弃旧路由或旧会话结果；WAITING、BUILDING、无旧版的 ERROR 可见时每30秒刷新。隐藏、停用、关闭、注销停止计时；恢复焦点立即刷新，同一在途请求不会叠加。READY 不定时查询。版本变化重置分页，失败或重建期间保留旧成绩。重建按钮固定宽度并禁用重复点击。内部诊断不进入展示状态。

必要的最小后端适配：原 ContestVo 已有 type，但详情查询没有 select type，导致前端无法可靠区分比赛/作业。补齐该列；Controller 对 type 缺失的旧缓存条目仅逐 ID evict 后重读，不改缓存键、TTL 或全局 catalog，不清整个前缀。这是已有契约的轻微实现遗漏，不是新的 API 或架构设计。

数据库、消息、缓存目录均无新增或迁移。使用 T22 的三个既定 API。

## 验证结果

- `pnpm run build`：最终版本通过，包含 vue-tsc 与 Vite。保留既有大 chunk/混合静态动态导入警告。
- `pnpm exec vitest run`：20个文件、104项测试全部通过；本轮新增10项前端测试。
- Java 11：`mvn -pl problem-service -am test -Dtest=ContestDetailTypeTest,ContestRankPermissionTest,ContestRankPageCacheTest,ContestDraftTest,ContestAnswerScoringTest -Dsurefire.failIfNoSpecifiedTests=false`：编译成功、16项测试通过（本轮新增3项）。复用现有 target，没有 clean、没有跳过上述测试。
- Chromium 实际页面、模拟 API：390/768/1440、明暗主题成绩表高度/宽度边界；历史成绩/未知用户；未参赛公开结果不调用参赛接口；关闭结果；后台重建固定宽度/重复点击只一次/保留旧成绩/撤销权限关闭。上述检查通过，截图已检查。
- `git diff --check`：通过。没有新增 !important、全局 z-index、SSE 或永久轮询 interval。

## 初次全屏回归失败及追加授权修复

三个屏宽、900px视口均可打开/退出全屏，但全屏工作台高度900px、top60px、bottom960px，底部超出60px。原因：`OjWorkbench.vue` 中 `.oj-workbench--fullscreen` 定义 height:auto / max-height:none，后面的 `.oj-workbench--activity` 又定义 height:100% / max-height:100%，同一元素同时拥有两类。使用 `git show HEAD:.../OjWorkbench.vue` 核对，HEAD 已有相同规则；本轮没有修改该组件或其样式。

初次失败没有记为通过，也没有在获得追加授权前跨越 T23 第11项禁止修改编辑器样式的边界。

用户追加授权后进一步检查实际 CSS 级联，确认 `ContestProblems.vue` 的 `.problem-detail-shell :deep(> .oj-workbench)` 也以更高优先级覆盖全屏工作台高度。因此仅修改两个父容器填满规则：

- `OjWorkbench.vue`：`.oj-workbench--activity:not(.oj-workbench--fullscreen)`。
- `ContestProblems.vue`：`.problem-detail-shell :deep(> .oj-workbench:not(.oj-workbench--fullscreen))`。

普通面板仍填满父盒；全屏高度仅由 top/bottom 定位计算。不添加 !important、z-index、固定视口像素高度，不改变侧栏、拖拽、提交或编辑器功能。新增 `OjWorkbench/workbenchLayout.test.ts` 两项 CSS 隔离回归守卫。

追加验证：

- 比赛与作业 × 390/768/1440 × 明暗主题，共12次实际浏览器全屏检查通过；900px视口下 top60px、height840px、bottom900px。截图已检查。
- 每次退出全屏后的工作台高度与进入前一致；成绩区域、后台重建、权限撤销检查重新通过。
- `pnpm run build` 通过；`pnpm exec vitest run` 21个文件、106项测试全部通过。浏览器仍采用模拟 API，没有将其宣称为真实环境端到端验证。

## 未执行

没有接真实后端/数据库的浏览器端到端测试；浏览器数据是显式模拟 API，不能代替真实环境集成验证。没有执行数据库 migration、访问服务器、部署或 git commit。没有改变题解领域归属及消息生产者职责。

上述全屏阻塞已解决，依赖图下一项为 T24。追加修复未修改后端、数据库、消息或缓存；未提交、未部署。临时浏览器与检查服务结束后关闭，用户原有开发服务不受影响。
