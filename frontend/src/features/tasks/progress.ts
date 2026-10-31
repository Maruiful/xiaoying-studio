import {
  isTerminal,
  TASK_STAGES,
  type Task,
  type TaskStage,
  type TaskStatus,
} from "./types";

export const STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "等待处理",
  RUNNING: "处理中",
  SUCCEEDED: "已完成",
  FAILED: "制作失败",
};

export const STAGE_LABELS: Record<TaskStage, string> = {
  CHECK_ASSETS: "素材检查",
  GENERATE: "模拟生成",
  ORGANIZE_RESULT: "结果整理",
};

/** 任务推进的先后：PENDING < RUNNING(按阶段) < 终态。终态之后不会再变化。 */
function progressRank(task: Task): number {
  if (isTerminal(task)) return TASK_STAGES.length + 1;
  if (task.status === "RUNNING" && task.stage) {
    return 1 + TASK_STAGES.indexOf(task.stage);
  }
  return 0;
}

/**
 * 同一任务的两份数据取更新的一份。历史列表和单个任务的轮询是两个请求，
 * 列表响应可能比轮询结果旧，用它覆盖缓存时不能让已结束的任务退回处理中。
 * 进度相同时取 incoming（较新的请求）。
 */
export function pickLatest(cached: Task, incoming: Task): Task {
  return progressRank(incoming) >= progressRank(cached) ? incoming : cached;
}

export function formatTaskTime(iso: string): string {
  return new Date(iso).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}
