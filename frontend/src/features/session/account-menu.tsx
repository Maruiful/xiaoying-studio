"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { useState } from "react";
import { AccountPanel } from "./account-panel";
import { useMe } from "./use-session";

/** 顶栏右侧的用户入口：显示当前用户 ID 前 8 位，点击打开账号对话框。 */
export function AccountMenu() {
  const me = useMe();
  const [open, setOpen] = useState(false);

  if (me.isPending) {
    return (
      <span className="h-8 w-28 animate-pulse rounded-lg bg-surface">
        <span className="sr-only">正在加载用户信息</span>
      </span>
    );
  }

  if (me.isError) {
    return (
      <button
        type="button"
        onClick={() => void me.refetch()}
        className="rounded-lg px-3 py-1.5 text-sm text-danger hover:bg-surface"
      >
        用户信息加载失败，重试
      </button>
    );
  }

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          aria-label={`账号设置，当前用户 ${me.data.id.slice(0, 8)}`}
          className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-surface"
        >
          <span
            aria-hidden="true"
            className="grid h-7 w-7 place-items-center rounded-full bg-accent text-xs font-semibold text-accent-foreground"
          >
            {me.data.id.slice(0, 1).toUpperCase()}
          </span>
          <span className="hidden font-mono text-muted sm:inline">
            {me.data.id.slice(0, 8)}
          </span>
        </button>
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-20 bg-black/60" />
        <Dialog.Content className="fixed top-1/2 left-1/2 z-30 max-h-[85vh] w-[min(92vw,560px)] -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-3xl border border-border bg-surface p-7 shadow-xl">
          <div className="mb-5 flex items-center justify-between">
            <Dialog.Title className="text-xl font-semibold">
              账号设置
            </Dialog.Title>
            <Dialog.Close
              aria-label="关闭"
              className="grid h-9 w-9 place-items-center rounded-xl bg-background text-muted hover:bg-surface-raised hover:text-foreground"
            >
              <span aria-hidden="true">✕</span>
            </Dialog.Close>
          </div>
          <Dialog.Description className="sr-only">
            查看当前匿名用户，新建或切换演示用户
          </Dialog.Description>
          {/* 对话框打开时才挂载，读取 localStorage 不会影响服务端渲染 */}
          {open && <AccountPanel user={me.data} />}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
