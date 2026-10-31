"use client";

import { useRouter } from "next/navigation";
import { startTransition } from "react";

type Props = {
  title: string;
  /** error.tsx 收到的 reset，用于重新渲染出错的路由段 */
  reset: () => void;
};

/**
 * 路由级错误提示。生产环境下 Server Component 的错误信息会被 Next.js 隐去，
 * 所以这里只显示通用文案，不显示 error.message。
 */
export function ErrorPanel({ title, reset }: Props) {
  const router = useRouter();

  function retry() {
    // 先让服务端重新取数，再重置错误边界
    startTransition(() => {
      router.refresh();
      reset();
    });
  }

  return (
    <div role="alert" className="rounded-xl bg-surface px-6 py-16 text-center">
      <p className="font-medium">{title}</p>
      <p className="mt-2 text-sm text-muted">服务暂时不可用，请稍后重试。</p>
      <button
        type="button"
        onClick={retry}
        className="mt-6 rounded-lg bg-surface-raised px-4 py-2 text-sm hover:bg-border"
      >
        重试
      </button>
    </div>
  );
}
