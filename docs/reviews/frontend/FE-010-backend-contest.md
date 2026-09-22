# FE-010 竞赛/作业后台 Review

## 范围

- `oj-vue/src/views/backend/teacher/ContestManagement.vue`
- `ContestSubPageShell.vue`、`ClassView.vue`
- 竞赛/作业子页：参与用户、迟交、题目统计、题目分数、用户统计、用户分数、答题详情
- `oj-vue/src/api/contest/*`、`oj-vue/src/api/record/index.ts`

## 结论

竞赛和作业列表已经通过 `mode` 复用主页面，子页面也有统一壳，这是正确方向。但当前列表过滤、权限入口、ID/路由快照和统计数据加载仍混合在各页内；多数统计接口一次拉全量数据，前端再搜索/计算。小规模数据看起来正常，活动人数或题目数一大就会明显变慢，且多个页面的布局和操作列在窄屏下会消失。

## 问题清单

### FE-010-C-01 [高] 竞赛/作业搜索只过滤当前页，分页总数仍使用服务端总数

- 位置：`ContestManagement.vue:155,180-182`。
- `getContestAdmin` 只提交 `currentPage/pageSize/type`；标题/描述和状态在 `filteredList` 中本地过滤。`total` 仍是服务端总数。
- 影响：搜索结果只覆盖当前 20 条，分页器显示的总数与实际结果不符；用户以为没有记录，或翻页后结果“跳变”。
- 建议：关键词、状态下沉到后端查询并返回过滤后的 total；如果暂时只能前端过滤，必须先拉取完整数据且取消分页，不能两者混用。

### FE-010-C-02 [高] 竞赛/作业表单的日期和密码条件不完整，私有活动可能提交空密码或旧密码

- 位置：`ContestManagement.vue:105-106,174-179`。
- `rules` 没有 `auth` 和 private password 的条件校验；切换公开/私有时 `form.pwd` 不一定清空。日期 picker 只禁用日期，不保证同一天的时间粒度满足开始早于结束。
- 建议：为 `auth` 和 `pwd` 建立动态 rules；切换访问策略时清理不适用字段；提交前用同一时区解析并显式校验 `start < end`，后端也必须复核。

### FE-010-C-03 [高] 新建/编辑 debounce 持有同一个 `form` 引用，取消弹窗可能仍发出错请求

- 位置：`ContestManagement.vue:162,176-181`、`api/contest/index.ts:80-107`。
- add/update action 在 setup 时绑定 reactive `form`，`finishDialog/cancel` 会 reset 这份 form。保存后快速关闭、再次编辑或切换比赛/作业模式时，延迟任务可能读取另一条记录。
- 建议：提交时生成不可变快照，action 支持 cancel；保存按钮的 loading 应覆盖实际请求而不是只覆盖 debounce 等待。

### FE-010-C-04 [高][性能] 参与人和统计子页大量使用全量接口，前端再过滤/聚合

- 位置：`api/contest/user.ts:20-23`、`api/record/index.ts:59-76`；`UserJoined.vue`、`ProblemStatistic.vue`、`ProblemScores.vue`、`UserStatistic.vue`、`UserScores.vue`。
- 参与人、题目统计、用户统计、逐题得分都返回数组，没有分页/筛选参数；页面在浏览器中做搜索、排序、统计和渲染。
- 影响：活动规模增长后首次打开慢、内存高、表格 diff 和抽屉详情卡顿；统计数字还只代表当前加载快照。
- 建议：后端提供分页统计、服务端搜索和聚合字段；详情页按 ID 查询；前端只缓存当前页，必要时对统计使用明确的快照时间。

### FE-010-C-05 [中] 所有子页直接读取一次性 route params，组件复用时上下文可能错位

- 位置：`ProblemScores.vue:123-126`、`UserScores.vue:25-27`、`ProblemStatistic.vue`、`UserStatistic.vue`、`UserJoined.vue:59-61`、`Supplement.vue:39`。
- 多数页面将 contestId/problemId/userId 存为 setup 常量；同一组件实例被路由复用时不会重新加载，返回/前进和 keep-alive 可能显示旧活动数据。
- 建议：使用 computed params + watch，给请求加 session/request id；页面标题、返回目标、API 参数都从同一份 route snapshot 读取。

### FE-010-C-06 [中] `router.back()` 不是稳定的“返回管理”行为

- 位置：`ContestSubPageShell.vue:5`、`ProblemScores.vue:204-206`、`Supplement.vue:3`、`UserScores.vue:3`。
- 用户可能通过新标签、刷新、外部链接直接进入子页，此时 `router.back()` 会退到站外、空白页或上一个无关页面。
- 建议：共享壳提供显式 `backTo` route；有来源历史时才 back，否则 replace 到对应竞赛/作业管理页。

### FE-010-C-07 [中][权限] `canUseMore` 只判断“任一权限”，dropdown 可能出现空菜单或错误入口

- 位置：`ContestManagement.vue:148-150,75-91`。
- 更多入口用多个权限的 OR 判断显示，子项再分别过滤。只拥有其中一个不适用权限的账号会看到没有可执行内容的菜单；参与人管理、统计和迟交的权限边界也没有集中配置。
- 建议：由权限配置数组过滤出可见 command，再根据数组非空渲染 dropdown；command handler 仍做权限/路由兜底，不能只依赖隐藏按钮。

### FE-010-C-08 [中] 迟交状态依赖 `Date.now()` 渲染计算，不会自动过期刷新；删除接口却用 GET

- 位置：`Supplement.vue:6-7,43-56`、`api/contest/supplement.ts:10-14`。
- 当前时间只在组件重新渲染时计算，页面停留跨过截止时间后状态不会自动变；撤销操作调用 GET URL 删除资源，容易被缓存、预取或监控误认为读取请求。
- 建议：使用定时器或统一时间 composable，并在卸载时清理；后端删除接口改为 DELETE，前端 API 命名与 HTTP 语义一致。

### FE-010-C-09 [中] 迟交批量删除没有完整错误边界，单项失败会造成部分成功但统一刷新

- 位置：`Supplement.vue:54`。
- `Promise.all` 任一失败会 reject，前端没有逐项结果；后端可能已经删除部分用户，但页面只显示异常或直接中断，管理员不知道哪些仍保留。
- 建议：后端提供批量事务/批量结果；否则使用 `allSettled` 展示成功、失败 ID，并刷新服务端列表。

### FE-010-C-10 [中][CSS] 共享子页壳与各子页重复实现 panel/heading/table，统计条固定为四列

- 位置：`ContestSubPageShell.vue:44-47`、`ProblemScores.vue:218+`、`UserScores.vue:39+`、`UserStatistic.vue:42+`、`Supplement.vue`。
- 统计条、panel heading、用户 cell、result pill、表格行高被多份复制；壳固定四列统计，窄屏只在 800px 变两列，超长 label/ID 会撑开布局。`ProblemScores` 使用 `tone: 'orange'`，共享壳没有 orange 样式定义。
- 建议：壳接收可响应式 stats grid 或由 CSS auto-fit；状态 pill、用户单元格和分析 panel 统一组件；tone 使用受限 union，避免传入未实现的颜色。

### FE-010-C-11 [中][CSS] 小屏隐藏固定操作列，复杂统计无法继续下钻

- 位置：各竞赛子页的移动端样式及共享表格规则。
- 用户分数、题目分数、用户统计等页面在窄屏仍有固定“查看详情/退回/用户分数”操作；统一隐藏固定列会直接丢失下钻入口。
- 建议：将操作改为每行 kebab menu/详情抽屉；保留最小主操作，其他操作进入菜单，不能通过 `display:none` 消失。

### FE-010-C-12 [低] `MyClass.vue` 是空页面，若仍被路由/菜单暴露会产生可点击的空白后台页

- 位置：`teacher/my-class/MyClass.vue:1-13`。
- 文件当前没有模板内容、请求、空状态或权限说明。若数据库菜单仍指向它，用户会看到一个没有反馈的空页面。
- 建议：未实现功能从动态菜单/路由移除；若需要保留入口，至少显示明确的开发中/无权限状态，不要挂载空组件。

## 建议验收用例

1. 在第二页搜索一个只存在于第一页的数据，结果必须正确且 total 一致。
2. 私有活动切换公开/私有、同日调整时间，校验提示和最终 payload 正确。
3. 快速切换两个 contestId/userId，旧请求不能覆盖当前页面。
4. 1 万参与人/100 题时，列表和统计不再一次加载全部数据。
5. 360px 宽度仍能进入用户分数、题目分数、迟交撤销等关键操作。

## 本批执行结果（2026-09-22）

已完成：

- C-01：新增 `ContestAdminQuery`，关键词和活动状态由 `problem-service` 参与数据库分页，前端不再对当前页做二次过滤，分页 total 与结果一致；搜索/状态变更会回到第一页。
- C-02：前端增加访问策略、私有密码和开始/结束时间关系校验；切换为非私有策略会清理密码。后端新增同一时区下的时间关系和私有密码兜底校验。
- C-03：后台竞赛/作业保存改为提交时创建不可变 payload，直接等待实际请求完成；取消弹窗不会再触发旧的 debounce 请求，保存按钮 loading 覆盖真实网络请求。
- C-05/C-06：竞赛子页从响应式 route 参数读取上下文并监听路由复用；公共壳、统计下钻和迟交页使用明确的管理路由，不再依赖不稳定的 `router.back()`。
- C-07：更多菜单由权限配置生成，只有存在可执行项时才渲染，并移除了空菜单入口。
- C-08/C-09：迟交状态使用定时器自动跨过截止时间刷新；撤销使用 DELETE，批量撤销采用 `Promise.allSettled`，能反馈部分失败并在最后刷新服务端数据。
- C-10/C-11：共享壳支持响应式统计卡片、orange tone 和表格横向滚动，窄屏不再通过隐藏操作列丢失下钻入口。
- C-12：`MyClass.vue` 不再是空白页面，保留入口时会展示明确状态和班级管理跳转。

待独立拆分：

- C-04 的参与人/统计接口仍保留原有全量返回契约。本批先修复其外层页面的路由竞态和布局；如果要彻底支持万级参与人，需要同时改造 `RecordsMapper` 的分页统计 SQL、分页响应契约及四个统计页的服务端搜索，这部分不与主列表改动混在同一个小提交中。

验证：

- `pnpm run type-check` 通过。
- `pnpm run build-only` 通过；仅保留项目原有的动态/静态路由分包提示和大 chunk 警告。
- `JAVA_HOME=/usr/lib/jvm/java-11-temurin-jdk mvn -pl problem-service -am -DskipTests compile` 通过。
