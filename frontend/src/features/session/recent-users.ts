// 最近使用过的用户 ID，保存在 localStorage，供"切换演示用户"选择。
// 只是演示用的本地便利功能，真正的身份以后端的 HttpOnly cookie 为准。

const STORAGE_KEY = "xiaoying.recentUsers";
export const MAX_RECENT = 8;

const USER_ID =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

type Storage = Pick<globalThis.Storage, "getItem" | "setItem">;

function defaultStorage(): Storage | null {
  try {
    return typeof window === "undefined" ? null : window.localStorage;
  } catch {
    // 部分浏览器禁用存储时访问 localStorage 会抛异常
    return null;
  }
}

export function loadRecentUsers(
  storage: Storage | null = defaultStorage(),
): string[] {
  if (!storage) return [];
  try {
    const parsed: unknown = JSON.parse(storage.getItem(STORAGE_KEY) ?? "[]");
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (v): v is string => typeof v === "string" && USER_ID.test(v),
    );
  } catch {
    return [];
  }
}

/** 把用户放到列表最前面，去重并截断。 */
export function rememberUser(
  id: string,
  storage: Storage | null = defaultStorage(),
): void {
  if (!storage) return;
  const next = [id, ...loadRecentUsers(storage).filter((u) => u !== id)].slice(
    0,
    MAX_RECENT,
  );
  try {
    storage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // 存储已满或被禁用时放弃记录，不影响身份本身
  }
}
