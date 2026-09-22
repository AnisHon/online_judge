# FE-010 系统管理 Review

## 范围

- `oj-vue/src/views/backend/system/notice-manage/NoticeManage.vue`
- `file-manage/FileManage.vue`、`cache-manage/CacheManage.vue`
- `judge-case-log/JudgeCaseLog.vue`、`judge-submit-log/JudgeSubmitLog.vue`
- `oj-vue/src/api/notice/index.ts`、`api/file/*`、`api/cache/index.ts`、`api/judge-log/index.ts`

## 结论

系统页面视觉上已经统一到管理后台风格，但读写边界和数据规模边界还不够严格：公告/文件的过滤仍是当前页过滤，文件下载绕过统一 HTTP 层，缓存页可能直接对 Redis 做全量 key 查询，判题日志的详情请求缺少竞态和错误状态。判题日志尤其要注意敏感信息、管理员权限和错误堆栈的展示边界。

## 问题清单

### FE-010-S-01 [中] 公告和文件搜索只过滤当前页，分页 total 与结果不一致

- 位置：`NoticeManage.vue:185-195,209-219`、`FileManage.vue:128-155`。
- 公告标题/级别、文件名/路径/MD5 都由 `filteredList` 在当前页做过滤，API 查询没有带筛选字段，分页 total 仍是全量总数。
- 影响：管理员搜索不到不在当前页的数据；空状态和分页器给出误导信息。
- 建议：把筛选参数下沉到后端分页接口；如果后端暂不支持，前端必须明确“仅筛选当前页”，不能伪装成全局搜索。

### FE-010-S-02 [高][权限/安全] 文件下载绕过统一 HTTP 层，没有稳定的 token/刷新/错误处理

- 位置：`FileManage.vue:79-91,161-164`、`api/file/index.ts:213-219`。
- 文件管理页调用 `downloadFile`，直接创建 `<a>` 指向 `baseURL/file/download`，没有使用统一 service 的认证、401 refresh、403 处理和请求取消；下载按钮也没有单独的 download 权限指令。
- 影响：token 过期、生产代理或文件接口权限开启时会出现“列表能看但下载失败”；错误 JSON 可能被浏览器当成下载文件；有下载权限要求时普通用户可能仍看见入口。
- 建议：提供统一 binary client，下载失败安全解析 Blob 错误；增加 `content:file:download`（或项目约定的等价权限）并让 UI/后端一致校验。

### FE-010-S-03 [高][性能] 缓存类别下直接全量读取 key，可能阻塞 Redis

- 位置：`CacheManage.vue:135-146`、`api/cache/index.ts:21-23`。
- 进入类别就请求全部 key，接口契约没有 prefix search、分页、limit 或游标。生产 Redis key 数量大时，后端很可能使用 `KEYS` 或大集合扫描，前端也会一次渲染全部键。
- 建议：后端改为 SCAN + cursor/limit，前端做关键词和分页；默认限制返回数量并提示“还有更多”。禁止管理页面触发无界全量扫描。

### FE-010-S-04 [高][竞态] 缓存类别、key 和 value 请求没有 request id，旧响应可覆盖当前选择

- 位置：`CacheManage.vue:148-179`。
- 连续点击不同类别或 key 时，前一个 `getKeyList/getCacheInfo` 返回后仍直接写 `keyList/cacheInfo`。`refreshCurrent` 也会交错执行。
- 影响：标题显示 key B，但内容是 key A；删除后刷新又可能把旧列表写回来。
- 建议：按 prefix/key 分别维护 request sequence 或 AbortController；回调只允许当前选择 token 写入；切换类别时取消旧 value 请求。

### FE-010-S-05 [中][敏感信息] 缓存值读取没有独立的 read permission、脱敏或大小限制

- 位置：`CacheManage.vue:86-97`、`api/cache/index.ts:26-28`。
- 页面只对删除按钮使用 `content:cache:remove`，读取缓存值没有显式权限；textarea 直接加载任意长度原文。Redis 可能包含 token、验证码、会话或业务隐私。
- 建议：拆分 cache:list/cache:read/cache:remove 权限；后端按 key 类型脱敏/禁止敏感前缀，限制单次 value 大小并支持复制/截断查看。

### FE-010-S-06 [中] 缓存、文件、公告 API 既有 resultNotify 又在页面再次提示，失败结果可能被显示为成功

- 位置：`api/cache/index.ts:31-34`、`api/file/fileInfo.ts:22-25`、`NoticeManage.vue:240-267`、`CacheManage.vue:160-173`。
- 底层 helper 已经处理业务布尔结果，页面又无条件 `ElMessage.success`。若接口返回 `false` 但 HTTP 200，页面仍提示删除成功；成功时还会重复通知。
- 建议：API 层返回结构化 mutation result，不在 API 和组件同时 toast；只有明确成功才提示，失败交给统一错误边界。

### FE-010-S-07 [中] 公告编辑详情请求没有 loading/error/竞态保护

- 位置：`NoticeManage.vue:231-238`。
- 弹窗先打开，再 await `getNotice`；没有编辑加载状态、catch 或请求代际。快速点击不同公告时，前一个详情可能晚到并覆盖后一个；请求失败时表单可能保持空白。
- 建议：打开时保存 `editingId`，详情加载期间禁用保存并显示 skeleton；响应只写回相同 ID；失败关闭或显示可重试错误。

### FE-010-S-08 [中] 判题提交详情 drawer 没有错误状态，快速打开不同提交时会串数据

- 位置：`JudgeSubmitLog.vue:166-170`。
- `detail.value` 先清空，随后直接 await；没有 try/catch/error ref，也没有当前 submitId 校验。请求失败时长期显示 skeleton，连续打开 A/B 时 A 的慢响应可覆盖 B。
- 建议：使用 `detailState = idle/loading/success/error` 和 request id；详情接口失败显示稳定的管理员提示及 request id，不暴露原始异常给页面。

### FE-010-S-09 [中] 判题日志列表没有查询快照/取消机制，快速搜索和翻页会旧响应覆盖新结果

- 位置：`JudgeCaseLog.vue:125-144`、`JudgeSubmitLog.vue:145-164`。
- API 直接接收 reactive `query`，每次 search/page 触发新的请求；没有 AbortController、序列号或快照。慢请求返回顺序不确定。
- 建议：请求时复制 query snapshot，维护 monotonically increasing request id；旧响应只允许结束自己的 loading，不允许写列表/total。

### FE-010-S-10 [高][权限] 判题日志页面没有在控件层声明诊断/提交记录读取权限

- 位置：`JudgeCaseLog.vue:10-23,56-60`、`JudgeSubmitLog.vue:10-23,70-71`。
- 页面和查看详情按钮没有 `v-has` 或等价权限判断；如果只依赖动态菜单，直接访问 URL 时会先请求敏感接口。尤其测试用例日志包含 `internalError`，提交日志包含用户代码。
- 建议：为“判题提交记录读取”“判题内部日志读取”“代码详情读取”分别定义权限；页面入口、列表请求、drawer 按钮和后端接口均校验，前端只做体验层过滤。

### FE-010-S-11 [中][CSS/复用] 两个判题日志页面复制了完整 filter/table/drawer CSS，且与 ContestSubPageShell 强耦合

- 位置：`JudgeCaseLog.vue:154-160`、`JudgeSubmitLog.vue:175-180`。
- `.log-panel`、`.filter-heading`、`.filter-grid`、`.status-pill`、`.detail-grid`、`.section-title` 等样式几乎重复；系统模块借用了名字为竞赛的 `ContestSubPageShell`。
- 影响：日志页和竞赛子页的抽屉、表格、状态颜色修改无法集中处理；移动端断点也会逐渐分叉。
- 建议：抽取 `AdminLogPanel`、`AdminLogFilters`、`AdminDetailDrawer`、`AdminStatusPill`，再把 shell 重命名为领域无关的 `AdminPageShell`。

### FE-010-S-12 [中][CSS] 小屏直接隐藏系统表格操作列，下载/删除/查看入口消失

- 位置：`NoticeManage.vue:300`、`FileManage.vue:188` 及日志页面响应式规则。
- 通过隐藏 `.el-table__fixed-right` 处理窄屏，公告查看/编辑/删除、文件下载、判题详情全部可能不可达。
- 建议：提供行操作菜单或 drawer；表格容器支持横向滚动，不能用隐藏列代替响应式交互。

### FE-010-S-13 [低] 字段类型与显示边界不统一

- 位置：`api/file/fileInfo.ts:5-14`、`api/cache/index.ts:10-14`、`api/judge-log/index.ts:31,45`。
- 文件时间声明为 `Date` 但直接展示；缓存过期时间声明 `Date` 却当字符串显示；日志时间允许 string/Date，但没有统一格式化；ID 也没有统一截断组件。
- 建议：API 层统一时间/Long 解析策略，显示层使用 `formatAdminDateTime` 和 `AdminIdCell`，原值放 tooltip。

## 建议验收用例

1. 在公告/文件第二页搜索只存在于第一页/其它页的数据，结果和 total 正确。
2. 连续切换三个缓存类别和五个 key，最终页面只能显示最后一次选择的内容。
3. 大量 Redis key 时接口分页且不会长时间阻塞，value 超大时页面不崩。
4. 过期 token 下载文件能走统一刷新/错误提示；无 download 权限用户没有操作入口。
5. 判题详情快速打开 A/B，模拟 A 慢响应和 B 失败，页面不串数据并显示可重试错误。
