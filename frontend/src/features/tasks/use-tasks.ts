"use client";

import {
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useCallback, useRef } from "react";
import { qk } from "@/lib/query-keys";
import { createTask, getTask, listTasks } from "./api";
import { pickLatest } from "./progress";
import { isTerminal, type Task } from "./types";

export const TASK_POLL_INTERVAL_MS = 1000;
/** 单个任务连续查询失败这么多次后暂停自动轮询，等用户点重试或刷新 */
export const MAX_CONSECUTIVE_FAILURES = 3;

/** 把列表里的任务写进单任务缓存，已有更新的数据时不覆盖（见 pickLatest）。 */
function seedTask(queryClient: QueryClient, userId: string, task: Task) {
  queryClient.setQueryData<Task>(qk.task(userId, task.id), (cached) =>
    cached ? pickLatest(cached, task) : task,
  );
}

/** 当前用户的历史任务（最近一页）。列表本身不轮询，未结束的任务由各自的 useTask 跟踪。 */
export function useTaskHistory(userId: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: qk.tasks(userId),
    queryFn: async ({ signal }) => {
      const page = await listTasks(signal);
      for (const task of page.items) seedTask(queryClient, userId, task);
      return page.items;
    },
  });
}

type UseTaskOptions = { intervalMs?: number };

/**
 * 跟踪单个任务。未结束时按间隔轮询，进入终态后停止。
 *
 * 查询失败属于请求异常：保留上次的数据（TanStack Query 出错时不清空 data），
 * 不把任务当成失败。连续失败 MAX_CONSECUTIVE_FAILURES 次后暂停轮询，retry() 可以恢复。
 */
export function useTask(
  userId: string,
  initial: Task,
  { intervalMs = TASK_POLL_INTERVAL_MS }: UseTaskOptions = {},
) {
  // TanStack Query 每次重新请求都会清零 fetchFailureCount，连续失败次数需要自己记。
  // 在 queryFn 里同步更新，refetchInterval 回调读取时已经是最新值。
  const failures = useRef(0);

  const query = useQuery({
    queryKey: qk.task(userId, initial.id),
    queryFn: async ({ signal }) => {
      try {
        const task = await getTask(initial.id, signal);
        failures.current = 0;
        return task;
      } catch (error) {
        if (!signal.aborted) failures.current += 1;
        throw error;
      }
    },
    initialData: initial,
    // 已结束的任务不会再变化，不需要重新请求
    staleTime: (q) => (q.state.data && isTerminal(q.state.data) ? Infinity : 0),
    refetchInterval: (q) => {
      if (q.state.data && isTerminal(q.state.data)) return false;
      if (failures.current >= MAX_CONSECUTIVE_FAILURES) return false;
      return intervalMs;
    },
    // 重试由轮询本身承担
    retry: 0,
  });

  const { refetch } = query;
  const retry = useCallback(() => void refetch(), [refetch]);

  return {
    task: query.data,
    /** 最近一次状态查询失败（请求异常，不是任务失败） */
    queryError: query.isError ? query.error : null,
    paused: query.isError && failures.current >= MAX_CONSECUTIVE_FAILURES,
    fetching: query.isFetching,
    retry,
  };
}

/** 刷新历史列表，并重新查询所有未结束的任务（会恢复已暂停的轮询）。 */
export function useRefreshTasks(userId: string) {
  const queryClient = useQueryClient();
  return useCallback(() => {
    void queryClient.refetchQueries({ queryKey: qk.tasks(userId) });
    void queryClient.refetchQueries({
      queryKey: qk.allTasks(userId),
      predicate: (q) => {
        const task = q.state.data as Task | undefined;
        return task !== undefined && !isTerminal(task);
      },
    });
  }, [queryClient, userId]);
}

export type CreateTaskInput = {
  /** 提交时的用户。放在参数里而不是从组件读取：请求返回前切换了用户，结果也只写进提交者的缓存 */
  userId: string;
  templateId: string;
  templateTitle: string;
  assetIds: number[];
};

/** 提交制作任务。受理后立即把任务放进历史列表，由 useTask 接着跟踪状态。 */
export function useCreateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateTaskInput) =>
      createTask(input.templateId, input.assetIds),
    onSuccess: ({ taskId }, input) => {
      const task: Task = {
        id: taskId,
        templateId: input.templateId,
        templateTitle: input.templateTitle,
        status: "PENDING",
        stage: null,
        resultUrl: null,
        failReason: null,
        createdAt: new Date().toISOString(),
        startedAt: null,
        finishedAt: null,
      };
      seedTask(queryClient, input.userId, task);
      queryClient.setQueryData<Task[]>(qk.tasks(input.userId), (old) =>
        old ? [task, ...old.filter((t) => t.id !== taskId)] : old,
      );
      // 以服务端记录为准（创建时间等字段）
      void queryClient.invalidateQueries({ queryKey: qk.tasks(input.userId) });
    },
  });
}
