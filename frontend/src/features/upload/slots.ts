// 上传位的状态机。纯函数，不做任何副作用（请求、object URL 的创建和释放都在 use-upload-slots 里）。
//
//   empty ──select──▶ uploading ──succeeded──▶ ready
//     ▲                  │  └──────failed────▶ error ──retry(select)──▶ uploading
//     │                  │
//     └──── remove ◀─────┴─ 任意状态都可以 remove / 重新 select（替换）
//
// 每次开始上传都分配新的 uploadId。替换或移除后，旧请求即使晚返回，
// uploadId 对不上也会被忽略，不会覆盖新的状态。

export type Slot =
  | { kind: "empty" }
  | { kind: "uploading"; file: File; previewUrl: string; uploadId: number }
  | { kind: "ready"; file: File; previewUrl: string; assetId: number }
  | {
      kind: "error";
      file: File;
      /** 前端校验不通过的文件不生成预览 */
      previewUrl: string | null;
      message: string;
      /** 网络错误、服务器错误可以重试；文件本身不合格时重试没有意义 */
      retryable: boolean;
    };

export type SlotAction =
  | {
      type: "started";
      index: number;
      file: File;
      previewUrl: string;
      uploadId: number;
    }
  | { type: "rejected"; index: number; file: File; message: string }
  | { type: "succeeded"; index: number; uploadId: number; assetId: number }
  | {
      type: "failed";
      index: number;
      uploadId: number;
      message: string;
      retryable: boolean;
    }
  | { type: "removed"; index: number }
  | { type: "reset"; count: number };

export function initSlots(count: number): Slot[] {
  return Array.from({ length: count }, () => ({ kind: "empty" }));
}

export function slotsReducer(slots: Slot[], action: SlotAction): Slot[] {
  if (action.type === "reset") return initSlots(action.count);
  if (action.index < 0 || action.index >= slots.length) return slots;

  const current = slots[action.index];
  const next = reduceSlot(current!, action);
  if (next === current) return slots;
  return slots.map((s, i) => (i === action.index ? next : s));
}

function reduceSlot(
  slot: Slot,
  action: Exclude<SlotAction, { type: "reset" }>,
): Slot {
  switch (action.type) {
    case "started":
      return {
        kind: "uploading",
        file: action.file,
        previewUrl: action.previewUrl,
        uploadId: action.uploadId,
      };
    case "rejected":
      return {
        kind: "error",
        file: action.file,
        previewUrl: null,
        message: action.message,
        retryable: false,
      };
    case "succeeded":
      if (!isCurrentUpload(slot, action.uploadId)) return slot;
      return {
        kind: "ready",
        file: slot.file,
        previewUrl: slot.previewUrl,
        assetId: action.assetId,
      };
    case "failed":
      if (!isCurrentUpload(slot, action.uploadId)) return slot;
      return {
        kind: "error",
        file: slot.file,
        previewUrl: slot.previewUrl,
        message: action.message,
        retryable: action.retryable,
      };
    case "removed":
      return slot.kind === "empty" ? slot : { kind: "empty" };
  }
}

function isCurrentUpload(
  slot: Slot,
  uploadId: number,
): slot is Extract<Slot, { kind: "uploading" }> {
  return slot.kind === "uploading" && slot.uploadId === uploadId;
}

/** 所有上传位都已上传成功时返回按顺序排列的 assetId，否则返回 null。 */
export function readyAssetIds(slots: Slot[]): number[] | null {
  const ids: number[] = [];
  for (const slot of slots) {
    if (slot.kind !== "ready") return null;
    ids.push(slot.assetId);
  }
  return ids;
}

export function previewUrlOf(slot: Slot): string | null {
  return slot.kind === "empty" ? null : slot.previewUrl;
}
