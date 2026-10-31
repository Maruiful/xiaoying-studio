import { describe, expect, it } from "vitest";
import { validateFile } from "./validate";

const req = {
  acceptedMimeTypes: ["image/jpeg", "image/png"],
  maxFileSizeBytes: 5 * 1024 * 1024,
};

describe("validateFile", () => {
  it("类型和大小都符合时返回 null", () => {
    expect(validateFile({ type: "image/png", size: 1024 }, req)).toBeNull();
  });

  it("类型不在模板允许范围内", () => {
    expect(validateFile({ type: "image/webp", size: 1024 }, req)).toBe(
      "只支持 JPG、PNG 格式的图片",
    );
    // 浏览器认不出类型时 file.type 是空字符串
    expect(validateFile({ type: "", size: 1024 }, req)).not.toBeNull();
  });

  it("超过大小上限", () => {
    expect(
      validateFile({ type: "image/jpeg", size: 5 * 1024 * 1024 + 1 }, req),
    ).toBe("图片不能超过 5MB");
  });

  it("正好等于上限时允许", () => {
    expect(
      validateFile({ type: "image/jpeg", size: 5 * 1024 * 1024 }, req),
    ).toBeNull();
  });

  it("空文件", () => {
    expect(validateFile({ type: "image/jpeg", size: 0 }, req)).toBe(
      "图片文件为空",
    );
  });
});
