import { formatBytes, formatMimeTypes } from "./format";
import type { TemplateDetail } from "./types";

export function TemplateRequirements({
  template,
}: {
  template: TemplateDetail;
}) {
  const items = [
    { label: "图片数量", value: `${template.requiredImageCount} 张` },
    { label: "支持格式", value: formatMimeTypes(template.acceptedMimeTypes) },
    {
      label: "单张大小",
      value: `不超过 ${formatBytes(template.maxFileSizeBytes)}`,
    },
  ];

  return (
    <section aria-labelledby="requirements-heading">
      <h2 id="requirements-heading" className="text-sm font-medium">
        素材要求
      </h2>
      <dl className="mt-2 grid grid-cols-3 gap-2">
        {items.map((item) => (
          <div key={item.label} className="rounded-lg bg-surface-raised p-3">
            <dt className="text-xs text-muted">{item.label}</dt>
            <dd className="mt-1 text-sm">{item.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
