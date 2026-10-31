// 所有 query key 在这里统一定义。和用户有关的 key 第一段是 userId（见 docs/conventions.md 3.4），
// 切换用户后旧用户的请求即使晚返回，也只会写进旧 key。
export const qk = {
  me: ["me"] as const,
};
