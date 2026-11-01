"use client";

import { useState } from "react";
import { loadRecentUsers } from "./recent-users";
import type { User } from "./types";
import { useCreateUser, useSwitchUser } from "./use-session";

/** 账号对话框的内容。布局参考 resources/用户上传和个人记录.png。 */
export function AccountPanel({ user }: { user: User }) {
  const createUser = useCreateUser();
  const switchUser = useSwitchUser();
  const busy = createUser.isPending || switchUser.isPending;
  const error = createUser.error ?? switchUser.error;
  // 每次渲染重新读取，新建或切换后列表立即更新
  const recent = loadRecentUsers();

  return (
    <div className="flex flex-col gap-5">
      <section className="rounded-2xl bg-surface-raised p-4">
        <div className="flex items-center gap-3">
          <span
            aria-hidden="true"
            className="grid h-16 w-16 place-items-center rounded-full bg-accent text-2xl font-semibold text-accent-foreground"
          >
            {user.id.slice(0, 1).toUpperCase()}
          </span>
          <div>
            <p className="font-medium">匿名用户</p>
            <p className="text-xs text-muted">
              创建于 {new Date(user.createdAt).toLocaleString("zh-CN")}
            </p>
          </div>
        </div>
        <div className="mt-4 border-t border-border pt-4">
          <p className="text-sm text-muted">用户 ID</p>
          <div className="mt-1 flex items-center gap-2">
            <code className="min-w-0 flex-1 truncate font-mono text-sm">
              {user.id}
            </code>
            <CopyButton text={user.id} />
          </div>
        </div>
      </section>

      <section aria-labelledby="switch-heading">
        <div className="flex items-center justify-between">
          <h3 id="switch-heading" className="text-sm font-medium">
            切换演示用户
          </h3>
          <button
            type="button"
            disabled={busy}
            onClick={() => createUser.mutate()}
            className="rounded-lg bg-accent px-3 py-1.5 text-sm font-medium text-accent-foreground disabled:opacity-50"
          >
            {createUser.isPending ? "创建中…" : "新建匿名用户"}
          </button>
        </div>
        <p className="mt-1 text-xs text-muted">
          不同用户的制作记录互相隔离。下面列出在此浏览器中用过的用户。
        </p>

        {error && (
          <p role="alert" className="mt-3 text-sm text-danger">
            操作失败：{error.message}
          </p>
        )}

        <ul className="mt-3 divide-y divide-border rounded-2xl border border-border">
          {recent.map((id) => (
            <li key={id} className="flex items-center gap-3 px-4 py-3.5">
              <code className="min-w-0 flex-1 truncate font-mono text-sm">
                {id}
              </code>
              {id === user.id ? (
                <span className="text-xs text-accent">当前</span>
              ) : (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => switchUser.mutate(id)}
                  aria-label={`切换到用户 ${id.slice(0, 8)}`}
                  className="rounded-lg px-3 py-1 text-sm hover:bg-surface-raised disabled:opacity-50"
                >
                  切换<span aria-hidden="true"> ›</span>
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function CopyButton({ text }: { text: string }) {
  const [state, setState] = useState<"idle" | "copied" | "failed">("idle");

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setState("copied");
    } catch {
      setState("failed");
    }
    setTimeout(() => setState("idle"), 1500);
  }

  return (
    <button
      type="button"
      onClick={() => void copy()}
      className="shrink-0 rounded-lg px-2 py-1 text-xs text-muted hover:bg-surface hover:text-foreground"
    >
      <span aria-live="polite">
        {state === "copied"
          ? "已复制"
          : state === "failed"
            ? "复制失败"
            : "复制"}
      </span>
    </button>
  );
}
