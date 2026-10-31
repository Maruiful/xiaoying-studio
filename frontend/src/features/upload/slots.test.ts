import { describe, expect, it } from "vitest";
import { initSlots, readyAssetIds, type Slot, slotsReducer } from "./slots";

const fileA = new File(["a"], "a.png", { type: "image/png" });
const fileB = new File(["b"], "b.png", { type: "image/png" });

function start(slots: Slot[], index: number, uploadId: number, file = fileA) {
  return slotsReducer(slots, {
    type: "started",
    index,
    file,
    previewUrl: `blob:${uploadId}`,
    uploadId,
  });
}

describe("slotsReducer", () => {
  it("empty → uploading → ready", () => {
    let slots = initSlots(2);
    slots = start(slots, 0, 1);
    expect(slots[0]).toMatchObject({ kind: "uploading", uploadId: 1 });

    slots = slotsReducer(slots, {
      type: "succeeded",
      index: 0,
      uploadId: 1,
      assetId: 42,
    });
    expect(slots[0]).toEqual({
      kind: "ready",
      file: fileA,
      previewUrl: "blob:1",
      assetId: 42,
    });
    expect(slots[1]).toEqual({ kind: "empty" });
  });

  it("上传失败后保留文件和预览，可以重试", () => {
    let slots = start(initSlots(1), 0, 1);
    slots = slotsReducer(slots, {
      type: "failed",
      index: 0,
      uploadId: 1,
      message: "网络连接失败",
      retryable: true,
    });

    expect(slots[0]).toEqual({
      kind: "error",
      file: fileA,
      previewUrl: "blob:1",
      message: "网络连接失败",
      retryable: true,
    });

    slots = start(slots, 0, 2);
    expect(slots[0]).toMatchObject({ kind: "uploading", uploadId: 2 });
  });

  it("前端校验不通过时直接进入 error，不可重试", () => {
    const slots = slotsReducer(initSlots(1), {
      type: "rejected",
      index: 0,
      file: fileA,
      message: "只支持 JPG 格式的图片",
    });

    expect(slots[0]).toMatchObject({
      kind: "error",
      previewUrl: null,
      retryable: false,
    });
  });

  it("替换后，旧上传的结果晚返回也会被忽略", () => {
    let slots = start(initSlots(1), 0, 1, fileA);
    slots = start(slots, 0, 2, fileB);

    const afterStale = slotsReducer(slots, {
      type: "succeeded",
      index: 0,
      uploadId: 1,
      assetId: 1,
    });
    expect(afterStale).toBe(slots);

    const afterStaleFail = slotsReducer(slots, {
      type: "failed",
      index: 0,
      uploadId: 1,
      message: "x",
      retryable: true,
    });
    expect(afterStaleFail).toBe(slots);

    slots = slotsReducer(slots, {
      type: "succeeded",
      index: 0,
      uploadId: 2,
      assetId: 2,
    });
    expect(slots[0]).toMatchObject({ kind: "ready", file: fileB, assetId: 2 });
  });

  it("移除后，进行中的上传返回结果也不会恢复", () => {
    let slots = start(initSlots(1), 0, 1);
    slots = slotsReducer(slots, { type: "removed", index: 0 });
    expect(slots[0]).toEqual({ kind: "empty" });

    slots = slotsReducer(slots, {
      type: "succeeded",
      index: 0,
      uploadId: 1,
      assetId: 1,
    });
    expect(slots[0]).toEqual({ kind: "empty" });
  });

  it("各上传位互不影响", () => {
    let slots = start(initSlots(3), 0, 1);
    slots = start(slots, 2, 2);
    slots = slotsReducer(slots, {
      type: "failed",
      index: 2,
      uploadId: 2,
      message: "x",
      retryable: true,
    });

    expect(slots.map((s) => s.kind)).toEqual(["uploading", "empty", "error"]);
  });

  it("越界的 index 不改变状态", () => {
    const slots = initSlots(1);

    expect(slotsReducer(slots, { type: "removed", index: 5 })).toBe(slots);
  });

  it("reset 清空为指定数量的空上传位", () => {
    const slots = slotsReducer(start(initSlots(2), 0, 1), {
      type: "reset",
      count: 2,
    });

    expect(slots).toEqual([{ kind: "empty" }, { kind: "empty" }]);
  });
});

describe("readyAssetIds", () => {
  it("全部就绪时按上传位顺序返回 assetId", () => {
    const slots: Slot[] = [
      { kind: "ready", file: fileA, previewUrl: "blob:1", assetId: 9 },
      { kind: "ready", file: fileB, previewUrl: "blob:2", assetId: 3 },
    ];

    expect(readyAssetIds(slots)).toEqual([9, 3]);
  });

  it("有任何一个未就绪时返回 null", () => {
    const slots: Slot[] = [
      { kind: "ready", file: fileA, previewUrl: "blob:1", assetId: 9 },
      { kind: "uploading", file: fileB, previewUrl: "blob:2", uploadId: 1 },
    ];

    expect(readyAssetIds(slots)).toBeNull();
    expect(readyAssetIds(initSlots(1))).toBeNull();
  });
});
