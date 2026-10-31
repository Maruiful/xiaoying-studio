"use client";

import { useMe } from "@/features/session/use-session";
import { useCreateTask } from "@/features/tasks/use-tasks";
import type { TemplateDetail } from "@/features/templates/types";
import { readyAssetIds } from "./slots";
import { UploadSlot } from "./upload-slot";
import { useUploadSlots } from "./use-upload-slots";

/** 详情页左栏的上传区和提交按钮。 */
export function UploadPanel({ template }: { template: TemplateDetail }) {
  const me = useMe();

  if (!me.isSuccess) {
    return (
      <p aria-live="polite" className="text-sm text-muted">
        {me.isError
          ? "用户信息加载失败，请刷新页面后再上传"
          : "正在加载用户信息…"}
      </p>
    );
  }

  // key：切换用户后重置上传位。已上传的素材属于上一个用户，不能用来提交
  return (
    <UploadForm key={me.data.id} userId={me.data.id} template={template} />
  );
}

function UploadForm({
  userId,
  template,
}: {
  userId: string;
  template: TemplateDetail;
}) {
  const { slots, select, remove, reset } = useUploadSlots(template);
  const createTask = useCreateTask();
  const assetIds = readyAssetIds(slots);
  const readyCount = slots.filter((s) => s.kind === "ready").length;
  const uploading = slots.some((s) => s.kind === "uploading");
  const submitting = createTask.isPending;
  const accept = template.acceptedMimeTypes.join(",");

  const submit = () => {
    if (assetIds === null || submitting) return;
    createTask.mutate(
      {
        userId,
        templateId: template.id,
        templateTitle: template.title,
        assetIds,
      },
      // 受理后清空上传位，按钮恢复可用，可以接着提交下一个任务
      { onSuccess: () => reset() },
    );
  };

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
              disabled={submitting}
              onSelect={(file) => select(i, file)}
              onRemove={() => remove(i)}
            />
          </li>
        ))}
      </ul>

      <button
        type="button"
        onClick={submit}
        disabled={assetIds === null || submitting}
        aria-busy={submitting}
        className="mt-2 rounded-xl bg-accent py-3 font-medium text-accent-foreground transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
      >
        {submitting ? "提交中…" : "开始制作"}
      </button>

      <p aria-live="polite" className="text-center text-xs">
        {createTask.isError ? (
          <span className="text-danger">
            提交失败：{createTask.error.message}
          </span>
        ) : createTask.isSuccess && readyCount === 0 ? (
          <span className="text-accent">
            任务 #{createTask.data.taskId} 已提交，可在制作记录中查看进度
          </span>
        ) : (
          <span className="text-muted">
            {uploading
              ? "图片上传中，请稍候"
              : assetIds === null
                ? `还需上传 ${slots.length - readyCount} 张图片`
                : "素材已就绪"}
          </span>
        )}
      </p>
    </section>
  );
}
