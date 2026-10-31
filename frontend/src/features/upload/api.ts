import { apiFetch } from "@/lib/api-client";

// 与后端 AssetVO 对应
export type Asset = {
  id: number;
  templateId: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  url: string;
  createdAt: string;
};

/** 上传一张图片。不手动设置 content-type，由浏览器生成带 boundary 的 multipart 头。 */
export function uploadAsset(
  templateId: string,
  file: File,
  signal: AbortSignal,
): Promise<Asset> {
  const form = new FormData();
  form.append("templateId", templateId);
  form.append("file", file);
  return apiFetch("/api/assets", { method: "POST", body: form, signal });
}
