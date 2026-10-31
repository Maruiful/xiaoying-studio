import { Suspense } from "react";
import { getCategories } from "@/features/templates/api";
import { CategoryTabs } from "@/features/templates/category-tabs";
import {
  TemplateGrid,
  TemplateGridSkeleton,
} from "@/features/templates/template-grid";

type Props = {
  searchParams: Promise<{ category?: string | string[] }>;
};

export default async function HomePage({ searchParams }: Props) {
  const { category: raw } = await searchParams;
  const category = typeof raw === "string" && raw ? raw : undefined;
  const categories = await getCategories();

  return (
    <main className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6">
      <h1 className="sr-only">模板列表</h1>
      <div className="mb-6">
        <CategoryTabs categories={categories} active={category} />
      </div>
      {/* key 随分类变化，切换分类时重新显示骨架屏 */}
      <Suspense key={category ?? "all"} fallback={<TemplateGridSkeleton />}>
        <TemplateGrid category={category} />
      </Suspense>
    </main>
  );
}
