"use client";

import {
  type UseQueryResult,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { qk } from "@/lib/query-keys";
import { useHydrated } from "@/lib/use-hydrated";
import { createUser, getMe, switchUser } from "./api";
import { rememberUser } from "./recent-users";
import type { User } from "./types";

/**
 * 当前用户。首次请求时后端会创建匿名用户并签发 cookie。
 * 水合完成前一律返回"加载中"，与服务端渲染的 HTML 一致（原因见 useHydrated）。
 */
export function useMe(): UseQueryResult<User, Error> {
  const query = useQuery({
    queryKey: qk.me,
    queryFn: async () => {
      const user = await getMe();
      rememberUser(user.id);
      return user;
    },
    // 身份只在新建或切换时变化，由下面的 mutation 主动更新
    staleTime: Infinity,
  });
  const hydrated = useHydrated();
  if (hydrated) return query;
  return {
    ...query,
    data: undefined,
    error: null,
    status: "pending",
    isPending: true,
    isLoading: true,
    isSuccess: false,
    isError: false,
    isLoadingError: false,
    isRefetchError: false,
    isPlaceholderData: false,
  } as UseQueryResult<User, Error>;
}

function useApplyUser() {
  const queryClient = useQueryClient();
  return async (user: User) => {
    // 先取消进行中的请求（包括轮询），再删掉上一个用户的数据，最后更新当前用户。
    // 和用户有关的 key 都以 userId 开头，旧请求即使晚返回也只会写进旧用户的 key，
    // 而旧 key 已经被删除，不会显示出来。
    //
    // 不能用 queryClient.clear()：它会连 me 查询一起删掉，正在使用 useMe() 的组件
    // 还挂在被删的旧查询上，收不到新用户，直到碰巧重新渲染。所以保留 me 查询，
    // 在它上面 setQueryData，所有观察者都会收到通知。
    await queryClient.cancelQueries();
    queryClient.removeQueries({
      predicate: (query) => query.queryKey[0] !== qk.me[0],
    });
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
