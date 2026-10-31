import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@/test/server";
import { proxyToBackend } from "./backend-proxy";

const BACKEND = "http://localhost:8080";

describe("proxyToBackend", () => {
  it("转发路径、查询参数、方法和 cookie", async () => {
    let seen: { url: string; method: string; cookie: string | null } | null =
      null;
    server.use(
      http.get(`${BACKEND}/api/templates`, ({ request }) => {
        seen = {
          url: request.url,
          method: request.method,
          cookie: request.headers.get("cookie"),
        };
        return HttpResponse.json([{ id: "demo" }]);
      }),
    );

    const res = await proxyToBackend(
      new Request("http://web/api/templates?category=a%20b", {
        headers: { cookie: "uid=u1" },
      }),
      ["templates"],
    );

    expect(res.status).toBe(200);
    expect(await res.json()).toEqual([{ id: "demo" }]);
    expect(seen).toEqual({
      url: `${BACKEND}/api/templates?category=a%20b`,
      method: "GET",
      cookie: "uid=u1",
    });
  });

  it("转发 POST 请求体", async () => {
    server.use(
      http.post(`${BACKEND}/api/tasks`, async ({ request }) =>
        HttpResponse.json(await request.json(), { status: 202 }),
      ),
    );

    const res = await proxyToBackend(
      new Request("http://web/api/tasks", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ templateId: "demo", assetIds: [1, 2] }),
      }),
      ["tasks"],
    );

    expect(res.status).toBe(202);
    expect(await res.json()).toEqual({ templateId: "demo", assetIds: [1, 2] });
  });

  it("保留后端返回的多个 Set-Cookie", async () => {
    server.use(
      http.post(
        `${BACKEND}/api/users`,
        () =>
          new HttpResponse(null, {
            status: 201,
            headers: [
              ["set-cookie", "uid=u2; Path=/; HttpOnly"],
              ["set-cookie", "other=x; Path=/"],
            ],
          }),
      ),
    );

    const res = await proxyToBackend(
      new Request("http://web/api/users", { method: "POST" }),
      ["users"],
    );

    expect(res.headers.getSetCookie()).toEqual([
      "uid=u2; Path=/; HttpOnly",
      "other=x; Path=/",
    ]);
  });

  it("后端的错误状态码和响应体原样返回", async () => {
    server.use(
      http.get(`${BACKEND}/api/templates/missing`, () =>
        HttpResponse.json(
          { code: "TEMPLATE_NOT_FOUND", message: "模板不存在" },
          { status: 404 },
        ),
      ),
    );

    const res = await proxyToBackend(
      new Request("http://web/api/templates/missing"),
      ["templates", "missing"],
    );

    expect(res.status).toBe(404);
    expect(await res.json()).toMatchObject({ code: "TEMPLATE_NOT_FOUND" });
  });

  it("后端不可达时返回 502", async () => {
    server.use(http.get(`${BACKEND}/api/health`, () => HttpResponse.error()));

    const res = await proxyToBackend(new Request("http://web/api/health"), [
      "health",
    ]);

    expect(res.status).toBe(502);
    expect(await res.json()).toMatchObject({ code: "BACKEND_UNAVAILABLE" });
  });

  it("拒绝包含 .. 的路径", async () => {
    const res = await proxyToBackend(new Request("http://web/api/x"), [
      "..",
      "actuator",
    ]);

    expect(res.status).toBe(400);
  });
});
