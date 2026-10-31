import { getTemplates } from "./api";
import { TemplateCard } from "./template-card";

/** 异步 Server Component：按分类加载模板。外层用 Suspense 包裹，切换分类时显示骨架屏。 */
export async function TemplateGrid({ category }: { category?: string }) {
  const templates = await getTemplates(category);

  if (templates.length === 0) {
    return (
      <p className="rounded-xl bg-surface px-6 py-16 text-center text-muted">
        该分类下暂无模板
      </p>
    );
  }

  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
      {templates.map((t) => (
        <li key={t.id}>
          <TemplateCard template={t} />
        </li>
      ))}
    </ul>
  );
}

export function TemplateGridSkeleton() {
  return (
    <ul
      aria-busy="true"
      aria-label="模板加载中"
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6"
    >
      {Array.from({ length: 6 }, (_, i) => (
        <li
          key={i}
          className="aspect-[3/4] animate-pulse rounded-xl bg-surface"
        />
      ))}
    </ul>
  );
}
