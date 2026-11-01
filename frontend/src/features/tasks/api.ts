import { apiFetch } from "@/lib/api-client";
import type { Page, Task } from "./types";

// 浏览器端经同源 /api 转发调用，cookie 由浏览器自动携带。
// 任务状态会变化，全部 no-store，避免浏览器缓存旧状态。

/** 后端单页上限 50。 */
export const HISTORY_PAGE_SIZE = 50;

export function createTask(
  templateId: string,
  assetIds: number[],
): Promise<{ taskId: number }> {
  return apiFetch("/api/tasks", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ templateId, assetIds }),
  });
}

export function getTask(id: number, signal?: AbortSignal): Promise<Task> {
  return apiFetch(`/api/tasks/${id}`, { cache: "no-store", signal });
}

export function listTasks(
  page: number,
  signal?: AbortSignal,
): Promise<Page<Task>> {
  return apiFetch(`/api/tasks?page=${page}&size=${HISTORY_PAGE_SIZE}`, {
    cache: "no-store",
    signal,
  });
}
