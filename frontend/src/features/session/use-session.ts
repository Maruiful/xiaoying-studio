"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { qk } from "@/lib/query-keys";
import { createUser, getMe, switchUser } from "./api";
import { rememberUser } from "./recent-users";
import type { User } from "./types";

/** 当前用户。首次请求时后端会创建匿名用户并签发 cookie。 */
export function useMe() {
  return useQuery({
    queryKey: qk.me,
    queryFn: async () => {
      const user = await getMe();
      rememberUser(user.id);
      return user;
    },
    // 身份只在新建或切换时变化，由下面的 mutation 主动更新
    staleTime: Infinity,
  });
}

function useApplyUser() {
  const queryClient = useQueryClient();
  return (user: User) => {
    // 阶段 5：在这里取消并清空上一个用户的任务查询，避免旧请求写入新用户的视图
    queryClient.setQueryData(qk.me, user);
    rememberUser(user.id);
  };
}

export function useCreateUser() {
  const apply = useApplyUser();
  return useMutation({ mutationFn: createUser, onSuccess: apply });
}

export function useSwitchUser() {
  const apply = useApplyUser();
  return useMutation({ mutationFn: switchUser, onSuccess: apply });
}
