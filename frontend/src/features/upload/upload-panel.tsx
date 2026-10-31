"use client";

import type { TemplateDetail } from "@/features/templates/types";
import { readyAssetIds } from "./slots";
import { UploadSlot } from "./upload-slot";
import { useUploadSlots } from "./use-upload-slots";

/** 详情页左栏的上传区和提交按钮。 */
export function UploadPanel({ template }: { template: TemplateDetail }) {
  const { slots, select, remove } = useUploadSlots(template);
  const assetIds = readyAssetIds(slots);
  const readyCount = slots.filter((s) => s.kind === "ready").length;
  const uploading = slots.some((s) => s.kind === "uploading");
  const accept = template.acceptedMimeTypes.join(",");

  return (
    <section aria-labelledby="upload-heading" className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between">
        <h2 id="upload-heading" className="text-sm font-medium">
          上传素材
        </h2>
        <span className="text-xs text-muted">
          {readyCount} / {slots.length}
        </span>
      </div>

      <ul
        className={
          slots.length === 1
            ? "grid grid-cols-1 gap-3"
            : "grid grid-cols-2 gap-3"
        }
      >
        {slots.map((slot, i) => (
          <li key={i}>
            <UploadSlot
              index={i}
              slot={slot}
              accept={accept}
              onSelect={(file) => select(i, file)}
              onRemove={() => remove(i)}
            />
          </li>
        ))}
      </ul>

      {/* 阶段 4：点击后提交制作任务 */}
      <button
        type="button"
        disabled={assetIds === null}
        className="mt-2 rounded-xl bg-accent py-3 font-medium text-accent-foreground transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
      >
        开始制作
      </button>
      <p aria-live="polite" className="text-center text-xs text-muted">
        {uploading
          ? "图片上传中，请稍候"
          : assetIds === null
            ? `还需上传 ${slots.length - readyCount} 张图片`
            : "素材已就绪"}
      </p>
    </section>
  );
}
