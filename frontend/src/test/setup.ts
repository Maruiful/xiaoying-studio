import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./server";

// 未注册的请求直接报错，避免测试意外访问真实网络
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
