// 与后端 TaskVO、PageVO 对应（backend/src/main/java/com/xiaoying/task/TaskVO.java）

export type TaskStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";

/** 三个阶段按顺序执行 */
export const TASK_STAGES = [
  "CHECK_ASSETS",
  "GENERATE",
  "ORGANIZE_RESULT",
] as const;
export type TaskStage = (typeof TASK_STAGES)[number];

export type Task = {
  id: number;
  templateId: string;
  templateTitle: string;
  status: TaskStatus;
  /** 当前（或失败时所在的）阶段，PENDING 时为 null */
  stage: TaskStage | null;
  /** 仅 SUCCEEDED 时有值 */
  resultUrl: string | null;
  /** 仅 FAILED 时有值 */
  failReason: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
};

export type Page<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};

export function isTerminal(task: Pick<Task, "status">): boolean {
  return task.status === "SUCCEEDED" || task.status === "FAILED";
}
