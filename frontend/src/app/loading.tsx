import { TemplateGridSkeleton } from "@/features/templates/template-grid";

export default function Loading() {
  return (
    <main className="mx-auto max-w-[1600px] px-4 py-6 sm:px-6">
      <div className="mb-6 h-9 w-72 animate-pulse rounded-lg bg-surface" />
      <TemplateGridSkeleton />
    </main>
  );
}
