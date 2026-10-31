import Link from "next/link";

// 用户入口（当前用户、切换演示用户）在阶段 2 加到右侧
export function SiteHeader() {
  return (
    <header className="sticky top-0 z-10 border-b border-border bg-background/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-[1600px] items-center justify-between px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2 font-semibold">
          <span
            aria-hidden="true"
            className="grid h-8 w-8 place-items-center rounded-lg bg-accent text-accent-foreground"
          >
            影
          </span>
          小影素材工坊
        </Link>
      </div>
    </header>
  );
}
