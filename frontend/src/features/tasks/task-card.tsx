"use client";

import { formatTaskTime, STAGE_LABELS, STATUS_LABELS } from "./progress";
import { isTerminal, TASK_STAGES, type Task } from "./types";
import { useTask } from "./use-tasks";

/**
 * 一条制作记录。自己跟踪自己的状态，和其他卡片互不影响。
 * 两种异常分开展示：任务 FAILED 是任务本身失败；"状态查询失败"只是请求异常，保留上次的数据。
 */
export function TaskCard({
  userId,
  initial,
}: {
  userId: string;
  initial: Task;
}) {
  const { task, queryError, paused, fetching, retry } = useTask(
    userId,
    initial,
  );

  return (
    // 布局参考 resources/模板详情与素材上传.png：竖版卡片，上方预览区，下方标题与进度
    <article
      aria-labelledby={`task-${task.id}-title`}
      className="flex flex-col overflow-hidden rounded-xl border border-border bg-background"
    >
      <div className="relative aspect-[3/4]">
        {task.status === "SUCCEEDED" && task.resultUrl ? (
          <a
            href={task.resultUrl}
            target="_blank"
            rel="noreferrer"
            className="block h-full"
            aria-label={`在新窗口查看「${task.templateTitle}」的制作结果`}
          >
            <img
              src={task.resultUrl}
              alt={`「${task.templateTitle}」制作结果`}
              className="h-full w-full object-cover"
              loading="lazy"
            />
          </a>
        ) : (
          <div className="flex h-full items-center justify-center p-4 text-center text-sm">
            {task.status === "FAILED" ? (
              <p className="text-danger">
                {task.stage ? `${STAGE_LABELS[task.stage]}失败：` : ""}
                {task.failReason ?? "未知原因"}
              </p>
            ) : (
              <p className="text-foreground">
                {task.stage ? `${STAGE_LABELS[task.stage]}中…` : "等待处理…"}
              </p>
            )}
          </div>
        )}
        <div className="absolute top-2 right-2">
          <StatusBadge task={task} />
        </div>
      </div>

      <div className="flex flex-col gap-3 p-3">
        <header className="min-w-0">
          <h3 id={`task-${task.id}-title`} className="truncate font-medium">
            {task.templateTitle}
          </h3>
          <p className="text-xs text-muted">
            #{task.id} · 创建于{" "}
            <time dateTime={task.createdAt}>
              {formatTaskTime(task.createdAt)}
            </time>
          </p>
        </header>

        <StageList task={task} />

        {queryError && (
          <div
            role="status"
            className="flex items-center justify-between gap-2 rounded-lg border border-border px-3 py-2 text-xs"
          >
            <span className="text-muted">
              状态查询失败{paused ? "，已暂停自动刷新" : "，正在重试"}：
              {queryError.message}
            </span>
            <button
              type="button"
              onClick={retry}
              disabled={fetching}
              className="shrink-0 rounded px-2 py-1 text-accent hover:bg-surface disabled:opacity-50"
            >
              {fetching ? "查询中…" : "重试"}
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

function StatusBadge({ task }: { task: Task }) {
  const tone =
    task.status === "SUCCEEDED"
      ? "bg-accent text-accent-foreground"
      : task.status === "FAILED"
        ? "bg-danger/15 text-danger"
        : "bg-surface text-foreground";
  return (
    <span
      className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-medium ${tone}`}
    >
      {!isTerminal(task) && (
        <span
          aria-hidden="true"
          className="mr-1 inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-accent align-middle"
        />
      )}
      {STATUS_LABELS[task.status]}
    </span>
  );
}

type StepState = "done" | "current" | "failed" | "todo";

function stepState(task: Task, index: number): StepState {
  if (task.status === "SUCCEEDED") return "done";
  if (task.stage === null) return "todo";
  const current = TASK_STAGES.indexOf(task.stage);
  if (index < current) return "done";
  if (index > current) return "todo";
  return task.status === "FAILED" ? "failed" : "current";
}

const STEP_TEXT: Record<StepState, string> = {
  done: "已完成",
  current: "进行中",
  failed: "失败",
  todo: "未开始",
};

const STEP_DOT: Record<StepState, string> = {
  done: "bg-accent",
  current: "bg-accent animate-pulse",
  failed: "bg-danger",
  todo: "bg-border",
};

/** 三个阶段的进度。阶段完全来自后端记录，前端不按时间推测。 */
function StageList({ task }: { task: Task }) {
  return (
    <ol aria-label="处理阶段" className="grid grid-cols-3 gap-2">
      {TASK_STAGES.map((stage, i) => {
        const state = stepState(task, i);
        return (
          <li key={stage} className="flex flex-col gap-1.5">
            <span
              aria-hidden="true"
              className={`h-1 rounded-full ${STEP_DOT[state]}`}
            />
            <span
              className={`text-xs ${state === "todo" ? "text-muted" : state === "failed" ? "text-danger" : ""}`}
            >
              {STAGE_LABELS[stage]}
              <span className="sr-only">：{STEP_TEXT[state]}</span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
