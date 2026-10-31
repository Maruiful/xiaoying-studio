import { describe, expect, it } from "vitest";
import { loadRecentUsers, MAX_RECENT, rememberUser } from "./recent-users";

function memoryStorage(initial?: string) {
  const data = new Map<string, string>();
  if (initial !== undefined) data.set("xiaoying.recentUsers", initial);
  return {
    getItem: (k: string) => data.get(k) ?? null,
    setItem: (k: string, v: string) => void data.set(k, v),
  };
}

const ID_A = "aaaaaaaa-0000-0000-0000-000000000000";
const ID_B = "bbbbbbbb-0000-0000-0000-000000000000";

describe("recent users", () => {
  it("新用户放在最前面，重复的不会出现两次", () => {
    const s = memoryStorage();
    rememberUser(ID_A, s);
    rememberUser(ID_B, s);
    rememberUser(ID_A, s);

    expect(loadRecentUsers(s)).toEqual([ID_A, ID_B]);
  });

  it(`最多保留 ${MAX_RECENT} 个`, () => {
    const s = memoryStorage();
    for (let i = 0; i < MAX_RECENT + 3; i++) {
      rememberUser(
        `${String(i).padStart(8, "0")}-0000-0000-0000-000000000000`,
        s,
      );
    }

    expect(loadRecentUsers(s)).toHaveLength(MAX_RECENT);
  });

  it.each([
    ["不是 JSON", "{oops"],
    ["不是数组", '{"a":1}'],
  ])("存储内容%s时视为空列表", (_, raw) => {
    expect(loadRecentUsers(memoryStorage(raw))).toEqual([]);
  });

  it("过滤掉不是用户 ID 的元素", () => {
    const s = memoryStorage(JSON.stringify([ID_A, 42, "x", ID_B]));

    expect(loadRecentUsers(s)).toEqual([ID_A, ID_B]);
  });

  it("没有可用存储时不报错", () => {
    expect(loadRecentUsers(null)).toEqual([]);
    expect(() => rememberUser(ID_A, null)).not.toThrow();
  });
});
