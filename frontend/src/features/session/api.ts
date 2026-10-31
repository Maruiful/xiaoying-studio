import { apiFetch } from "@/lib/api-client";
import type { User } from "./types";

// 浏览器端经同源 /api 转发调用，cookie 由浏览器自动携带

export function getMe(): Promise<User> {
  return apiFetch("/api/me", { cache: "no-store" });
}

export function createUser(): Promise<User> {
  return apiFetch("/api/users", { method: "POST" });
}

export function switchUser(userId: string): Promise<User> {
  return apiFetch("/api/session/switch", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ userId }),
  });
}
