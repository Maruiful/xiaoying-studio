# 设计方案

需求原文见 [`要求.md`](../要求.md)，开发规范见 [conventions.md](conventions.md)，参考截图在 [`resources/`](../resources/)。

## 1. 开发方式与阶段

先定接口契约，再按垂直切片迭代。第一步搭一个能跑通整条链路的骨架，之后每个阶段交付一个可演示的完整功能，包括表、接口、页面和测试。

| 阶段 | 内容 | 完成标准 |
|---|---|---|
| 0 骨架 | 目录结构、Spring Boot + Flyway + MySQL、Next.js 空页面、docker-compose、GitHub Actions | `docker compose up --build` 启动三个服务，CI 通过 |
| 1 模板 | 分类与模板种子数据、列表/详情接口、首页、详情页的加载中/不存在/失败状态 | 能浏览模板 |
| 2 身份 | 匿名 cookie、`/api/me`、新建与切换演示用户 | 后续数据都能关联 userId |
| 3 上传 | 上传接口与服务端校验、上传位交互 | 素材存到后端并可访问 |
| 4 任务后端 | 受理、线程池、三步流水线、失败中断、模拟控制、查询、后端测试 | 用 curl 能演示成功、指定阶段失败、多任务 |
| 5 任务前端 | 提交状态、History、轮询生命周期、请求异常与任务失败的区分、刷新恢复、前端测试 | 完整业务闭环 |
| 6 完善 | 账号对话框、小屏、可访问性、视觉细节 | 对照 `resources/` 截图检查 |
| 7 交付 | README、部署与回滚说明、在全新环境验证 compose | 按 R-13 清单逐条核对 |

## 2. 技术版本

| 部分 | 选择 |
|---|---|
| 后端 | Java 17、Spring Boot 3.5.16、MyBatis Spring Boot Starter 3.0.5、Flyway、Lombok |
| 数据库 | MySQL 8.0 |
| 后端测试 | JUnit 5、Testcontainers（MySQL）、Awaitility |
| 前端 | Node 22（本地 20 可用）、pnpm 10、Next.js 15.5、React 19.1、TypeScript 5.9、Tailwind CSS 4、TanStack Query 5、Radix Dialog |
| 前端测试 | Vitest 3.2、Testing Library、MSW 2 |
| 交付 | Docker Compose、GitHub Actions |

2026-10-31 选定成熟的上一代版本（Boot 3.5 而非 4.x，Next 15 而非 16），优先保证生态兼容性。精确版本见 README。

## 3. 整体架构

```text
浏览器 ──同源 /api/*──▶ Next.js（页面 + 转发 Route Handler）──▶ Java（业务、持久化、异步执行）──▶ MySQL
                                                                 └──▶ 本地目录 / 数据卷（素材与结果图）
```

- 浏览器只访问 Next.js 的同源地址。cookie 由 Java 签发，经 Next.js 原样转发，不需要处理 CORS 和跨站 cookie。
- 转发由 `app/api/[...path]/route.ts` 在运行时读取 `BACKEND_URL` 完成。不用 `next.config` 的 rewrites，因为 rewrites 的目标地址在构建时就固定了。
- 素材和结果图由 Java 的 `/api/files/**` 提供，走同一个转发。

## 4. 数据模型

| 表 | 主要字段 |
|---|---|
| `app_user` | id（UUID）、created_at |
| `category` | id、name、sort_order |
| `template` | id（slug）、category_id、title、cover_url、description、required_image_count、accepted_mime_types（JSON）、max_file_size_bytes、sort_order |
| `asset` | id、user_id、template_id、original_name、mime_type、size_bytes、storage_path、created_at |
| `task` | id、user_id、template_id、status、stage、result_url、fail_reason、sim_config（JSON 快照）、created_at、started_at、finished_at、updated_at |
| `task_asset` | task_id、asset_id、slot_index |
| `task_step` | id、task_id、stage、status、started_at、finished_at、message |

`task_step` 每步一条记录，作为"每一步有独立执行边界"（R-05）的证据，测试也直接断言这张表。

## 5. 任务状态与阶段

```text
status:  PENDING ──▶ RUNNING ──▶ SUCCEEDED
            │           └──────▶ FAILED
            └──────────────────▶ FAILED   （线程池拒绝、服务重启）
stage:   null ▶ CHECK_ASSETS ▶ GENERATE ▶ ORGANIZE_RESULT
```

- status 为 PENDING 时 stage 为空。RUNNING 时 stage 是正在执行的步骤。FAILED 时 stage 停在失败的那一步，也就是失败阶段，不再单独设 `fail_stage` 字段。
- SUCCEEDED 和 FAILED 是终态，之后不再改变。所有状态更新都带前置状态条件（见 conventions.md 的 MyBatis 部分）。

## 6. 后台执行

1. `POST /api/tasks` 在一个事务里校验模板、素材数量、是否重复、素材归属和所属模板，然后写入 PENDING 状态的 `task`、`task_asset`，并保存当前模拟配置的快照。
2. 事务提交后（`TaskDispatcher` 注册的 `afterCommit`）把 taskId 提交给有界线程池（AbortPolicy），接口返回 `202 {taskId}`。
3. 工作线程运行 `TaskPipeline`，按 `TaskStage` 的声明顺序执行三个 `TaskStep`，每一步：
   - `TaskStateRecorder.stepStarted`：把 `task` 更新为 `status=RUNNING, stage=当前步骤`，并插入一条 RUNNING 的 `task_step`（同一事务）；
   - `TaskStep.execute`：做这一步的事，按快照中的耗时等待。如果快照指定这一步失败，就抛出 `StepFailedException`；
   - `stepSucceeded`：把这条 `task_step` 更新为 SUCCEEDED。最后一步改用 `taskSucceeded`，`task_step` 和 `task` 一起更新为 SUCCEEDED，并写入 `result_url`。
4. 任何一步失败，`stepFailed` 把这一步的 `task_step` 和 `task` 一起更新为 FAILED，记录 `fail_reason`，后续步骤不再执行。未预期的异常记为"处理时发生内部错误"，不把异常信息暴露给用户。
5. 每个状态更新都带前置状态条件。`stepStarted` 返回 false（任务已不可执行）时停止；终态更新影响行数不对时回滚并停止。
6. 线程池队列满时，任务直接置为 FAILED，原因是"系统繁忙，请稍后重试"。
7. 线程池默认 4 个核心线程、最大 4 个、队列 100：最多同时执行 4 个任务，其余排队。核心线程数和最大线程数取相同值，因为 `ThreadPoolExecutor` 只有在队列满后才会创建超出核心数的线程，两者不同时最大线程数基本用不到。
8. 服务启动时（`UnfinishedTaskCleaner`，`ApplicationReadyEvent`），把残留的 PENDING 和 RUNNING 任务及 RUNNING 的步骤置为 FAILED，原因是"服务重启，处理被中断"。只适用于单实例部署。题目不要求重启后自动恢复执行。

三步的具体内容：

| 步骤 | 做什么 |
|---|---|
| 素材检查 | 先检查再模拟耗时：确认数量、归属、文件在磁盘上存在、类型被模板接受 |
| 模拟生成 | 按配置的耗时等待 |
| 结果整理 | 按模板选一张预置结果图（`demo/results/`），复制到 `results/{taskId}.svg`，结果地址为 `/api/files/results/{taskId}` |

## 7. 模拟控制

- 默认值写在 `application.yml` 的 `app.simulation` 下，可以用环境变量覆盖。
- 开发接口 `GET/PUT /api/dev/simulation` 用来查看和修改每一步的耗时及要失败的阶段，只有 `app.dev-api.enabled=true` 时才注册。
- 配置在任务提交时存成快照，修改只影响之后提交的任务。

## 8. 用户身份

- 需要用户的接口在方法参数里声明 `CurrentUser`，由 `CurrentUserArgumentResolver` 读取 HttpOnly cookie `uid`。cookie 不存在、格式不对或用户不存在时，创建新的 `app_user` 并写入 cookie（SameSite=Lax，有效期 1 年）。
- 不用对所有请求生效的过滤器，是为了让模板、健康检查、静态文件等接口不创建用户：Compose 每 5 秒一次的健康检查和 Server Component 不带 cookie 的请求，都会产生无用的用户记录。
- `GET /api/me` 首次访问会创建用户，这是签发匿名身份的入口，和"查询接口不推动任务"的约束无关。
- `GET /api/me` 返回当前用户。`POST /api/users` 新建匿名用户并切换过去。`POST /api/session/switch {userId}` 切换到已有用户。
- 素材和任务的所有查询都按当前用户过滤。访问别人的资源返回 404。
- 已知限制：userId 本身就是凭证，知道某个 userId 的人就能切换成该用户。演示方案接受这一点，README 里会说明。
- 前端在 localStorage 保存最近用过的 userId，供"切换演示用户"对话框选择。

## 9. 接口一览

| 方法与路径 | 说明 | 成功响应 |
|---|---|---|
| `GET /api/categories` | 分类列表 | 200 `CategoryVO[]` |
| `GET /api/templates?category=` | 模板列表 | 200 `TemplateCardVO[]` |
| `GET /api/templates/{id}` | 模板详情 | 200 `TemplateDetailVO` |
| `POST /api/assets` | multipart：file、templateId | 201 `AssetVO` |
| `GET /api/files/assets/{assetId}` | 读取素材文件，只有主人能访问，其他人 404 | 200 图片 |
| `POST /api/tasks` | `{templateId, assetIds[]}` | 202 `{taskId}` |
| `GET /api/tasks/{id}` | 单个任务 | 200 `TaskVO` |
| `GET /api/tasks?page=&size=` | 当前用户的历史 | 200 `PageVO<TaskVO>` |
| `GET /api/me` | 当前用户 | 200 `UserVO` |
| `POST /api/users` | 新建匿名用户并切换过去 | 201 `UserVO` |
| `POST /api/session/switch` | `{userId}` | 200 `UserVO` |
| `GET /api/files/results/{taskId}` | 成功任务的结果图，只有主人能访问 | 200 SVG |
| `GET/PUT /api/dev/simulation` | 模拟配置，仅开发环境 | 200 `SimulationVO` |

`TaskVO` 示例：

```json
{
  "id": 42,
  "templateId": "demo-postcard",
  "templateTitle": "旅行明信片",
  "status": "FAILED",
  "stage": "GENERATE",
  "resultUrl": null,
  "failReason": "模拟生成失败（模拟失败）",
  "startedAt": "2025-10-31T06:00:00.050Z",
  "createdAt": "2025-10-31T06:00:00.000Z",
  "finishedAt": "2025-10-31T06:00:03.120Z"
}
```

错误响应格式和状态码见 conventions.md。

## 10. 前端页面与状态

| 路由 | 内容 |
|---|---|
| `/` | 分类筛选（`?category=`）和模板卡片网格 |
| `/templates/[id]` | 左栏：详情和上传位；右栏：当前用户的全部历史 |
| `/history` | 独立的历史页，复用 History 组件 |

- 页面和详情信息用 Server Component 渲染。上传区、History 和账号对话框是 Client Component。
- 每个上传位用 reducer 管理状态：empty → uploading → ready 或 error。
- 提交用 `useMutation`。受理成功后，把任务插入历史缓存，清空上传位，按钮恢复可用。
- 每张未结束的任务卡片有自己的 query，`refetchInterval` 在任务进入终态后返回 `false`。
- 查询出错时，卡片保留上次的数据，并显示"状态查询失败 · 重试"。这和任务 FAILED 是两种不同的展示。连续失败 3 次后暂停自动轮询，点重试或 History 的刷新按钮可以恢复。
- 切换用户时，先 `cancelQueries()` 取消进行中的请求，再用 `removeQueries()` 删除除 `me` 以外的查询，最后对 `me` 执行 `setQueryData` 写入新用户，并重置上传位。不用 `clear()`：它会连 `me` 一起删掉，已挂载的 `useMe()` 收不到新用户。query key 都以 userId 开头，旧用户的请求晚返回也不会写进新用户的数据。
- 刷新页面后，按后端分页接口逐页拉取全部历史（每页最多 50 条），未结束的卡片挂载后自动恢复轮询。
- `useMe()` 在水合完成前一律返回"加载中"（`useHydrated`）。详情页的内容在 Suspense 边界里流式到达，`/api/me` 可能先于边界水合返回，直接用缓存渲染会和服务端 HTML 不一致（React #418）。

## 11. 测试重点

**后端**
- 三步的执行顺序。
- 某一步失败后，后续步骤不执行，并记录失败阶段和原因。
- 提交接口立即返回。
- 查询不推进任务，终态不会被改回。
- 服务端校验数量、类型、大小和归属。

**前端**
- 多个任务的状态互不干扰。
- 查询请求异常和任务失败分开展示，查询异常时保留已有数据。
- 任务结束后停止轮询。
- 切换用户后，旧请求不会覆盖新用户的数据。
- 上传位的状态流转。

## 12. 已定事项

- 模板封面和预置结果图由 `scripts/generate-demo-assets.mjs` 生成 SVG，不使用第三方图片，来源写在 README。
