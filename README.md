# 小影素材工坊

素材制作网站：浏览模板、上传图片、提交制作任务，Java 后端按三步异步执行模拟生成，前端跟踪任务状态并展示历史结果。题目见 [`要求.md`](要求.md)，设计见 [`docs/design.md`](docs/design.md)，开发规范见 [`docs/conventions.md`](docs/conventions.md)。

> 当前进度：阶段 5（任务前端）。下面标注"待补充"的章节会随开发推进补全。

## 技术版本

| 部分 | 版本 |
|---|---|
| Java | 17（Eclipse Temurin） |
| Spring Boot | 3.5.16 |
| MyBatis Spring Boot Starter | 3.0.5 |
| Flyway | 由 Spring Boot 管理 |
| MySQL | 8.0 |
| 后端测试 | JUnit 5、Testcontainers、Awaitility（版本由 Spring Boot 管理） |
| Node | 22（CI 和 Docker）；本地 20 也可运行 |
| pnpm | 10.28.1 |
| Next.js | 15.5.28（App Router） |
| React | 19.1.0 |
| TypeScript | 5.9 |
| Tailwind CSS | 4 |
| 前端测试 | Vitest 3.2、MSW 2 |

依赖版本锁定在 `backend/pom.xml` 和 `frontend/pnpm-lock.yaml`。

## 项目结构

```text
backend/     Spring Boot 后端（Maven，含 mvnw）
frontend/    Next.js 前端（pnpm）
docs/        设计与开发规范
resources/   参考截图
.github/     CI 配置
docker-compose.yml
.env.example
```

## 快速启动（Docker Compose）

```bash
cp .env.example .env        # 可选，不建 .env 时使用默认值
docker compose up --build
```

- 前端：http://localhost:3000
- 后端：http://localhost:8080/api/health
- MySQL：宿主机端口 3307（避免和本机已有的 MySQL 冲突）

启动顺序：MySQL 健康后启动后端，后端启动时由 Flyway 建表并写入初始数据，后端健康后启动前端。

国内网络构建前端镜像较慢时，在 `.env` 中设置 `NPM_REGISTRY=https://registry.npmmirror.com`。

## 本地开发

**后端**（需要本地 MySQL 8，先创建 `xiaoying` 库和同名用户）

```bash
cd backend
./mvnw spring-boot:run        # 默认连接 localhost:3306，可用 DB_HOST 等环境变量覆盖
./mvnw verify                 # 单元测试 + 集成测试 + 格式检查；集成测试需要 Docker
./mvnw spotless:apply         # 格式化
```

**前端**

```bash
cd frontend
pnpm install
pnpm dev                      # http://localhost:3000，/api/* 转发到 BACKEND_URL（默认 http://localhost:8080）
pnpm lint && pnpm typecheck && pnpm test && pnpm build
```

## 前后端与数据库的连接

浏览器只访问 Next.js 的同源地址。`/api/*` 由 `frontend/src/app/api/[...path]/route.ts` 在运行时转发到 `BACKEND_URL`，业务逻辑和持久化全部在 Java。后端通过 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD` 连接 MySQL。

## 配置项

| 变量 | 作用 | 默认值 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | 数据库地址 | `localhost` / `3306` / `xiaoying` |
| `DB_USER` / `DB_PASSWORD` | 数据库账号（示例值，非真实凭据） | `xiaoying` / `xiaoying` |
| `STORAGE_DIR` | 上传素材和结果图的存储目录（Compose 中为数据卷 `/data`） | `./data` |
| `COOKIE_SECURE` | 身份 cookie 是否加 `Secure`，部署到 https 时设为 `true` | `false` |
| `BACKEND_URL` | 前端转发的后端地址 | `http://localhost:8080` |
| `NPM_REGISTRY` | 前端镜像构建使用的 npm 源 | `https://registry.npmjs.org` |

## CI

`.github/workflows/ci.yml` 在 push 和 pull request 时运行：前端 lint、格式检查、类型检查、测试、构建；后端 `./mvnw verify`（单元测试、Testcontainers 集成测试、Spotless 检查、打包）。

## 模拟数据与素材

- 分类和模板由 Flyway 迁移 `backend/src/main/resources/db/migration/V2__seed.sql` 写入：3 个分类、8 个模板，素材要求分 1、2、3 张，格式和大小上限各不相同。
- 模板封面和模拟结果图都是本项目用脚本生成的 SVG（`scripts/generate-demo-assets.mjs`，渐变加简单图形），没有使用第三方图片。重新生成：`node scripts/generate-demo-assets.mjs`。
- 封面位于 `backend/src/main/resources/demo/covers/`，由后端 `/api/files/demo/**` 提供访问。

## 素材上传

详情页按模板要求的数量显示上传位，每张图片单独上传。

| 接口 | 说明 |
|---|---|
| `POST /api/assets` | multipart 字段 `file`、`templateId`。成功返回 201 `{id, templateId, originalName, mimeType, sizeBytes, url, createdAt}` |
| `GET /api/files/assets/{id}` | 读取素材文件。只有上传者能访问，其他用户返回 404 |

**服务端校验**（顺序如下，以服务端为准）：

| 情况 | 响应 |
|---|---|
| 模板不存在 | 404 `TEMPLATE_NOT_FOUND` |
| 缺少 `file` 字段 | 400 `VALIDATION_FAILED` |
| 文件为空 | 400 `ASSET_EMPTY` |
| 超过模板的大小上限 | 413 `ASSET_TOO_LARGE`，`details.maxBytes` 为上限 |
| 真实类型不在模板允许范围内 | 415 `ASSET_TYPE_NOT_ALLOWED`，`details.acceptedMimeTypes` |

真实类型按文件头字节判断（JPG `FF D8 FF`、PNG `89 50 4E 47…`、WebP `RIFF….WEBP`），不信任扩展名和客户端声明的 Content-Type。被拒绝的上传不写数据库，也不留文件。

**前端**：选择文件后先按模板要求校验类型和大小，不合格直接提示，不发请求。每个上传位支持预览、替换、移除；上传失败时，网络或服务器错误可以重试，文件本身被拒绝则需要换一张。替换或移除时会取消进行中的上传，旧请求晚返回也不会覆盖新状态。所有上传位都成功后，"开始制作"按钮才可点击。

**存储**：文件保存在 `${STORAGE_DIR}/assets/{userId}/{uuid}.{ext}`，Compose 中是数据卷 `backend-data`，容器重建后保留。

## 制作任务与后台执行

### 状态与阶段

| 字段 | 取值 |
|---|---|
| `status`（整体状态） | `PENDING` 等待处理 → `RUNNING` 处理中 → `SUCCEEDED` 成功 / `FAILED` 失败 |
| `stage`（当前阶段） | `CHECK_ASSETS` 素材检查 → `GENERATE` 模拟生成 → `ORGANIZE_RESULT` 结果整理；`PENDING` 时为 `null`；失败时停在失败的那一步 |

`SUCCEEDED` 和 `FAILED` 是终态，不会再变。

### 执行方式

1. `POST /api/tasks` 在一个短事务里校验模板、素材数量、素材归属和所属模板，写入 `PENDING` 任务和当前模拟配置的快照。
2. 事务提交后（`afterCommit`）把任务 ID 交给有界线程池（默认 4 个线程、队列 100，最多同时执行 4 个任务，其余排队），接口立即返回 `202 {taskId}`。关闭页面不影响任务。
3. 工作线程按顺序执行三步。每一步的边界：
   - 开始：`task` 更新为 `RUNNING` + 当前阶段，插入一条 `task_step`（RUNNING）；
   - 执行：按快照中的耗时等待，指定失败时抛出失败；
   - 结束：`task_step` 更新为 SUCCEEDED 或 FAILED。
   每次状态变化是一个独立的短事务，`task` 和 `task_step` 一起更新。
4. 某一步失败后不再执行后续步骤，`task` 记为 `FAILED`，`stage` 停在该步，`fail_reason` 写入原因。
5. 结果整理复制一张预置结果图到 `${STORAGE_DIR}/results/{taskId}.svg`，`task` 记为 `SUCCEEDED` 并写入 `result_url`。

**保证终态不被覆盖**：所有状态更新 SQL 都带前置状态条件（如 `WHERE id = ? AND status = 'RUNNING' AND stage = ?`）并检查影响行数，不符合时停止执行。查询接口只读数据库，不会推动任务。

**异常情况**：线程池队列满时任务直接记为失败（"系统繁忙"）；服务启动时把上次进程留下的 `PENDING`/`RUNNING` 任务记为失败（"服务重启，处理被中断"）。

### 接口

| 接口 | 说明 |
|---|---|
| `POST /api/tasks` | 请求 `{templateId, assetIds[]}`（按上传位顺序）。返回 202 `{taskId}` |
| `GET /api/tasks/{id}` | 返回 `TaskVO`，其他用户的任务返回 404 |
| `GET /api/tasks?page=1&size=20` | 当前用户的历史，按创建时间倒序。返回 `{items, total, page, size}`，`size` 最大 50 |
| `GET /api/files/results/{taskId}` | 成功任务的结果图（SVG），只有任务主人能访问 |

`TaskVO` 字段：`id`、`templateId`、`templateTitle`、`status`、`stage`、`resultUrl`（仅成功时）、`failReason`（仅失败时）、`createdAt`、`startedAt`、`finishedAt`。

提交校验失败：数量不对 400 `TASK_ASSET_COUNT_MISMATCH`；素材重复、不存在、属于他人或属于其他模板 400 `TASK_ASSET_INVALID`；模板不存在 404 `TEMPLATE_NOT_FOUND`。

### 模拟控制（调整耗时、指定阶段失败）

启动默认值由环境变量设置（见 `.env.example`）：`SIM_CHECK_ASSETS_MS`、`SIM_GENERATE_MS`、`SIM_ORGANIZE_RESULT_MS`、`SIM_FAIL_STAGE`（`NONE` / `CHECK_ASSETS` / `GENERATE` / `ORGANIZE_RESULT`）。

运行时通过开发接口修改，只在 `DEV_API_ENABLED=true` 时开放（Compose 默认开启，正式部署应关闭）：

```bash
# 查看当前配置
curl http://localhost:3000/api/dev/simulation

# 让"模拟生成"失败（可重复触发，直到改回 NONE）
curl -X PUT http://localhost:3000/api/dev/simulation -H 'content-type: application/json' -d '{"failStage":"GENERATE"}'

# 把模拟生成调到 10 秒，便于观察处理中和多任务
curl -X PUT http://localhost:3000/api/dev/simulation -H 'content-type: application/json' -d '{"generateMs":10000}'

# 恢复为全部成功
curl -X PUT http://localhost:3000/api/dev/simulation -H 'content-type: application/json' -d '{"failStage":"NONE","generateMs":3000}'
```

字段都是可选的，未给出的沿用当前值；耗时范围 0～60000 毫秒。配置在任务提交时固定为快照，修改只影响之后提交的任务。配置只保存在内存中，重启后恢复为环境变量的值。

## 前端：提交、制作记录与状态跟踪

制作记录显示在详情页右栏，也可以从顶栏"制作记录"进入独立的 `/history` 页。两处用的是同一个组件。

**界面上区分的状态**

| 状态 | 表现 |
|---|---|
| 素材上传中 | 上传位显示"上传中…"，提交按钮不可用 |
| 提交请求中 | 按钮显示"提交中…"，上传位暂时锁定 |
| 已受理 | 立即在制作记录里插入一张"等待处理"的卡片，上传位清空，按钮恢复可用，可以接着提交 |
| 等待处理／处理中 | 卡片显示整体状态和三个阶段的进度，阶段完全来自后端记录 |
| 成功 | 显示结果图，点击在新窗口打开 |
| 任务失败 | 红色提示：失败阶段和原因（来自后端 `failReason`） |
| 状态查询失败 | 灰色提示"状态查询失败"和"重试"按钮，卡片保留上次的数据，**不会**显示成任务失败 |

**状态跟踪方式**（TanStack Query 轮询）

- 每张未结束的任务卡片有自己的查询，每秒查一次 `GET /api/tasks/{id}`。任务进入成功或失败后停止。没有未结束任务时不发任何轮询请求。
- 查询出错时保留已有数据。同一个任务连续失败 3 次后暂停自动轮询，点卡片上的"重试"或历史标题旁的刷新按钮可以恢复。
- 历史列表和单任务查询是两个请求。用列表数据更新卡片时，只接受进度不落后的数据，已结束的任务不会被较旧的列表响应改回处理中。
- query key 都以 userId 开头。切换用户时先取消进行中的请求，再清空缓存，上传位也会重置（已上传的素材属于上一个用户）。旧用户晚返回的请求不会出现在新用户的界面上。
- 刷新页面后从后端重新拉取历史，未结束的任务自动接着轮询。

**演示**

1. 按上面"模拟控制"把 `generateMs` 调到 8000，连续提交 3 个任务，可以看到几张卡片各自推进。
2. 设 `failStage` 为 `GENERATE` 再提交，卡片显示"模拟生成失败"。
3. 任务处理中时刷新页面，卡片恢复并继续更新。
4. 任务处理中时执行 `docker compose stop backend`，卡片显示"状态查询失败"，数据保留；`docker compose start backend` 后点"重试"，状态继续更新。

## 用户身份与个人记录

**方案**：匿名用户加 HttpOnly cookie，不需要注册登录。

- 第一次打开网站时，前端调用 `GET /api/me`，后端创建一个匿名用户（随机 UUID），通过 `Set-Cookie: uid=...; HttpOnly; SameSite=Lax` 写入浏览器。之后的请求都带着这个 cookie，后端据此识别用户。
- 只有需要用户的接口才会创建用户。模板、健康检查等接口不会。
- 素材和任务记录都按用户隔离。

**演示**：点击顶栏右侧的用户 ID 打开"账号设置"。

- **新建匿名用户**：切换成一个全新的用户，制作记录为空。
- **切换**：列表里是这个浏览器用过的用户（保存在 localStorage），点击切回去，能看到该用户之前的记录。
- 也可以开一个隐私窗口，它没有 cookie，会成为另一个用户。

| 接口 | 说明 |
|---|---|
| `GET /api/me` | 返回当前用户 `{id, createdAt}`；没有有效 cookie 时创建用户 |
| `POST /api/users` | 新建匿名用户并切换过去，201 |
| `POST /api/session/switch` | 请求体 `{userId}`，切换到已有用户；不存在返回 404 `USER_NOT_FOUND`，格式错误返回 400 |

**限制**：userId 本身就是凭证，知道某个 userId 的人就能切换成该用户。这是演示方案，没有密码或签名。正式产品需要换成登录体系，或者给 cookie 加签名并去掉"切换到任意用户"的接口。

## 已知限制

- 访问不存在的模板（如 `/templates/nope`）时页面显示"模板不存在"，但 HTTP 状态码是 200。原因是详情页的 `loading.tsx` 骨架屏会先以 200 流式发出，之后才判断出模板不存在；Next.js 会为该页面加上 `noindex`。接口 `/api/templates/nope` 本身返回 404。

## 待补充

- 已完成／未完成项、已知限制、实际投入
- 部署与回滚
- AI 使用说明与工作流
