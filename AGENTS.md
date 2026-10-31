# AGENTS.md

本文件写给在本仓库工作的 AI 编程助手。人类开发者请先读 README.md。

## 项目

素材制作网站笔试题。用户浏览模板、上传图片、提交制作任务，Java 后端按三步异步执行模拟生成，前端跟踪任务状态并展示历史结果。

开始任何任务前，先读这三份文档：

- [`要求.md`](要求.md)：题目原文，需求编号 R-01 至 R-14。判断某个功能做不做，以这份为准。
- [`docs/design.md`](docs/design.md)：整体方案、开发阶段、数据表、状态机、接口、前端状态管理。
- [`docs/conventions.md`](docs/conventions.md)：前后端开发规范。写代码前必须遵守。

参考截图在 `resources/`，按主要布局还原，不要求逐像素一致。

## 目录

```text
backend/    Spring Boot 3.5 + MyBatis + Flyway（Maven，Java 17）
frontend/   Next.js 15 App Router + TypeScript（pnpm，Node 22）
docs/       设计与规范
resources/  参考截图
```

## 常用命令

```bash
# 后端（在 backend/ 下）
./mvnw spotless:apply       # 格式化
./mvnw test                 # 只跑单元测试，不需要 Docker
./mvnw verify               # 单元测试 + 集成测试（Testcontainers，需要 Docker）+ 格式检查
./mvnw spring-boot:run      # 本地启动，需要本地 MySQL

# 前端（在 frontend/ 下）
pnpm install
pnpm format                 # Prettier 格式化
pnpm lint && pnpm typecheck && pnpm test && pnpm build
pnpm dev

# 全部服务
docker compose up --build
```

## 必须遵守的约束

以下约束对应题目的核心评分点，违反时宁可停下来询问，也不要绕过。

- **业务逻辑和持久化只在 Java。** Next.js 只负责页面渲染和 `/api/*` 转发。
- **查询接口只读。** 任何 GET 请求都不能推动任务进入下一步。
- **终态不可变。** 任务状态更新必须带前置状态条件，并检查影响行数（conventions.md 2.6）。
- **事务要短。** 事务里不能 sleep 或做耗时 IO，流水线的三步不能包在一个事务里。
- **任务在事务提交后才派发**（`afterCommit`）。
- **请求异常不等于任务失败。** 前端查询报错时保留已有数据，不能把任务显示为失败。
- **前端数据按用户隔离。** 和用户有关的 query key 第一段必须是 userId，切换用户时取消并清空缓存。
- **不实现题外功能。** 积分、计费、删除记录、重新制作、语言切换都不做，除非用户明确要求。
- **不修改已执行过的 Flyway 迁移文件**，表结构变化要新增版本。
- **不提交真实凭据**，示例配置放 `.env.example`。

## 工作方式

- 按 design.md 第 1 节的阶段推进。开始一个阶段前，先和用户确认范围。
- 改代码前先读相关的现有代码，沿用已有写法。规范里没写到的，按所用框架的惯例处理。
- 完成一项改动后，运行受影响部分的检查：后端 `mvn verify`，前端 `pnpm lint && pnpm typecheck && pnpm test`。如实报告结果，失败就贴出输出，不要说成已完成。
- 实现和规范或设计不一致时，先问用户。确认后同步修改对应文档，保证文档和代码一致。
- 不要主动做需求之外的重构或功能，可以提出建议。

## Git

- 只在用户要求时提交，不自行 push。
- 提交信息遵循 Conventional Commits，例如 `feat(task): 实现三步流水线`。
- 单人开发，直接在 `main` 上提交，不建功能分支。提交前本地检查必须通过，保证 `main` 始终能构建。
- 不提交 `node_modules`、`target`、`.next`、`data/`、`.env`。

## AI 工作流记录

题目要求 README 说明 AI 工作流（R-13）。当 AI 生成的代码被测试或审查发现问题并修正时，在 `docs/ai-log.md` 追加一条记录：日期、问题、如何发现、如何修正。每条两三行即可，供最后写 README 时取材。

## 语言

与用户沟通、文档、提交信息和代码注释都用中文。代码标识符、测试方法名用英文。
