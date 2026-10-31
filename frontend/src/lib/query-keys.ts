// 所有 query key 在这里统一定义。和用户有关的 key 第一段是 userId（见 docs/conventions.md 3.4），
// 切换用户后旧用户的请求即使晚返回，也只会写进旧 key。
export const qk = {
  me: ["me"] as const,
  tasks: (userId: string) => [userId, "tasks"] as const,
  /** 单个任务；前缀 [userId, "task"] 可用于批量刷新该用户的全部任务查询 */
  task: (userId: string, id: number) => [userId, "task", id] as const,
  allTasks: (userId: string) => [userId, "task"] as const,
};
