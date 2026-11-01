# 小影素材工坊

素材制作网站：浏览模板、上传图片、提交制作任务，Java 后端按三步异步执行模拟生成，前端跟踪任务状态并展示历史结果。题目见 [`要求.md`](要求.md)，设计见 [`docs/design.md`](docs/design.md)，开发规范见 [`docs/conventions.md`](docs/conventions.md)。

> **验收基线**：tag `v1.0` 对应首次完成版本，本次复查修复在后续提交中；交付时以 `main` 的最新 commit 为准。完成情况见[已完成与未完成](#已完成与未完成)。

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
| `COOKIE_SECURE` | 身份 cookie 是否加 `Secure`，部署到 https 时设为 `true`（Compose 中已传给后端） | `false` |
| `BACKEND_URL` | 前端转发的后端地址 | `http://localhost:8080` |
| `NPM_REGISTRY` | 前端镜像构建使用的 npm 源 | `https://registry.npmjs.org` |

## CI

`.github/workflows/ci.yml` 在 push 和 pull request 时运行：前端 lint、格式检查、类型检查、测试、构建；后端 `./mvnw verify`（单元测试、Testcontainers 集成测试、Spotless 检查、打包）。

## 模拟数据与素材

- 分类和模板由 Flyway 迁移 `backend/src/main/resources/db/migration/V2__seed.sql` 写入：3 个分类、8 个模板，素材要求分 1、2、3 张，格式和大小上限各不相同。
- 模板封面和模拟结果图都是本项目用脚本生成的 SVG（`scripts/generate-demo-assets.mjs`，渐变加简单图形），没有使用第三方图片。重新生成：`node scripts/generate-demo-assets.mjs`。
- 封面位于 `backend/src/main/resources/demo/covers/`，由后端 `/api/files/demo/**` 提供访问。

### 分类与模板接口

以下接口无需用户身份，数据由 Java 从 MySQL 的种子记录读取。图片地址可通过同源 `/api/files/demo/**` 访问。

| 接口 | 请求 | 成功响应 |
|---|---|---|
| `GET /api/categories` | 无参数 | 分类数组，每项为 `{id, name}` |
| `GET /api/templates` | 可选查询参数 `category`（分类 ID）；不传则返回全部 | 模板卡片数组，每项为 `{id, categoryId, title, coverUrl, requiredImageCount}` |
| `GET /api/templates/{id}` | 路径参数 `id`（模板 ID） | `{id, categoryId, categoryName, title, coverUrl, description, requiredImageCount, acceptedMimeTypes, maxFileSizeBytes}`；不存在返回 404 `TEMPLATE_NOT_FOUND` |

`acceptedMimeTypes` 是允许的 MIME 类型字符串数组，`maxFileSizeBytes` 是单张图片的字节上限。

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
| `GET /api/tasks?page=1&size=20` | 当前用户的历史，按创建时间倒序。返回 `{items, total, page, size}`，`size` 最大 50；前端逐页读取全部历史 |
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
| 等待处理／处理中 | 卡片预览区显示当前阶段（如"模拟生成中…"），右上角是整体状态，下方是三个阶段的进度条；阶段完全来自后端记录 |
| 成功 | 预览区显示结果图，点击在新窗口打开 |
| 任务失败 | 预览区显示红色的失败阶段和原因（来自后端 `failReason`） |
| 状态查询失败 | 卡片底部灰色提示"状态查询失败"和"重试"按钮，卡片保留上次的数据，**不会**显示成任务失败 |

任务状态变化时（等待处理、处理中、已完成、制作失败）读屏会播报一次，阶段切换不重复播报。

**状态跟踪方式**（TanStack Query 轮询）

- 每张未结束的任务卡片有自己的查询，每秒查一次 `GET /api/tasks/{id}`。任务进入成功或失败后停止。没有未结束任务时不发任何轮询请求。
- 查询出错时保留已有数据。同一个任务连续失败 3 次后暂停自动轮询，点卡片上的"重试"或历史标题旁的刷新按钮可以恢复。
- 历史列表和单任务查询是两个请求。用列表数据更新卡片时，只接受进度不落后的数据，已结束的任务不会被较旧的列表响应改回处理中。
- query key 都以 userId 开头。切换用户时先取消进行中的请求，再删除除当前用户（`me`）以外的全部缓存，然后写入新用户；上传位也会重置（已上传的素材属于上一个用户）。旧用户晚返回的请求只会写进已删除的旧 key，不会出现在新用户的界面上。
- 刷新页面后从后端重新拉取历史，未结束的任务自动接着轮询。

**演示**

0. 默认配置下提交一个任务，约 5 秒后卡片依次经过三个阶段，显示结果图（演示成功）。
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

## 测试

| 位置 | 覆盖的核心行为 |
|---|---|
| `backend/.../task/TaskFlowIT.java`（Testcontainers + MySQL） | 提交立即返回；三步按顺序执行并成功；指定阶段失败后停止后续步骤并记录阶段和原因；配置快照；查询不推动任务；终态不再变化；多任务并发互不影响；按用户隔离；启动时清理中断的任务 |
| `backend/.../pipeline/TaskPipelineTest.java`、`task/TaskDispatcherTest.java` | 流水线步骤顺序、前置状态条件不满足时停止、未预期异常记为失败；线程池队列满时记为"系统繁忙" |
| `backend/.../asset/AssetUploadIT.java`、`ImageTypeTest.java` | 上传校验（空文件、大小、按文件头判断真实类型）、素材归属 |
| `frontend/src/features/tasks/use-tasks.test.tsx`（Vitest + MSW） | 终态后停止轮询；查询失败保留数据、不当成任务失败、连续失败后暂停、重试可恢复；多个任务互不干扰；受理后立即出现在历史；切换用户后旧请求不写进新用户的缓存 |

后端 58 个（含参数化展开）、前端 48 个测试用例。运行方式见上面[本地开发](#本地开发)，CI 在每次 push 时运行全部测试。

## 部署与回滚

本项目只提供 Compose 部署，没有接入发布平台。以下步骤在部署机（装有 Docker 的服务器）上执行。

**部署**

```bash
git clone <仓库地址> xiaoying && cd xiaoying
git checkout <要部署的 commit>
cp .env.example .env
# 修改 .env：DB_PASSWORD、DB_ROOT_PASSWORD 改为强密码；DEV_API_ENABLED=false 关闭模拟控制接口；https 部署设 COOKIE_SECURE=true
docker compose up --build -d
docker compose ps              # 三个服务都是 healthy / running 后即可访问
```

- 后端启动时由 Flyway 执行尚未执行过的迁移，不需要手动建表。
- MySQL 只在数据卷为空时按 `.env` 初始化账号。之后再改 `.env` 里的密码不会改到数据库，需要进 MySQL 用 `ALTER USER` 修改。
- 部署到 https 时，在 `.env` 里设 `COOKIE_SECURE=true`，身份 cookie 会加上 `Secure`。

**升级**：`git pull`（或 checkout 新的 commit）后再执行 `docker compose up --build -d`。数据卷 `mysql-data`、`backend-data` 保留。升级前先按下面的方法备份。

**备份**

```bash
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" xiaoying' > backup-$(date +%Y%m%d%H%M).sql
docker run --rm -v xiaoying_backend-data:/data -v "$PWD":/backup alpine tar czf /backup/files-$(date +%Y%m%d%H%M).tgz -C /data .
```

数据卷名前缀取决于项目目录名，可用 `docker volume ls` 确认。

**回滚**

1. `git checkout <上一个正常的 commit>`，执行 `docker compose up --build -d`。
2. 如果两个版本之间没有新增 Flyway 迁移，到这一步就完成了。
3. 如果新版本加了迁移：Flyway 只向前执行，不会撤销已执行的迁移。旧版本启动时会忽略它不认识的新版本迁移（Flyway 默认行为），但表结构已经是新的，旧代码能否正常读写取决于改了什么。没有把握时，用升级前的备份恢复数据库：

```bash
docker compose stop backend frontend
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE xiaoying; CREATE DATABASE xiaoying CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"'
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xiaoying' < backup-XXXX.sql
docker compose up -d
```

恢复会丢掉升级之后产生的数据。迁移文件执行过后不再修改，表结构变化都通过新增版本完成，所以每个 commit 对应的表结构是确定的。

## 已完成与未完成

**已完成**（对应 [`要求.md`](要求.md) 的需求编号）

| 需求 | 实现位置 |
|---|---|
| R-01 技术栈与职责 | 业务和持久化都在 Java，Next.js 只渲染页面和转发 `/api/*` |
| R-02 列表与模拟数据 | Flyway 种子数据、`/api/categories`、`/api/templates?category=` |
| R-03 详情、上传与校验 | 见[素材上传](#素材上传)；加载中、不存在、请求失败、空分类都有界面反馈 |
| R-04 受理与异步执行 | `afterCommit` 后派发到有界线程池，接口返回 202 |
| R-05 多步骤状态与查询 | `task` + `task_step` 表，每步独立的短事务；查询只读 |
| R-06 失败与模拟控制 | 环境变量和 `/api/dev/simulation`，见[模拟控制](#模拟控制调整耗时指定阶段失败) |
| R-07 状态表达与请求异常 | 见[前端：提交、制作记录与状态跟踪](#前端提交制作记录与状态跟踪) |
| R-08 多任务与查询生命周期 | 每个任务一个查询，终态停止；切换用户时取消请求并清理缓存 |
| R-09 刷新恢复与结果记录 | 历史从后端拉取，未结束的任务自动接着轮询 |
| R-10 用户身份与个人记录 | 见[用户身份与个人记录](#用户身份与个人记录) |
| R-11 参考界面与交互 | 按 `resources/` 三张截图调整布局；小屏单列可操作；键盘焦点、对话框焦点、读屏标签与播报、文字对比度（均达 WCAG AA 4.5:1） |
| R-12 CI 与关键测试 | 见 [CI](#ci) 和[测试](#测试) |
| R-13 提交材料 | 本 README |
| R-14 容器交付 | `docker compose up --build` 启动三个服务和初始化数据 |

**未实现，按题目说明不在范围内**：积分和计费、删除记录、重新制作、语言切换、真实图片处理、模板管理后台。参考截图中的这些入口都没有做。

**边界与取舍**

- **服务重启后不恢复执行**：任务只在内存线程池里执行。后端启动时会把上次进程留下的 `PENDING`／`RUNNING` 任务记为失败，原因是"服务重启，处理被中断"，不会一直显示处理中。没有做断点续跑：三步都是模拟的，续跑需要为每一步设计幂等和恢复点，收益不大。
- **提交不做幂等**：重复点击由前端拦截（提交中按钮不可用）。网络超时后用户再提交一次，会产生两个任务。要做幂等可以让前端生成 `Idempotency-Key`，后端加唯一索引，这次没有做。
- **不自动重试失败任务**：失败是模拟出来的，题目也不要求重新执行。
- **单实例**：线程池和模拟配置都在进程内，多实例部署需要换成数据库或消息队列来分配任务。

## 已知限制

- 访问不存在的模板（如 `/templates/nope`）时页面显示"模板不存在"，但 HTTP 状态码是 200。原因是详情页的 `loading.tsx` 骨架屏会先以 200 流式发出，之后才判断出模板不存在；Next.js 会为该页面加上 `noindex`。接口 `/api/templates/nope` 本身返回 404。
- userId 本身就是凭证，见[用户身份](#用户身份与个人记录)的限制说明。
- 边框色和面板背景的对比度是 1.27:1，低于 WCAG 对界面组件边界的 3:1。上传位靠更亮的底色和文字识别，边框只是装饰。
- 模拟配置只保存在内存中，后端重启后恢复为环境变量的值。

## 实际投入

约 10 小时，分布在 2026-10-31 下午至 2026-11-01 下午，共 7 段。

统计方法：前 6 段按 AI 编程工具会话记录里每条消息的时间戳，相邻两条间隔小于 30 分钟的算作连续工作，累加得到 8.9 小时（间隔阈值取 10 分钟时是 7.5 小时）；本次复查与修复约 1 小时。会话之外的时间没有算进去，例如开始前读题，以及在浏览器里手动检查页面的部分时间。

| 时间段 | 时长 | 内容 |
|---|---|---|
| 10-31 14:12–17:52 | 约 3 小时 | 需求整理、设计与规范文档、阶段 0 骨架 |
| 10-31 22:12–11-01 01:25 | 约 3.2 小时 | 阶段 1～5：模板、身份、上传、任务后端、任务前端 |
| 11-01 11:27–15:47 | 约 2.5 小时 | CI 问题修复、阶段 6 界面与可访问性、阶段 7 交付文档 |
| 11-01 17:02–18:07 | 约 1 小时 | 对照 R-01～R-14 复查、修复线程池拒绝路径与历史分页、补回归测试和接口文档、核验新镜像 |

各阶段的具体内容见提交历史。

## AI 使用说明

本项目使用了 AI 编程工具（Claude Code）。代码大部分由 AI 生成，需求拆解、方案取舍、验收和问题判断由我负责。

**工作流**

1. **先写文档，再写代码**：开始编码前，先和 AI 一起把题目整理成 [`docs/design.md`](docs/design.md)（阶段划分、表结构、状态机、接口、前端状态管理）和 [`docs/conventions.md`](docs/conventions.md)（事务边界、状态更新必须带前置条件、query key 以 userId 开头等）。[`AGENTS.md`](AGENTS.md) 列出不能违反的约束，AI 每次会话开始时都会读到。这样上下文写在仓库里，不依赖聊天记录。
2. **按阶段拆分**：design.md 第 1 节把工作分成 0～7 共 8 个阶段，每个阶段交付一个可演示的切片（表、接口、页面、测试）。开始一个阶段前先确认范围，题外功能（积分、删除、重新制作）一律不做。
3. **生成与修改**：AI 先读相关的现有代码再改，沿用已有写法。和设计不一致时先停下来问我，确认后同步更新文档。
4. **验证**：每次改动后运行受影响部分的检查（后端 `./mvnw verify`，前端 `lint`、`typecheck`、`test`），如实报告结果。界面调整用 Playwright 截图和参考图对比；构建和启动由我在自己的终端执行，AI 截图前先确认容器里是新代码。
5. **记录修正**：AI 生成的代码被测试、CI 或我发现问题时，记到 [`docs/ai-log.md`](docs/ai-log.md)，写明问题、怎么发现的、怎么改的。

**例子：切换用户后界面停在旧用户**

阶段 5 的前端测试在本地全部通过，push 后 GitHub Actions 上"切换用户"的用例失败了：切换后 `useMe()` 返回的仍是旧用户。

- **排查**：AI 生成的切换逻辑用了 `queryClient.clear()`。它把 `me` 查询本身也删掉了，已经挂载的 `useMe()` 还订阅着被删除的旧查询，收不到新用户。本地机器慢，组件碰巧重新渲染了，所以本地能通过。这个问题在真实页面上同样会出现。
- **修正**：改为 `removeQueries` 删除 `me` 以外的查询，再对 `me` 执行 `setQueryData`，让所有订阅者都收到通知。测试中切换后的断言改用 `waitFor`，不依赖通知的调度时机。同时更新了 design.md 的对应描述。
- **收获**：本地通过不代表正确，时序相关的问题要在不同速度的环境下验证。之后排查详情页偶发的 React #418 水合报错时，也是先用 Playwright 改变 `/api/me` 的响应时间、统计复现次数来确认原因，再动手修改。

其余记录见 [`docs/ai-log.md`](docs/ai-log.md)。

## 第三方代码与素材来源

- **脚手架**：前端由 `create-next-app` 生成后修改（ESLint、Tailwind、TypeScript 配置）；后端的 `mvnw`、`mvnw.cmd` 是 Maven Wrapper 官方脚本，项目结构按 Spring Boot 惯例手写。
- **开源依赖**：版本见[技术版本](#技术版本)和锁文件。界面组件中只有账号对话框用了 `@radix-ui/react-dialog`（处理焦点限制、Esc 关闭和读屏属性），其余都是自己写的 Tailwind 组件。
- **图片素材**：模板封面和模拟结果图都由本项目的 `scripts/generate-demo-assets.mjs` 生成（SVG 渐变和简单图形），没有使用第三方图片。
- **参考截图**：`resources/` 下的三张截图随题目提供，只用来参考布局。

