// @vitest-environment jsdom
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { delay, http, HttpResponse } from "msw";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it } from "vitest";
import { useMe, useSwitchUser } from "@/features/session/use-session";
import { qk } from "@/lib/query-keys";
import { server } from "@/test/server";
import type { Task } from "./types";
import {
  MAX_CONSECUTIVE_FAILURES,
  useCreateTask,
  useTask,
  useTaskHistory,
} from "./use-tasks";

const USER_A = "aaaaaaaa-0000-4000-8000-000000000001";
const USER_B = "bbbbbbbb-0000-4000-8000-000000000002";
/** 测试里用很短的轮询间隔，避免使用假时钟 */
const FAST = { intervalMs: 15 };

function task(id: number, patch: Partial<Task> = {}): Task {
  return {
    id,
    templateId: "demo-postcard",
    templateTitle: "复古明信片",
    status: "RUNNING",
    stage: "CHECK_ASSETS",
    resultUrl: null,
    failReason: null,
    createdAt: "2026-11-01T00:00:00Z",
    startedAt: "2026-11-01T00:00:00Z",
    finishedAt: null,
    ...patch,
  };
}

function succeeded(id: number): Task {
  return task(id, {
    status: "SUCCEEDED",
    stage: "ORGANIZE_RESULT",
    resultUrl: `/api/files/results/${id}`,
    finishedAt: "2026-11-01T00:00:03Z",
  });
}

function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: 0 }, mutations: { retry: 0 } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { client, wrapper };
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

afterEach(() => cleanup());

describe("useTask 轮询生命周期", () => {
  it("任务进入终态后停止轮询", async () => {
    let calls = 0;
    server.use(
      http.get("*/api/tasks/1", () => {
        calls += 1;
        return HttpResponse.json(
          calls < 3 ? task(1, { stage: "GENERATE" }) : succeeded(1),
        );
      }),
    );
    const { wrapper } = setup();

    const { result } = renderHook(() => useTask(USER_A, task(1), FAST), {
      wrapper,
    });

    await waitFor(() => expect(result.current.task.status).toBe("SUCCEEDED"));
    const callsAtEnd = calls;
    await sleep(100);
    expect(calls).toBe(callsAtEnd);
    expect(result.current.task.resultUrl).toBe("/api/files/results/1");
  });

  it("已结束的任务挂载后不发请求", async () => {
    let calls = 0;
    server.use(
      http.get("*/api/tasks/1", () => {
        calls += 1;
        return HttpResponse.json(succeeded(1));
      }),
    );
    const { wrapper } = setup();

    renderHook(() => useTask(USER_A, succeeded(1), FAST), { wrapper });

    await sleep(80);
    expect(calls).toBe(0);
  });

  it("查询失败是请求异常：保留已有数据，不当成任务失败；连续失败后暂停，重试可恢复", async () => {
    let calls = 0;
    let healthy = false;
    server.use(
      http.get("*/api/tasks/1", () => {
        calls += 1;
        if (healthy) return HttpResponse.json(succeeded(1));
        return HttpResponse.json(
          { code: "INTERNAL_ERROR", message: "服务暂时不可用" },
          { status: 503 },
        );
      }),
    );
    const { wrapper } = setup();
    const initial = task(1, { stage: "GENERATE" });

    const { result } = renderHook(() => useTask(USER_A, initial, FAST), {
      wrapper,
    });

    await waitFor(() => expect(result.current.paused).toBe(true));
    expect(result.current.task).toEqual(initial);
    expect(result.current.task.status).toBe("RUNNING");
    expect(result.current.queryError?.message).toBe("服务暂时不可用");
    expect(calls).toBe(MAX_CONSECUTIVE_FAILURES);

    // 暂停后不再自动请求
    await sleep(80);
    expect(calls).toBe(MAX_CONSECUTIVE_FAILURES);

    healthy = true;
    act(() => result.current.retry());

    await waitFor(() => expect(result.current.task.status).toBe("SUCCEEDED"));
    expect(result.current.queryError).toBeNull();
    expect(result.current.paused).toBe(false);
  });

  it("多个任务互不干扰：一个查询出错不影响另一个", async () => {
    server.use(
      http.get("*/api/tasks/1", () =>
        HttpResponse.json(
          { code: "INTERNAL_ERROR", message: "服务暂时不可用" },
          { status: 503 },
        ),
      ),
      http.get("*/api/tasks/2", () =>
        HttpResponse.json(
          task(2, {
            status: "FAILED",
            stage: "GENERATE",
            failReason: "模拟生成失败（模拟失败）",
          }),
        ),
      ),
      http.get("*/api/tasks/3", () => HttpResponse.json(succeeded(3))),
    );
    const { wrapper } = setup();

    const { result } = renderHook(
      () => ({
        one: useTask(USER_A, task(1), FAST),
        two: useTask(USER_A, task(2), FAST),
        three: useTask(USER_A, task(3), FAST),
      }),
      { wrapper },
    );

    await waitFor(() => {
      expect(result.current.one.paused).toBe(true);
      expect(result.current.two.task.status).toBe("FAILED");
      expect(result.current.three.task.status).toBe("SUCCEEDED");
    });
    expect(result.current.one.task.status).toBe("RUNNING");
    expect(result.current.two.queryError).toBeNull();
    expect(result.current.two.task.failReason).toBe("模拟生成失败（模拟失败）");
    expect(result.current.three.queryError).toBeNull();
    expect(result.current.three.task.resultUrl).toBe("/api/files/results/3");
  });
});

describe("提交与切换用户", () => {
  it("受理成功后任务立即出现在历史中，状态为等待处理", async () => {
    server.use(
      http.get("*/api/tasks", () =>
        HttpResponse.json({ items: [], total: 0, page: 1, size: 50 }),
      ),
      http.post("*/api/tasks", () =>
        HttpResponse.json({ taskId: 7 }, { status: 202 }),
      ),
    );
    const { wrapper } = setup();

    const { result } = renderHook(
      () => ({ history: useTaskHistory(USER_A), create: useCreateTask() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.history.isSuccess).toBe(true));

    // 列表刷新会返回服务端记录；先让它慢一点，确认插入是立即发生的
    server.use(
      http.get("*/api/tasks", async () => {
        await delay(200);
        return HttpResponse.json({
          items: [task(7, { status: "PENDING", stage: null })],
          total: 1,
          page: 1,
          size: 50,
        });
      }),
    );
    act(() =>
      result.current.create.mutate({
        userId: USER_A,
        templateId: "demo-postcard",
        templateTitle: "复古明信片",
        assetIds: [1],
      }),
    );

    await waitFor(() => expect(result.current.create.isSuccess).toBe(true));
    expect(result.current.history.data?.map((t) => [t.id, t.status])).toEqual([
      [7, "PENDING"],
    ]);
  });

  it("请求返回前切换了用户：结果只写进提交者自己的缓存", async () => {
    server.use(
      http.post("*/api/tasks", async () => {
        await delay(50);
        return HttpResponse.json({ taskId: 8 }, { status: 202 });
      }),
      http.get("*/api/tasks", () =>
        HttpResponse.json({ items: [], total: 0, page: 1, size: 50 }),
      ),
    );
    const { client, wrapper } = setup();
    client.setQueryData(qk.tasks(USER_B), [] as Task[]);

    const { result } = renderHook(() => useCreateTask(), { wrapper });
    act(() =>
      result.current.mutate({
        userId: USER_A,
        templateId: "demo-postcard",
        templateTitle: "复古明信片",
        assetIds: [1],
      }),
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(client.getQueryData(qk.tasks(USER_B))).toEqual([]);
    expect(client.getQueryData(qk.task(USER_B, 8))).toBeUndefined();
  });

  it("切换用户后，上一个用户晚返回的请求不会写进缓存", async () => {
    let lateResponses = 0;
    server.use(
      http.get("*/api/tasks/1", async () => {
        await delay(120);
        lateResponses += 1;
        return HttpResponse.json(succeeded(1));
      }),
      http.post("*/api/session/switch", () =>
        HttpResponse.json({ id: USER_B, createdAt: "2026-11-01T00:00:00Z" }),
      ),
    );
    const { client, wrapper } = setup();
    client.setQueryData(qk.me, {
      id: USER_A,
      createdAt: "2026-11-01T00:00:00Z",
    });

    // 和页面一样：跟踪哪个用户的任务由当前 useMe() 决定
    const { result } = renderHook(
      () => {
        const me = useMe();
        const userId = me.data!.id;
        const tracked = useTask(
          userId,
          userId === USER_A ? task(1) : succeeded(2),
          FAST,
        );
        return { userId, tracked, switchUser: useSwitchUser() };
      },
      { wrapper },
    );

    // 用户 A 的任务正在查询（请求要 120ms 才返回）
    await waitFor(() => expect(result.current.tracked.fetching).toBe(true));

    await act(async () => {
      await result.current.switchUser.mutateAsync(USER_B);
    });
    // 缓存更新的通知由 TanStack Query 批量调度到下一个 tick，可能晚于 act 返回，所以用 waitFor 等待
    await waitFor(() => expect(result.current.userId).toBe(USER_B));
    expect(result.current.tracked.task.id).toBe(2);

    // 等 A 的请求在服务端返回之后再检查
    await sleep(200);
    expect(lateResponses).toBeLessThanOrEqual(1);
    expect(client.getQueryCache().findAll({ queryKey: [USER_A] })).toHaveLength(
      0,
    );
    expect(result.current.tracked.task).toEqual(succeeded(2));
  });
});
