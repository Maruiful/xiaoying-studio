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
  return async (user: User) => {
    // 先取消进行中的请求（包括轮询），再清空缓存，最后写入新用户。
    // 和用户有关的 key 都以 userId 开头，旧请求即使晚返回也只会写进旧用户的 key，
    // 而旧 key 已经没有观察者，不会显示出来。
    await queryClient.cancelQueries();
    queryClient.clear();
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
