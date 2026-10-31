import { describe, expect, it } from "vitest";
import { formatBytes, formatMimeTypes } from "./format";

describe("formatMimeTypes", () => {
  it("把常见 MIME 转成简短名称，未知类型原样显示", () => {
    expect(formatMimeTypes(["image/jpeg", "image/png", "image/gif"])).toBe(
      "JPG、PNG、image/gif",
    );
  });
});

describe("formatBytes", () => {
  it.each([
    [500, "500B"],
    [1536, "1.5KB"],
    [2097152, "2MB"],
    [5242880, "5MB"],
  ])("%d → %s", (bytes, expected) => {
    expect(formatBytes(bytes)).toBe(expected);
  });
});
