# 开发规范

整体设计见 [design.md](design.md)。规范要和代码保持一致，有变化时同步修改本文件。

## 1. 仓库结构

```text
xiaoying/
├── backend/            Spring Boot（Maven）
├── frontend/           Next.js（pnpm）
├── docs/               设计与规范
├── resources/          参考截图
├── .github/workflows/  CI
├── docker-compose.yml
├── .env.example
└── README.md
```

## 2. 后端

### 2.1 包结构

按业务模块分包，模块内部再按层分文件，不按层建顶级包。

```text
com.xiaoying
├── common/      ErrorCode、BizException、GlobalExceptionHandler、PageVO
├── config/      线程池、Web、配置属性
├── user/        身份过滤器、用户
├── template/    分类、模板
├── asset/       上传、存储、校验
├── task/        提交、查询、状态更新
├── pipeline/    TaskPipeline、TaskStep 及三个实现
└── simulation/  模拟配置与开发接口
```

以 `task/` 为例，一个模块包含：`TaskController`、`TaskService`、`TaskMapper`（简单 SQL 用注解，复杂 SQL 写在 `resources/mapper/TaskMapper.xml`）、`TaskEntity`、`CreateTaskRequest`、`TaskVO`、`TaskConverter`、`TaskStatus`。

### 2.2 对象模型

| 类型 | 后缀 | 用途 | 出现的位置 |
|---|---|---|---|
| 实体 | `XxxEntity` | 一张表一个类，字段和列一一对应。用 Lombok `@Data` | Mapper、Service |
| 请求体 | `XxxRequest` | POST/PUT 的请求体。用 `record` 加 Bean Validation 注解 | Controller → Service |
| 查询参数 | `XxxQuery` | GET 的查询参数，比如分页、筛选。用 `record` | Controller → Service |
| 响应 | `XxxVO` | 返回给前端的数据。用 `record` | Service → Controller |
| 枚举 | 无 | 状态、阶段等 | 各层 |

规则：
- Controller 不接触 Entity，也不直接调用 Mapper。
- Entity 上不加 Jackson 注解，VO 不出现在 Mapper 里。
- 对象转换写在模块内的 `XxxConverter` 静态方法里，手写，不引入 MapStruct。对象数量少，手写更直观。
- 不单独设 DTO 层。Service 之间直接传 Entity、record 或基本类型。

### 2.3 分层职责

- **Controller**：只做参数绑定、`@Valid` 校验、读取当前用户，然后调用一个 Service 方法。不写业务逻辑。
- **Service**：写成具体类，不写接口。负责业务校验、事务、调用 Mapper、组装 VO。只有确实存在多种实现时才定义接口，比如 `TaskStep`、`FileStorage`。
- **Mapper**：只放 SQL，不写逻辑。
- 依赖注入一律用构造器注入（`final` 字段加 Lombok `@RequiredArgsConstructor`），不在字段上写 `@Autowired`。

### 2.4 接口约定

- 路径统一以 `/api` 开头，资源名用复数、小写，多个单词用连字符。
- JSON 字段用 camelCase。时间统一用 ISO-8601 UTC 格式（Java 端是 `Instant`），前端负责转成本地时间显示。
- 成功响应直接返回数据，不再包一层 `{code, data}`。成功或失败由 HTTP 状态码表达，前端和 TanStack Query 本来就按状态码判断，再包一层就有两套判断标准。
- 分页参数 `page`（从 1 开始）、`size`（默认 20，最大 50），返回 `PageVO {items, total, page, size}`。

| 场景 | 状态码 |
|---|---|
| 查询成功 | 200 |
| 上传成功、新建用户 | 201 |
| 任务已受理 | 202 |
| 参数校验失败、素材数量不对 | 400 |
| 资源不存在，或属于其他用户 | 404 |
| 文件过大 | 413 |
| 文件类型不支持 | 415 |
| 未预期的异常 | 500 |

错误响应统一使用以下格式：

```json
{ "code": "ASSET_TOO_LARGE", "message": "图片不能超过 5MB", "details": { "maxBytes": 5242880 } }
```

- `code` 给程序判断，取值固定，不随文案变化。`message` 是给用户看的中文文案。`details` 可选。
- 校验失败时，`details` 是 `{字段名: 错误信息}`。

### 2.5 异常处理

- 所有错误码定义在 `ErrorCode` 枚举里，每个错误码带 HTTP 状态和默认文案，例如 `TEMPLATE_NOT_FOUND(404, "模板不存在")`。
- 业务错误一律抛 `BizException(ErrorCode, [message], [details])`，不用返回 null 或布尔标志来表示失败。
- `GlobalExceptionHandler`（`@RestControllerAdvice`）统一转换：
  - `BizException` 按其错误码转换；
  - `MethodArgumentNotValidException` 转成 `VALIDATION_FAILED`，并附字段明细；
  - `MaxUploadSizeExceededException` 转成 `ASSET_TOO_LARGE`；
  - 其他异常转成 `INTERNAL_ERROR`，详细信息只写日志，不把堆栈或 SQL 返回给前端。
- 流水线某一步失败时抛 `StepFailedException(reason)`，`reason` 是给用户看的文案，会写入 `task.fail_reason`。其他未预期的异常，fail_reason 统一写"处理时发生内部错误"。

### 2.6 数据库与 MyBatis

- 表名和列名用 snake_case，表名用单数，字符集 `utf8mb4`。
- 每张表都有 `created_at`，会更新的表另加 `updated_at`。时间列用 `DATETIME(3)`，存 UTC。
- 主键：`app_user` 用 UUID 字符串，`template` 用可读的 slug，其余表用 `BIGINT AUTO_INCREMENT`。
- 枚举以名称字符串存为 `VARCHAR`，不存数字。
- 索引命名 `idx_表_列`，唯一索引 `uk_表_列`，例如 `idx_task_user_created (user_id, created_at)`。建外键约束。
- 表结构只通过 Flyway 变更，文件放在 `db/migration`。`V1__schema.sql` 建表，`V2__seed.sql` 插入种子数据。已经执行过的迁移文件不能修改，有变化就新增一个版本。
- 不引入 MyBatis-Plus。SQL 按复杂度选择写法：
  - **注解**（`@Select`、`@Insert`、`@Update`）：没有动态条件的简单单表 SQL，例如按 id 查询、单条插入、带前置状态条件的状态更新。
  - **XML**（`resources/mapper/XxxMapper.xml`，namespace 为 Mapper 接口的全限定名）：含动态 SQL（`<if>`、`<where>`、`<foreach>`）、多表关联、需要 `resultMap` 映射，或超过约 10 行的 SQL。例如按分类筛选模板、`IN` 列表校验素材、批量插入 `task_asset`、带模板标题的历史分页查询。
  - 同一个 Mapper 接口可以两种写法并存，但同一条 SQL 只能写在一个地方。不在注解里写 `<script>`，也不使用 Provider 类。
- 开启 `map-underscore-to-camel-case`，不写 `SELECT *`，列名显式列出。自增主键插入时用 `@Options(useGeneratedKeys = true, keyProperty = "id")` 回填。
- **状态更新必须带前置状态条件**，并检查影响的行数：
  ```java
  @Update("""
      UPDATE task SET status = 'SUCCEEDED', result_url = #{resultUrl}, finished_at = #{now}
      WHERE id = #{id} AND status = 'RUNNING'
      """)
  int markSucceeded(@Param("id") long id, @Param("resultUrl") String resultUrl, @Param("now") Instant now);
  ```
  影响 0 行说明状态已经变了，记一条 warn 日志并停止处理，不能覆盖已有状态。

### 2.7 事务与并发

- `@Transactional` 只加在 Service 的 public 方法上。事务里不能有 sleep 或耗时的 IO。
- 流水线每一步的状态更新各自是一个短事务，不能把三步包在一个大事务里。
- 任务派发放在 `TransactionSynchronization.afterCommit` 里执行。
- 线程池的核心线程数、最大线程数和队列长度可以配置。队列满时按 design.md 第 6 节处理。

### 2.8 配置

- 自定义配置统一放在 `app.*` 下，用 `@ConfigurationProperties` 绑定到 record，不到处写 `@Value`。
  ```yaml
  app:
    storage: { dir: ./data, public-base-url: /api/files }
    executor: { core-size: 2, max-size: 4, queue-capacity: 100 }
    simulation: { check-assets-ms: 1000, generate-ms: 3000, organize-result-ms: 1000, fail-stage: NONE }
    dev-api: { enabled: false }
  ```
- 数据库地址和账号用环境变量覆盖。仓库里只放 `.env.example` 中的示例值，不提交真实凭据。
- profile 有三个：默认（本地开发）、`docker`、`test`。

### 2.9 日志

- 用 SLF4J（Lombok `@Slf4j`）。流水线执行时把 taskId 放进 MDC。
- 每一步记录开始、结束（含耗时）和失败（含原因）。
- 不记录 cookie 的值和文件内容。

### 2.10 测试

- 单元测试类命名 `XxxTest`。集成测试类命名 `XxxIT`，用 Testcontainers 启动 MySQL。两类测试都由 `mvn verify` 执行。
- 测试方法用英文命名，`@DisplayName` 写中文说明。
- 测试里把模拟耗时设为 0 或几十毫秒，异步结果用 Awaitility 等待，不用 `Thread.sleep`。

### 2.11 代码格式

- 用 Spotless 加 palantir-java-format。`mvn spotless:apply` 格式化代码，CI 中执行 `spotless:check`。
- 仓库根目录放 `.editorconfig`：UTF-8、LF 换行、行尾不留空格。

## 3. 前端

### 3.1 目录结构

```text
frontend/src/
├── app/                     只放路由、布局、loading/error/not-found 和转发用的 api/[...path]
├── features/                按业务模块拆分
│   ├── templates/           api.ts、types.ts、组件
│   ├── upload/              上传位的 reducer、hooks、组件
│   ├── tasks/               api.ts、types.ts、use-task.ts、组件
│   └── session/             当前用户、切换演示用户
├── components/ui/           与业务无关的基础组件（Button、Dialog 等）
├── lib/                     api-client.ts、query-keys.ts、format.ts
└── test/                    测试工具、MSW handlers
```

`features` 之间可以互相引用。`components/ui` 和 `lib` 不能引用 `features`。

### 3.2 命名

- 文件名用 kebab-case，例如 `task-card.tsx`、`use-task.ts`。
- 组件名用 PascalCase，hook 以 `use` 开头。
- 一个文件只放一个主要组件。除了 Next.js 约定必须默认导出的文件（page、layout 等），其余都用具名导出。

### 3.3 TypeScript

- 开启 `strict` 和 `noUncheckedIndexedAccess`，ESLint 禁止 `any`。
- 后端的枚举在前端用字符串联合类型表示，例如 `type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'`。
- 接口类型放在各模块的 `types.ts`，手写，字段和后端 VO 一一对应。

### 3.4 数据请求

- 组件里不直接调用 `fetch`，统一通过各模块 `api.ts` 中的函数请求。
- `lib/api-client.ts` 提供 `apiFetch<T>()`：
  - 后端返回非 2xx 时抛出 `ApiError {status, code, message, details}`；
  - 网络中断或超时抛出 `NetworkError`。
  这两类错误在界面上属于"请求异常"，和任务的 FAILED 状态是两回事。
- Client Component 请求同源的 `/api/*`。Server Component 直接请求 `BACKEND_URL`，需要身份时手动转发 cookie。
- query key 统一在 `lib/query-keys.ts` 里定义，和当前用户有关的 key 第一段都是 userId：
  ```ts
  export const qk = {
    templates: (category?: string) => ['templates', category ?? 'all'] as const,
    tasks: (userId: string) => [userId, 'tasks'] as const,
    task: (userId: string, id: number) => [userId, 'task', id] as const,
  };
  ```

### 3.5 组件与状态

- 默认写 Server Component。只有需要交互、浏览器 API 或 hooks 的叶子组件才加 `'use client'`。
- 从服务端获取的数据只放在 TanStack Query 的缓存里，不复制到 `useState`。
- 页面局部状态用 `useState` 或 `useReducer`，不引入 Redux 或 Zustand。
- 有多种状态的数据用可辨识联合类型表示，不用多个布尔值拼凑：
  ```ts
  type Slot =
    | { kind: 'empty' }
    | { kind: 'uploading'; file: File; previewUrl: string }
    | { kind: 'ready'; assetId: number; previewUrl: string }
    | { kind: 'error'; file: File; previewUrl: string; message: string };
  ```
- `URL.createObjectURL` 生成的预览地址只用来显示，提交时只传 assetId。替换、移除上传位或组件卸载时，调用 `revokeObjectURL` 释放。

### 3.6 样式

- 用 Tailwind。主题色写成 CSS 变量（深色主题），代码里用语义化的类名，比如 `bg-surface`、`text-muted`，不写死颜色值。
- 不写行内 style，只有动态尺寸可以例外。
- 以桌面端为主。`md` 断点以下，详情页的两栏改为上下排列。

- 有参考截图的页面（`resources/`）沿用其主要布局和信息层级，不要求逐像素一致，图片和文案可以替换。截图中的题外功能（积分、计费、删除账号、语言切换、删除记录）不实现。没有截图的页面自行设计，风格保持一致。

### 3.7 可访问性

- 可点击的元素必须是 `<button>` 或 `<a>`/`<Link>`，不在 div 上绑 onClick。
- 图标按钮必须有 `aria-label`，例如"刷新历史""移除第 1 张图片"。
- 上传位用 `<label>` 包住视觉上隐藏的 `<input type="file">`，键盘可以聚焦和触发。
- 不全局去掉 outline，保留 `focus-visible` 样式。
- 任务卡片的状态区域加 `aria-live="polite"`，上传错误提示用 `role="alert"`。
- 对话框用 Radix Dialog，它会处理焦点限制、Esc 关闭和关闭后焦点归还。

### 3.8 工具与测试

- ESLint 使用 `next/core-web-vitals` 和 `typescript-eslint`，格式化用 Prettier。
- CI 依次执行 `pnpm lint`、`pnpm typecheck`（即 `tsc --noEmit`）、`pnpm test`、`pnpm build`。
- 测试文件和源文件放在一起，命名 `*.test.ts(x)`。
- 接口用 MSW 模拟，不直接 mock `fetch`。涉及轮询的测试用 fake timers。

## 4. Git

- `main` 必须始终能构建通过。每个阶段在 `feat/xxx` 分支上开发，CI 通过后合并。
- 提交信息遵循 Conventional Commits，例如 `feat(task): 实现三步流水线`、`fix(upload): 移除时取消上传请求`、`test`、`docs`、`chore`、`ci`。
- 一个提交只做一件事。前后端需要一起改的同一个功能，可以放在同一个提交里。
- 不提交 `node_modules`、`target`、`.next`、`data/` 和 `.env`。
- 验收版本打 tag `v1.0.0`，README 里写明对应的 commit。

## 5. 素材

- 模板封面和预置结果图自己制作，放在 `backend/src/main/resources/static/demo/`，README 写明来源。不使用来源不明的网络图片。
