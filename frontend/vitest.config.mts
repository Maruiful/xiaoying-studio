import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  // tsconfig 的 jsx: preserve 是给 Next 用的；测试里的 .tsx 用 React 17+ 的自动 JSX 运行时
  esbuild: { jsx: "automatic" },
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  test: {
    // 阶段 0 只有服务端代码的测试；组件测试在需要时改用 jsdom
    environment: "node",
    include: ["src/**/*.test.{ts,tsx}"],
    setupFiles: ["src/test/setup.ts"],
  },
});
