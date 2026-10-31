import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@/test/server";
import { ApiError, apiFetch, NetworkError } from "./api-client";

const URL = "http://backend.test/api/thing";

describe("apiFetch", () => {
  it("2xx 时返回解析后的 JSON", async () => {
    server.use(http.get(URL, () => HttpResponse.json({ id: 1 })));

    await expect(apiFetch(URL)).resolves.toEqual({ id: 1 });
  });

  it("后端返回统一错误格式时抛出带 code 的 ApiError", async () => {
    server.use(
      http.get(URL, () =>
        HttpResponse.json(
          {
            code: "VALIDATION_FAILED",
            message: "请求参数不合法",
            details: { templateId: "不能为空" },
          },
          { status: 400 },
        ),
      ),
    );

    const error = await apiFetch(URL).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      code: "VALIDATION_FAILED",
      message: "请求参数不合法",
      details: { templateId: "不能为空" },
    });
  });

  it("错误响应不是统一格式时仍抛出 ApiError", async () => {
    server.use(
      http.get(URL, () => new HttpResponse("Bad Gateway", { status: 503 })),
    );

    const error = await apiFetch(URL).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 503, code: "HTTP_ERROR" });
  });

  it("拿不到响应时抛出 NetworkError，而不是 ApiError", async () => {
    server.use(http.get(URL, () => HttpResponse.error()));

    const error = await apiFetch(URL).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(NetworkError);
    expect(error).not.toBeInstanceOf(ApiError);
  });

  it("主动取消时抛出 AbortError，不包装成 NetworkError", async () => {
    server.use(http.get(URL, () => HttpResponse.json({})));
    const controller = new AbortController();
    controller.abort();

    const error = await apiFetch(URL, { signal: controller.signal }).catch(
      (e: unknown) => e,
    );

    expect(error).toBeInstanceOf(DOMException);
    expect((error as DOMException).name).toBe("AbortError");
  });
});
