import Link from "next/link";
import type { TemplateCard as TemplateCardData } from "./types";

export function TemplateCard({ template }: { template: TemplateCardData }) {
  return (
    <Link
      href={`/templates/${encodeURIComponent(template.id)}`}
      className="group relative block aspect-[3/4] overflow-hidden rounded-xl bg-surface"
    >
      {/* 封面由后端提供（SVG/上传图），不经过 next/image 优化 */}
      <img
        src={template.coverUrl}
        alt=""
        loading="lazy"
        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
      />
      <div className="absolute inset-x-0 bottom-0 bg-linear-to-t from-black/80 to-transparent px-3 pt-10 pb-3">
        <h2 className="text-sm font-medium text-white">{template.title}</h2>
        <p className="mt-0.5 text-xs text-white/70">
          需要 {template.requiredImageCount} 张图片
        </p>
      </div>
    </Link>
  );
}
