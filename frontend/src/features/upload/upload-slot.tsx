"use client";

import { useId, useRef } from "react";
import type { Slot } from "./slots";

type Props = {
  index: number;
  slot: Slot;
  accept: string;
  /** 提交任务期间禁止修改 */
  disabled?: boolean;
  onSelect: (file: File) => void;
  onRemove: () => void;
};

/** 单个上传位。用 label 包住隐藏的 file input，键盘可以聚焦并用回车或空格选择文件。 */
export function UploadSlot({
  index,
  slot,
  accept,
  disabled = false,
  onSelect,
  onRemove,
}: Props) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const label = `第 ${index + 1} 张图片`;

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    // 清空 value，重新选择同一个文件时也能触发 change
    e.target.value = "";
    if (file) onSelect(file);
  }

  const input = (
    <input
      ref={inputRef}
      id={inputId}
      type="file"
      accept={accept}
      disabled={disabled}
      onChange={handleChange}
      className="sr-only"
    />
  );

  if (slot.kind === "empty") {
    return (
      <label
        htmlFor={inputId}
        aria-disabled={disabled}
        className="flex aspect-square cursor-pointer aria-disabled:cursor-not-allowed aria-disabled:opacity-50 flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-surface-raised text-sm text-muted transition-colors hover:border-accent hover:text-foreground has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-accent"
      >
        {input}
        <span
          aria-hidden="true"
          className="grid h-9 w-9 place-items-center rounded-full bg-surface text-lg"
        >
          +
        </span>
        <span>点击上传{label}</span>
      </label>
    );
  }

  const status = statusText(slot);

  return (
    <div className="relative aspect-square overflow-hidden rounded-xl border border-border bg-surface-raised">
      {input}
      {slot.previewUrl && (
        <img
          src={slot.previewUrl}
          alt={`${label}预览`}
          className={
            slot.kind === "ready"
              ? "h-full w-full object-cover"
              : "h-full w-full object-cover opacity-40"
          }
        />
      )}

      <div
        aria-live="polite"
        className="absolute inset-0 flex flex-col items-center justify-center gap-2 p-3 text-center"
      >
        {slot.kind === "uploading" && (
          <span className="rounded bg-black/60 px-2 py-1 text-xs">上传中…</span>
        )}
        {slot.kind === "error" && (
          <p
            role="alert"
            className="rounded bg-black/70 px-2 py-1 text-xs text-danger"
          >
            {slot.message}
          </p>
        )}
        <span className="sr-only">
          {label}：{status}
        </span>
      </div>

      <div className="absolute inset-x-0 bottom-0 flex justify-end gap-1 bg-linear-to-t from-black/70 to-transparent p-2 pt-6">
        {slot.kind === "error" && slot.retryable && (
          <button
            type="button"
            onClick={() => onSelect(slot.file)}
            disabled={disabled}
            className="rounded-lg bg-black/60 px-2 py-1 text-xs hover:bg-black/80 disabled:opacity-50"
          >
            重试
          </button>
        )}
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={disabled}
          aria-label={`替换${label}`}
          className="rounded-lg bg-black/60 px-2 py-1 text-xs hover:bg-black/80 disabled:opacity-50"
        >
          替换
        </button>
        <button
          type="button"
          onClick={onRemove}
          disabled={disabled}
          aria-label={`移除${label}`}
          className="rounded-lg bg-black/60 px-2 py-1 text-xs hover:bg-black/80 disabled:opacity-50"
        >
          移除
        </button>
      </div>
    </div>
  );
}

function statusText(slot: Slot): string {
  switch (slot.kind) {
    case "empty":
      return "未选择";
    case "uploading":
      return "上传中";
    case "ready":
      return "已上传";
    case "error":
      return `上传失败，${slot.message}`;
  }
}
