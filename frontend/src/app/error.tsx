"use client";

import { ErrorPanel } from "@/components/error-panel";

export default function RootError({ reset }: { reset: () => void }) {
  return (
    <main className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6">
      <ErrorPanel title="模板加载失败" reset={reset} />
    </main>
  );
}
