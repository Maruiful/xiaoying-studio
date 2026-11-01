import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getTemplate } from "@/features/templates/api";
import { TemplateCover } from "@/features/templates/template-cover";
import { TemplateRequirements } from "@/features/templates/template-requirements";
import { TaskHistory } from "@/features/tasks/task-history";
import { UploadPanel } from "@/features/upload/upload-panel";

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const template = await getTemplate(id);
  return { title: template?.title ?? "模板不存在" };
}

// 布局参考 resources/模板详情与素材上传.png：左栏详情与上传，右栏制作记录
export default async function TemplatePage({ params }: Props) {
  const { id } = await params;
  const template = await getTemplate(id);
  if (!template) notFound();

  return (
    // md 及以上两栏占满视口高度（减去 3.5rem 顶栏），各自滚动，开始制作按钮固定在左栏底部
    <main className="mx-auto grid max-w-[1600px] gap-2 p-2 md:h-[calc(100dvh-3.5rem)] md:grid-cols-[minmax(320px,425px)_1fr]">
      <section
        aria-labelledby="template-title"
        className="flex flex-col gap-5 rounded-2xl bg-surface p-5 pb-0 md:overflow-y-auto"
      >
        <div className="flex items-center gap-2">
          <Link
            href="/"
            aria-label="返回模板列表"
            className="grid h-8 w-8 place-items-center rounded-lg text-muted hover:bg-surface-raised hover:text-foreground"
          >
            <span aria-hidden="true">←</span>
          </Link>
          <h1 id="template-title" className="font-semibold">
            {template.title}
          </h1>
          <span className="ml-auto rounded bg-surface-raised px-2 py-0.5 text-xs text-muted">
            {template.categoryName}
          </span>
        </div>

        <TemplateCover src={template.coverUrl} title={template.title} />

        <p className="text-sm text-muted">{template.description}</p>

        <TemplateRequirements template={template} />

        {/* key：切换到另一个模板时重置上传位 */}
        <UploadPanel key={template.id} template={template} />
      </section>

      <section
        aria-labelledby="history-heading"
        className="rounded-2xl bg-surface p-5 md:overflow-y-auto"
      >
        <TaskHistory headingId="history-heading" />
      </section>
    </main>
  );
}
