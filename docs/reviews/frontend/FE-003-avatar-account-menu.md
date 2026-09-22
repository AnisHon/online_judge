# FE-003 前端头像与账号菜单 Review

## 1. Review 范围

- 头像展示：`oj-vue/src/components/Avatar/Avatar.vue`
- 头像裁剪：`oj-vue/src/components/AvatarCutter/AvatarCutter.vue`
- 账号菜单：`oj-vue/src/components/AccountMenu/AccountMenu.vue`
- 直接依赖：`oj-vue/src/api/file/index.ts`、`useUserStore`、`useMenuStore`、退出登录 API、前台/后台两套布局
- 本次覆盖：缓存与刷新、上传/裁剪状态、权限与退出流程、弹层层级、响应式、暗色模式、CSS 复用、可访问性和重复逻辑
- 本次只做 review，不修改业务代码。

## 2. 结论摘要

这三个组件的基本功能已经具备，但当前头像更新和账号菜单仍有几个会直接影响线上体验的问题：

1. 上传头像后没有调用已有的 `refreshAvatar` 版本机制，固定头像 URL 配合浏览器缓存会导致上传成功后仍显示旧头像。
2. 上传接口和头像展示都把失败状态压得太深：后端返回 `data: null` 时前端仍可能当作成功，图片加载失败后只显示默认图标且没有重试/错误状态。
3. 退出登录没有被账号菜单等待或捕获；网络失败时虽然清掉了本地会话，但可能不会跳转登录页并产生未处理 Promise。
4. 账号菜单用菜单树“是否非空”推断后台权限/按钮显示，权限、路由加载完成度和 UI 显示状态混在一起，容易再次出现普通用户按钮闪现或后台入口判断错误。
5. 账号弹层固定为 `z-index: 3000 !important`，会覆盖 Element Plus 后续创建的 Drawer/Dialog/Popover，和项目此前出现的菜单遮挡抽屉问题直接相关。
6. AvatarCutter 的裁剪状态、图片加载失败、上传中状态没有形成完整状态机；同时 Avatar、Profile、Home、HeaderBar 中存在多套头像尺寸和交互 CSS，后续修改容易出现局部样式崩坏。

## 3. 问题清单

### FE-003-01 [高] 上传头像后没有使头像 URL 失效，成功后可能继续显示旧头像

- 位置：
  - `oj-vue/src/components/Avatar/Avatar.vue:19-26`
  - `oj-vue/src/stores/useUserStore.ts:54-58`
  - `oj-vue/src/views/profile/Profile.vue:325-333`
  - `oj-vue/src/api/file/index.ts:100-109`
- 问题：`Avatar` 会用 `avatarVersions[userId]` 拼接查询参数，`useUserStore` 已经提供了 `refreshAvatar`，但头像上传成功后只调用 `uploadAvatar` 和个人主页 `load()`，没有递增版本号。`load()` 重新获取资料不会改变头像 URL。
- 影响：后端头像已更新，但当前页、首页、题解卡片、后台 Header 等组件仍然使用旧的缓存 URL；后端头像接口还设置了 `max-age=300`，用户可能等待几分钟才看到新头像，表现为“上传成功但没有刷新”。
- 建议：上传成功后立即调用 `userStore.refreshAvatar(userId)`，并把头像更新作为统一的 store 事件；如果允许多人头像实时变化，公共头像继续短缓存即可，但当前用户必须使用版本号立即失效。上传成功不要依赖整页 reload 来解决缓存问题。

### FE-003-02 [高] 上传接口的 null data 会被当作成功结果

- 位置：
  - `oj-vue/src/api/file/index.ts:111-120`
  - `content-service/src/main/java/com/anishan/content/controller/FileController.java:115-125`
  - `content-service/src/main/java/com/anishan/content/service/impl/FileServiceImpl.java:41-59`
  - `oj-vue/src/views/profile/Profile.vue:327-332`
- 问题：前端 `uploadAvatar` 的返回类型声明为 `Promise<string>`，但后端空文件、格式不支持或存储失败时可能返回业务成功包装里的 `null`。Profile 只要请求没有抛异常就提示“头像更换成功”，没有检查返回路径。
- 影响：格式校验/MinIO 保存失败时用户收到成功提示，随后页面刷新仍然是旧头像；问题被误判为浏览器缓存或头像组件问题，后端真正的失败原因也没有被正确传递。
- 建议：后端上传失败返回明确的 4xx/业务错误，不要用 `R.success(null)` 表示失败；前端 API 层把返回值校验为非空字符串，空值转为标准 `ApiError`，页面只在路径有效且版本刷新完成后提示成功。

### FE-003-03 [高] 退出登录请求未被账号菜单等待/捕获，网络异常时可能不跳登录页

- 位置：
  - `oj-vue/src/components/AccountMenu/AccountMenu.vue:38-47`
  - `oj-vue/src/api/auth/authentication.ts:134-142`
- 问题：`handleCommand` 调用 `logout()` 后不等待也不捕获。`logout` 在请求失败时会执行 `finally` 清理 Token 和菜单，但异常会继续向外抛出，后面的 `router.replace({name: 'login'})` 不会执行。
- 影响：断网、服务超时或网关临时不可用时，用户实际上已经被前端清理了会话，但页面仍停在当前页，控制台出现未处理 Promise；再次点击账号菜单或访问动态路由时才可能被动跳转。
- 建议：退出应采用“本地会话清理优先”的流程：无论后端注销接口是否成功，都清理 user/menu/token 并跳登录页；后端请求失败只记录或给低干扰提示，不阻断导航。账号菜单用 `void logout().catch(...)` 或 await 统一收口。

### FE-003-04 [高] 后台入口的显示条件过宽，并把权限和菜单加载状态混在一起

- 位置：
  - `oj-vue/src/components/AccountMenu/AccountMenu.vue:50-58`
  - `oj-vue/src/stores/useMenuStore.ts:7-15,61-64`
  - `oj-vue/src/router/index.ts:270-289`
- 问题：`hasAccessToBackend` 只判断 `menuStore.hasBackendAccess()`，而该方法实际是 `menuTrees.length > 0`。这只能说明接口返回过至少一个树节点，不能表达用户是否有明确的后台入口权限，也不能区分“菜单还没加载完成”和“用户确实没有后台权限”。路由守卫也复用了同一判断。
- 影响：菜单加载时按钮可能先消失后出现；普通用户拥有某个动态菜单节点或接口返回异常结构时，可能看到“进入后台”；反过来，只有固定动态页面权限但没有树节点时，按钮和路由守卫又可能互相矛盾。这与此前普通用户看到后台按钮的故障类型相同。
- 建议：后端返回明确的后台访问权限字符串/能力字段，前端将其作为唯一入口判断；菜单 store 增加 `status: idle/loading/ready/error`，不要用数组长度代替权限和就绪状态。路由守卫和 AccountMenu 共享同一个权限 selector，但仍由后端权限控制最终访问。

### FE-003-05 [高][CSS] 账号弹层固定 z-index 3000，可能覆盖 Drawer/Dialog 和其他全局浮层

- 位置：`oj-vue/src/components/AccountMenu/AccountMenu.vue:73-75`
- 问题：`.account-menu-popper` 使用全局非 scoped 样式并强制 `z-index: 3000 !important`。Element Plus 的 Drawer、Dialog、Select、Tooltip 等浮层通常通过全局 z-index 管理器递增，账号菜单硬编码到 3000 会破坏这个层级顺序。
- 影响：从后台 Header、移动端 Drawer 或资料弹框中打开账号菜单时，菜单可能盖住抽屉/弹窗；后来打开的 Dialog 也可能被账号菜单遮挡。固定值还会影响其它页面中同 class 的 popper。
- 建议：删除组件级硬编码 z-index，优先使用 Element Plus 的层级管理；如果必须定制，只在布局容器级统一配置 popper 层级，并验证 Drawer/Dialog/Tooltip/Dropdown 的打开顺序。不要使用 `!important` 作为层级冲突补丁。

### FE-003-06 [中] 账号菜单触发器不是完整的键盘可操作控件，头像和箭头行为也不一致

- 位置：`oj-vue/src/components/AccountMenu/AccountMenu.vue:2-6,64-70`
- 问题：Dropdown 触发器是普通 `span`，只有 `title`，没有明确 `role`、`tabindex`、`aria-label` 或当前展开状态；头像自身通过 `@click.stop` 直接跳个人主页，箭头/空白区域才打开下拉菜单。
- 影响：键盘用户不能稳定聚焦和打开账号菜单；鼠标点击头像与点击箭头得到不同结果，用户很难理解“头像是个人主页，箭头是菜单”的隐式规则。触发器在移动端 Drawer 中复用时，行为更加不一致。
- 建议：把整个触发区域做成一个明确的按钮/可访问 trigger，头像点击和触发器点击行为统一；如果必须保留头像直达主页，应给出清晰的按钮语义和 aria 文案，并显式处理打开状态。

### FE-003-07 [中] 账号菜单在移动端 Drawer 中没有关闭协同

- 位置：
  - `oj-vue/src/layout/component/menu/Menu.vue:15-20`
  - `oj-vue/src/components/AccountMenu/AccountMenu.vue:38-47`
- 问题：移动导航 Drawer 内重新挂载了 `AccountMenu`，但 AccountMenu 没有向父组件发出命令完成事件，菜单命令只做路由跳转；外层 Drawer 只监听导航菜单的 `select`，不会监听账号菜单命令。
- 影响：在手机端选择个人中心、返回前台或后台后，Drawer 可能仍保持打开，路由已改变但遮罩和导航层还在；如果同时叠加 Dropdown popper，容易出现两个浮层都停留在页面上。
- 建议：为 AccountMenu 增加 `command`/`close` 事件，移动布局收到命令后关闭 Drawer；或由菜单容器统一持有 AccountMenu 的打开/关闭状态，不让组件在不同布局里各自猜测外层状态。

### FE-003-08 [中] Avatar 图片失败后只显示静态 fallback，没有重试和失败可观测状态

- 位置：`oj-vue/src/components/Avatar/Avatar.vue:1-26`
- 问题：头像加载失败时设置 `imageFailed = true`，之后只展示 Element Plus 默认图标；只有 path 变化才通过 watcher 重置失败状态。网络暂时失败、对象存储短暂不可用或缓存命中坏响应时，组件不会自动重试，也没有向调用方报告失败。
- 影响：头像会永久显示默认图标，直到版本号或 userId 改变；用户无法区分“用户没有头像”和“头像服务加载失败”。当头像接口偶发 404/502 时，多个页面会静默降级，增加排障难度。
- 建议：区分 `empty/loading/error` 状态，提供一次低频重试或由父级传入 fallback；对当前用户上传后的版本刷新要清空 error。不要用静态 fallback 掩盖服务端图片异常。

### FE-003-09 [中] AvatarCutter 对图片加载失败没有错误出口，组件可能永久停留在“准备图片”

- 位置：
  - `oj-vue/src/components/AvatarCutter/AvatarCutter.vue:5-22`
  - `oj-vue/src/components/AvatarCutter/AvatarCutter.vue:119-172`
- 问题：预览 `img` 只有 `@load`，没有 `@error`。对象 URL 无法解码、浏览器拒绝图片或文件内容损坏时，`ready` 永远是 false，界面一直显示“正在准备图片…”，没有重选提示。
- 影响：用户会看到不可结束的 loading，确认按钮一直禁用；这不是可恢复的失败体验，也会让用户误以为上传接口或浏览器卡死。
- 建议：添加图片错误状态，撤销对象 URL、清理裁剪状态并给出“图片无法读取，请重新选择”；对 `handleImageLoad` 也补充尺寸校验和异常兜底。

### FE-003-10 [中] AvatarCutter 的文件校验只依赖 MIME，且失败边界与后端契约不一致

- 位置：
  - `oj-vue/src/components/AvatarCutter/AvatarCutter.vue:3,119-146`
  - `content-service/src/main/java/com/anishan/content/service/impl/FileServiceImpl.java:28-37,45-53`
- 问题：前端只根据 `file.type` 接受三种 MIME，浏览器可能给出空 MIME 或不可靠 MIME；后端使用文件内容探测类型，支持的格式集合远大于前端，且前端接受 GIF 后最终统一输出 PNG。两端没有共享明确的大小、格式和转码契约。
- 影响：合法图片可能在前端被误拒；某些伪装文件可能通过前端 MIME 检查后才在后端被静默返回 null；用户看到的“支持 GIF”实际是“可选择 GIF 但最终上传 PNG”，语义不够准确。
- 建议：前端校验只作为体验优化，后端必须用内容探测并返回明确错误；统一定义“输入格式/输出格式/最大尺寸”，前端文案说明 GIF 会被裁剪为静态 PNG，避免把前端校验当安全边界。

### FE-003-11 [中] AvatarCutter 的裁剪状态与父级上传状态断开，上传失败无法在当前上下文重试

- 位置：
  - `oj-vue/src/components/AvatarCutter/AvatarCutter.vue:36-43,225-255`
  - `oj-vue/src/views/profile/Profile.vue:156-165,325-333`
- 问题：裁剪完成后子组件把 `isCropping` 设回 false 并发出文件，父组件立即关闭 Dialog，再异步上传；组件没有收到上传中的状态，也没有处理父级上传失败后的恢复。
- 影响：用户看不到头像上传进度，上传慢时可以重复打开/触发其它操作；上传失败后裁剪结果已经丢失，用户必须从头选择和裁剪，失败体验尤其差。
- 建议：将“裁剪中”和“上传中”建模成父子可协同的状态，父级上传期间禁用关闭/重复提交并保留预览；成功后关闭，失败后保留裁剪结果并允许重试。至少提供 `uploading` prop 或 Promise 返回协议。

### FE-003-12 [中][CSS] 头像尺寸依赖多处页面级覆盖，缺少统一尺寸 token

- 位置：
  - `oj-vue/src/components/Avatar/Avatar.vue:30-33`
  - `oj-vue/src/views/Home.vue:153-156`
  - `oj-vue/src/views/profile/Profile.vue:373-390,807-810`
  - `oj-vue/src/layout-backend/component/HeaderBar/HeaderBar.vue:94-97`
  - `oj-vue/src/components/SolutionCard/SolutionCard.vue:37`
- 问题：Avatar 自身只提供 40px 默认尺寸，首页、主页、后台 Header、题解卡片各自通过 class/deep selector 覆盖根节点和内部 `.avatar__image` 尺寸；头像的圆角、边框、裁剪、hover 也散落在不同页面。
- 影响：修改头像 DOM 结构、尺寸或 Element Plus 版本时，某个页面容易出现头像被挤下去、内层图片不铺满、圆角不一致等局部回归；此前出现的“头像不居中/被挡住”类问题难以一次性修复。
- 建议：Avatar 提供明确的 `size`/`shape`/`interactive` props 或 CSS 自定义属性，页面只选择尺寸 token，不再用多个 `:deep` 选择器重复覆盖内部结构。

### FE-003-13 [低][CSS] 账号菜单的全局 popper 样式和组件 scoped 样式职责混乱

- 位置：`oj-vue/src/components/AccountMenu/AccountMenu.vue:64-75`
- 问题：触发器样式是 scoped，popper 层级又单独写在全局 `<style>` 中；菜单主题、层级、暗色背景实际依赖 Element Plus 全局样式和一个硬编码 class，组件本身无法说明它需要什么宿主层级。
- 影响：在前台导航、移动 Drawer、后台 Header 三处复用时，样式行为不一致；全局 class 可能被其它页面复用或被主题覆盖，暗色模式下难以保证弹层边框/阴影统一。
- 建议：把 popper 的视觉 token 收口到全局主题层，把层级交给浮层管理器；组件只负责触发器和菜单内容，必要的 popper class 使用带业务前缀且不承担全局 z-index 覆盖。

### FE-003-14 [低][CSS/可访问性] 头像和裁剪按钮缺少统一 focus-visible、减少动效和触摸反馈策略

- 位置：
  - `oj-vue/src/components/AccountMenu/AccountMenu.vue:66-69`
  - `oj-vue/src/components/AvatarCutter/AvatarCutter.vue:315-326`
  - `oj-vue/src/views/profile/Profile.vue:395-412`
- 问题：账号头像主要用 hover 放大/滤镜，裁剪按钮和“居中图片”按钮主要用 hover 改色；没有完整的 `:focus-visible` 样式、`prefers-reduced-motion` 处理和触摸端的 active 状态。裁剪区域的拖动也没有键盘替代操作。
- 影响：键盘用户很难判断当前焦点；移动端没有 hover 时反馈较弱；系统开启减少动效后仍会执行缩放、filter 和过渡。
- 建议：抽取统一的可交互控件 focus token，补充 focus-visible/active 状态；对非必要动画响应 `prefers-reduced-motion`；如果要求无障碍完整支持，增加用键盘微调图片位置或至少提供方向按钮。

### FE-003-15 [低] 文件 API 中存在未使用的响应式头像路径和重复的头像 URL 生成逻辑

- 位置：
  - `oj-vue/src/api/file/index.ts:100-109`
  - `oj-vue/src/components/Avatar/Avatar.vue:20-24`
- 问题：`myAvatarPath` 导出一个响应式头像路径，但项目内没有调用方；它与 Avatar 内部再次读取 store、拼接 `getAvatarPath` 的逻辑重复。上传/刷新策略也没有围绕这个导出建立统一入口。
- 影响：维护者容易误以为修改 `myAvatarPath` 就能刷新所有头像，实际页面使用的是 Avatar 自己的计算属性；无用导出增加 API 表面和状态来源。
- 建议：删除未使用导出，或将头像 URL/版本失效统一放入一个 composable/store selector；所有展示组件复用同一份 URL 生成逻辑，并明确公共头像和当前用户头像的缓存策略。

## 4. 做得较好的地方

- `UserProfileVo` 没有返回邮箱、权限、备注等敏感字段，公开资料 DTO 的字段边界是清楚的。
- 头像资源走独立的 `/api/avatar/{userId}` 公共读取路径，后端提供了短缓存、ETag 和 `X-Content-Type-Options`，方向上比每次经过业务 JSON 接口更合理。
- AvatarCutter 已经使用对象 URL、组件卸载时释放 URL、ResizeObserver 和 Canvas 固定输出尺寸，基本替代了外部裁剪依赖。
- 裁剪过程有 `isCropping` 防重复提交，拖动时使用 Pointer Capture，触摸拖动基础行为是完整的。
- AccountMenu 的后台入口和返回前台入口会根据当前路由互斥显示，且后端访问最终仍由动态路由/服务端权限控制，不能只依赖菜单项隐藏。

## 5. 建议修复顺序

1. 先修 FE-003-01、FE-003-02、FE-003-03：解决头像更新假成功、缓存不刷新和退出登录悬挂。
2. 修 FE-003-04、FE-003-05：收紧后台入口权限判断，移除固定 popper z-index，避免菜单再次覆盖抽屉/对话框。
3. 修 FE-003-08、FE-003-09、FE-003-11：补齐图片加载/上传/裁剪状态机，保证失败可恢复。
4. 修 FE-003-06、FE-003-07、FE-003-12、FE-003-14：统一触发器可访问性、移动端关闭协同和头像尺寸 token。
5. 最后清理 FE-003-10、FE-003-13、FE-003-15，并补浏览器回归：头像上传后所有页面立即更新、缓存命中、MinIO 失败、断网退出、Drawer/Dialog/Dropdown 叠加、键盘操作、暗色模式和 320～1200px 宽度。

## 6. 测试与验证

### 本次检查

- 逐行检查了 Avatar、AvatarCutter、AccountMenu 及其 API/store/布局依赖。
- 对照检查了 content-service 的头像上传/读取、缓存头和文件类型探测逻辑。
- 对照检查了动态菜单加载、路由守卫和退出登录清理流程。
- 未修改业务代码，未执行会改变运行环境或数据库的操作。

### 当前验证限制

- 本次没有真实浏览器截图和网络故障注入，弹层层级、移动 Drawer 关闭和缓存刷新需要后续浏览器回归验证。
- 前端已有测试命令此前会在加载 Element Plus CSS 时因 `Unknown file extension ".css"` 失败，现有测试也没有覆盖头像上传/账号菜单流程。

## 7. 本次未修改内容

- 未修改 Avatar、AvatarCutter、AccountMenu、头像 API、store 或后端文件服务。
- 未提交 Git。
- 仅新增本 review 报告。
