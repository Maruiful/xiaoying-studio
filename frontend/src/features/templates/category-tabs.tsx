import Link from "next/link";
import type { Category } from "./types";

type Props = {
  categories: Category[];
  /** 当前选中的分类 id，undefined 表示"全部" */
  active?: string;
};

export function CategoryTabs({ categories, active }: Props) {
  const tabs = [{ id: undefined, name: "全部" }, ...categories];

  return (
    <nav aria-label="模板分类">
      <ul className="flex flex-wrap gap-2">
        {tabs.map((tab) => {
          const selected = tab.id === active;
          return (
            <li key={tab.id ?? "all"}>
              <Link
                href={tab.id ? `/?category=${encodeURIComponent(tab.id)}` : "/"}
                aria-current={selected ? "page" : undefined}
                scroll={false}
                className={
                  selected
                    ? "block rounded-lg bg-surface-raised px-3 py-1.5 sm:px-4 text-sm font-medium text-foreground"
                    : "block rounded-lg px-3 py-1.5 text-sm text-muted sm:px-4 transition-colors hover:bg-surface hover:text-foreground"
                }
              >
                {tab.name}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
