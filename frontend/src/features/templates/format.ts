const MIME_LABELS: Record<string, string> = {
  "image/jpeg": "JPG",
  "image/png": "PNG",
  "image/webp": "WebP",
};

/** ["image/jpeg", "image/png"] → "JPG、PNG" */
export function formatMimeTypes(mimeTypes: string[]): string {
  return mimeTypes.map((m) => MIME_LABELS[m] ?? m).join("、");
}

/** 5242880 → "5MB"，1536 → "1.5KB" */
export function formatBytes(bytes: number): string {
  const units = ["B", "KB", "MB", "GB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  const rounded = Number.isInteger(value) ? value : Number(value.toFixed(1));
  return `${rounded}${units[unit]}`;
}
