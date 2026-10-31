import { BackendStatus } from "@/components/backend-status";

// 阶段 0 占位页：验证 浏览器 → Next.js 转发 → Java → MySQL 整条链路。阶段 1 替换为模板列表。
export default function HomePage() {
  return (
    <main className="mx-auto flex max-w-xl flex-col gap-4 px-6 py-16">
      <h1 className="text-2xl font-semibold">小影素材工坊</h1>
      <p className="text-muted">项目骨架已启动，模板列表将在阶段 1 实现。</p>
      <BackendStatus />
    </main>
  );
}
