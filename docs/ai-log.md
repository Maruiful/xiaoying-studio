# AI 工作流记录

AI 生成的代码被测试或审查发现问题并修正时记录在这里，供 README 的"AI 使用说明"取材。

## 2026-10-31 阶段 0 骨架

- **ESLint 找不到插件**：脚手架用 `--skip-install` 生成后改用 pnpm 安装，`eslint-config-next` 通过 FlatCompat 从项目根目录解析插件，pnpm 默认不提升，`pnpm lint` 报 `eslint-plugin-react-hooks` 找不到。修正：加 `.npmrc` 提升 `*eslint*`、`*prettier*`。
- **Windows 上 standalone 构建失败**：`output: "standalone"` 复制依赖时要创建符号链接，本机 `pnpm build` 报 EPERM。修正：只在 `NEXT_OUTPUT=standalone`（Dockerfile 中设置）时开启。
- **Spotless 检查失败**：AI 在 Windows 上生成的 Java 文件是 CRLF，`.gitattributes` 规定 LF，`mvnw verify` 的格式检查失败。修正：执行 `spotless:apply`；之后由格式化工具统一换行。
- **Maven Wrapper 写入了本机镜像地址**：生成的 `maven-wrapper.properties` 指向本机 settings 里的阿里云镜像，CI 和其他环境未必可用。人工检查时发现，改回 Maven 中央仓库。
- **集成测试卡住**：Testcontainers 需要的 `mysql:8.0` 从本机配置的镜像源下载近 20 分钟未完成。不是代码问题，由开发者用可用的镜像源手动拉取并 tag 回原名。

## 2026-10-31 阶段 1 模板列表与详情

- **容器没有用上新镜像**：AI 在后台执行 `docker compose up --build -d` 后，curl 检查发现接口仍是 404，对比镜像 ID 发现容器还在跑阶段 0 的旧镜像。改为由开发者执行 `docker compose up --build --force-recreate`，之后构建和启动都交给开发者在自己终端执行。
- **不存在的模板返回 200**：端到端检查发现 `/templates/nope` 内容正确但状态码 200，原因是 `loading.tsx` 的流式渲染先发出了响应头。评估后保留骨架屏，写入 README 已知限制。

## 2026-10-31 阶段 3 素材上传

- **测试 JVM 因内存不足无法启动**：`./mvnw verify` 报 "forked VM terminated"，读 surefire 的 dumpstream 发现是 `Native memory allocation (mmap) failed`，本机 7.7GB 只剩 0.5GB 空闲（Docker 的 WSL 虚拟机占用较多）。不是代码问题：停掉 compose 容器释放内存，并用 `-DargLine="-Xmx512m -XX:+UseSerialGC"` 限制测试 JVM 后通过。

## 2026-11-01 阶段 4 任务后端

- 结果整理需要预置结果图，把封面脚本改名为 `scripts/generate-demo-assets.mjs`，同时生成 `demo/results/*.svg`。封面文件内容没有变化。
- 测试中途 `TaskPipelineTest` 的日志里出现一段 IllegalStateException 堆栈，来自"未预期异常记为内部错误"这个用例故意抛出的异常，被流水线按设计记录为 error 日志，不是测试失败。
- 本机内存仍然紧张，后端测试继续用 `MAVEN_OPTS=-Xmx384m`、`-DargLine="-Xmx512m -XX:+UseSerialGC"` 运行。

## 2026-11-01 阶段 5 任务前端

- **测试里 `React is not defined`**：tsconfig 的 `jsx: preserve` 是给 Next 用的，Vitest 用 esbuild 转换 `.tsx` 时按经典运行时处理。修正：`vitest.config.mts` 设 `esbuild.jsx: "automatic"`。
- **切换用户测试的写法有误**：AI 第一版测试在切换后才卸载用户 A 的组件，卸载前组件又用 `initialData` 重建了 A 的缓存，断言失败。实际页面在切换后按新 userId 渲染，不会这样。改为在一个 hook 里按 `useMe()` 决定跟踪哪个用户的任务，与页面行为一致。
- 连续失败次数没有用 TanStack Query 的 `fetchFailureCount`，因为每次重新请求它都会清零（读 query-core 源码确认），改为在 hook 里自己记录。
- **CI 上切换用户的测试失败（本地通过）**：GitHub Actions 报切换后 `useMe()` 仍是旧用户。原因是 `useApplyUser` 用了 `queryClient.clear()`，把 `me` 查询本身也删了，已挂载的 `useMe()` 还订阅着被删的旧查询，收不到新用户；本地机器慢时碰巧重新渲染才通过。这在页面上也会出现（切换后顶栏和列表可能停在旧用户）。修正：改为 `removeQueries` 删除除 `me` 以外的查询，再对 `me` 做 `setQueryData` 通知所有观察者；测试里切换后的断言改用 `waitFor`，不依赖通知的调度时机。
