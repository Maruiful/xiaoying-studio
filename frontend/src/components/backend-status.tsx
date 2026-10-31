"use client";

import { useEffect, useState } from "react";

type Status =
  { kind: "loading" } | { kind: "up" } | { kind: "down"; message: string };

const LABEL: Record<Status["kind"], string> = {
  loading: "正在检查后端连接…",
  up: "后端与数据库连接正常",
  down: "后端连接异常",
};

export function BackendStatus() {
  const [status, setStatus] = useState<Status>({ kind: "loading" });

  useEffect(() => {
    const controller = new AbortController();
    fetch("/api/health", { signal: controller.signal })
      .then(async (res) => {
        const body: unknown = await res.json().catch(() => null);
        if (res.ok) {
          setStatus({ kind: "up" });
        } else {
          setStatus({ kind: "down", message: describe(res.status, body) });
        }
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setStatus({
          kind: "down",
          message: error instanceof Error ? error.message : "网络错误",
        });
      });
    return () => controller.abort();
  }, []);

  return (
    <p
      role="status"
      aria-live="polite"
      className={
        status.kind === "down"
          ? "rounded-lg bg-surface px-4 py-3 text-danger"
          : "rounded-lg bg-surface px-4 py-3"
      }
    >
      {LABEL[status.kind]}
      {status.kind === "down" && `：${status.message}`}
    </p>
  );
}

function describe(httpStatus: number, body: unknown): string {
  if (body && typeof body === "object" && "message" in body) {
    return `${httpStatus} ${String(body.message)}`;
  }
  return `HTTP ${httpStatus}`;
}
