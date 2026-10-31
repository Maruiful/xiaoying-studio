import { setupServer } from "msw/node";

// 各测试文件用 server.use(...) 注册自己需要的接口
export const server = setupServer();
