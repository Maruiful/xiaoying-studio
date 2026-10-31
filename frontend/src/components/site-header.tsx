import Link from "next/link";
import { AccountMenu } from "@/features/session/account-menu";

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
        <nav aria-label="主导航" className="flex items-center gap-1">
          <Link
            href="/history"
            className="rounded-lg px-3 py-1.5 text-sm text-muted hover:bg-surface hover:text-foreground"
          >
            制作记录
          </Link>
          <AccountMenu />
        </nav>
      </div>
    </header>
  );
}
