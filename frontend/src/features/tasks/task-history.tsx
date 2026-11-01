"use client";

import { useMe } from "@/features/session/use-session";
import { TaskCard } from "./task-card";
import { useRefreshTasks, useTaskHistory } from "./use-tasks";

/** 当前用户的全部制作记录。详情页右栏和 /history 共用。 */
export function TaskHistory({ headingId }: { headingId: string }) {
  const me = useMe();

  return (
    <div className="flex flex-col gap-4">
      {me.isSuccess ? (
        <HistoryBody userId={me.data.id} headingId={headingId} />
      ) : (
        <>
          <h2 id={headingId} className="font-semibold">
            制作记录
          </h2>
          {me.isError ? (
            <p className="text-sm text-danger">
              用户信息加载失败，请刷新页面重试
            </p>
          ) : (
            <ListSkeleton />
          )}
        </>
      )}
    </div>
  );
}

function HistoryBody({
  userId,
  headingId,
}: {
  userId: string;
  headingId: string;
}) {
  const history = useTaskHistory(userId);
  const refresh = useRefreshTasks(userId);
  const tasks = history.data;

  return (
    <>
      <div className="flex items-center justify-between">
        <h2 id={headingId} className="font-semibold">
          制作记录
          {tasks && tasks.length > 0 && (
            <span className="ml-2 text-sm font-normal text-muted">
              {tasks.length}
            </span>
          )}
        </h2>
        {/* 用 aria-disabled 而不是 disabled：disabled 会让按钮失去焦点，键盘用户的焦点会掉到页面顶部 */}
        <button
          type="button"
          onClick={() => {
            if (!history.isFetching) refresh();
          }}
          aria-disabled={history.isFetching}
          aria-label="刷新历史"
          className="grid h-8 w-8 place-items-center rounded-lg text-muted hover:bg-surface-raised hover:text-foreground aria-disabled:opacity-50"
        >
          <span
            aria-hidden="true"
            className={history.isFetching ? "animate-spin" : ""}
          >
            ↻
          </span>
        </button>
      </div>

      {/* 列表请求失败也是请求异常：已有记录继续显示 */}
      {history.isError && (
        <div
          role="alert"
          className="flex items-center justify-between gap-2 rounded-lg border border-border px-3 py-2 text-sm"
        >
          <span className="text-danger">
            制作记录加载失败：{history.error.message}
          </span>
          <button
            type="button"
            onClick={refresh}
            className="shrink-0 rounded px-2 py-1 text-accent hover:bg-surface-raised"
          >
            重试
          </button>
        </div>
      )}

      {tasks === undefined ? (
        history.isPending && <ListSkeleton />
      ) : tasks.length === 0 ? (
        <p className="rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
          还没有制作记录。上传素材并点击“开始制作”后，任务会显示在这里。
        </p>
      ) : (
        <ul className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-3">
          {tasks.map((task) => (
            <li key={task.id}>
              <TaskCard userId={userId} initial={task} />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function ListSkeleton() {
  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-3">
      <span className="sr-only">正在加载制作记录</span>
      {[0, 1].map((i) => (
        <div
          key={i}
          aria-hidden="true"
          className="aspect-[3/4] animate-pulse rounded-xl bg-surface-raised"
        />
      ))}
    </div>
  );
}
