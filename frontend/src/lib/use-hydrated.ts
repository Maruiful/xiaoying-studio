"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * 组件是否已完成水合。服务端渲染和水合期间返回 false，水合完成后返回 true。
 *
 * 用途：只在浏览器里才有的数据（如 useMe() 的结果）不能在水合时直接渲染。
 * 详情页的内容在 Suspense 边界里流式到达，边界水合可能晚于 /api/me 返回；
 * TanStack Query 的 useQuery 在水合时也读当前缓存，于是客户端首次渲染出"已加载"，
 * 服务端 HTML 却是"加载中"，React 报 #418。水合期间先按"加载中"渲染即可一致。
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
