# 小影素材工坊

素材制作网站：浏览模板、上传图片、提交制作任务，Java 后端按三步异步执行模拟生成，前端跟踪任务状态并展示历史结果。题目见 [`要求.md`](要求.md)，设计见 [`docs/design.md`](docs/design.md)，开发规范见 [`docs/conventions.md`](docs/conventions.md)。

> 当前进度：阶段 0（项目骨架）。下面标注"待补充"的章节会随开发推进补全。

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
| `BACKEND_URL` | 前端转发的后端地址 | `http://localhost:8080` |
| `NPM_REGISTRY` | 前端镜像构建使用的 npm 源 | `https://registry.npmjs.org` |

## CI

`.github/workflows/ci.yml` 在 push 和 pull request 时运行：前端 lint、格式检查、类型检查、测试、构建；后端 `./mvnw verify`（单元测试、Testcontainers 集成测试、Spotless 检查、打包）。

## 待补充

- 主要接口及字段、任务状态与阶段、后台执行方式
- 演示：成功、指定阶段失败、调整耗时、多任务、刷新恢复
- 用户身份与个人记录的设计及演示
- 已完成／未完成项、已知限制、实际投入
- 部署与回滚
- AI 使用说明与工作流
- 素材来源
