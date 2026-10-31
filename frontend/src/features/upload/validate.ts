import { formatBytes, formatMimeTypes } from "@/features/templates/format";
import type { TemplateDetail } from "@/features/templates/types";

type Requirements = Pick<
  TemplateDetail,
  "acceptedMimeTypes" | "maxFileSizeBytes"
>;

/**
 * 选择文件后先在前端校验，不合格就直接提示，不发请求。
 * file.type 由浏览器根据扩展名推断，可能不准；服务端会按文件头再校验一次，以服务端为准。
 *
 * @returns 不合格时返回给用户看的原因，合格时返回 null
 */
export function validateFile(
  file: Pick<File, "type" | "size">,
  req: Requirements,
): string | null {
  if (!req.acceptedMimeTypes.includes(file.type)) {
    return `只支持 ${formatMimeTypes(req.acceptedMimeTypes)} 格式的图片`;
  }
  if (file.size === 0) {
    return "图片文件为空";
  }
  if (file.size > req.maxFileSizeBytes) {
    return `图片不能超过 ${formatBytes(req.maxFileSizeBytes)}`;
  }
  return null;
}
