# AI 工作流记录

AI 生成的代码被测试或审查发现问题并修正时记录在这里，供 README 的"AI 使用说明"取材。

## 2026-10-31 阶段 0 骨架

- **ESLint 找不到插件**：脚手架用 `--skip-install` 生成后改用 pnpm 安装，`eslint-config-next` 通过 FlatCompat 从项目根目录解析插件，pnpm 默认不提升，`pnpm lint` 报 `eslint-plugin-react-hooks` 找不到。修正：加 `.npmrc` 提升 `*eslint*`、`*prettier*`。
- **Windows 上 standalone 构建失败**：`output: "standalone"` 复制依赖时要创建符号链接，本机 `pnpm build` 报 EPERM。修正：只在 `NEXT_OUTPUT=standalone`（Dockerfile 中设置）时开启。
- **Spotless 检查失败**：AI 在 Windows 上生成的 Java 文件是 CRLF，`.gitattributes` 规定 LF，`mvnw verify` 的格式检查失败。修正：执行 `spotless:apply`；之后由格式化工具统一换行。
- **Maven Wrapper 写入了本机镜像地址**：生成的 `maven-wrapper.properties` 指向本机 settings 里的阿里云镜像，CI 和其他环境未必可用。人工检查时发现，改回 Maven 中央仓库。
- **集成测试卡住**：Testcontainers 需要的 `mysql:8.0` 从本机配置的镜像源下载近 20 分钟未完成。不是代码问题，由开发者用可用的镜像源手动拉取并 tag 回原名。
