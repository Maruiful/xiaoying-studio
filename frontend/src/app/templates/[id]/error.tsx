"use client";

import Link from "next/link";
import { ErrorPanel } from "@/components/error-panel";

export default function TemplateError({ reset }: { reset: () => void }) {
  return (
    <main className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6">
      <Link href="/" className="mb-4 inline-block text-sm text-muted">
        ← 返回模板列表
      </Link>
      <ErrorPanel title="模板详情加载失败" reset={reset} />
    </main>
  );
}
