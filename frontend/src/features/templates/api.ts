import "server-only";
import { cache } from "react";
import { ApiError, apiFetch } from "@/lib/api-client";
import { backendUrl } from "@/lib/backend-url";
import type { Category, TemplateCard, TemplateDetail } from "./types";

// 模板数据与用户无关，由 Server Component 在服务端直接请求 Java，不经过 /api 转发。

const noStore = { cache: "no-store" } as const;

export function getCategories(): Promise<Category[]> {
  return apiFetch(`${backendUrl()}/api/categories`, noStore);
}

export function getTemplates(category?: string): Promise<TemplateCard[]> {
  const query = category ? `?category=${encodeURIComponent(category)}` : "";
  return apiFetch(`${backendUrl()}/api/templates${query}`, noStore);
}

/** 模板不存在时返回 null，其他错误继续抛出。同一次渲染内去重（generateMetadata 和页面共用）。 */
export const getTemplate = cache(
  async (id: string): Promise<TemplateDetail | null> => {
    try {
      return await apiFetch<TemplateDetail>(
        `${backendUrl()}/api/templates/${encodeURIComponent(id)}`,
        noStore,
      );
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) return null;
      throw error;
    }
  },
);
