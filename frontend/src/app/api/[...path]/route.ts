import { proxyToBackend } from "@/lib/backend-proxy";

// 每次请求都在运行时转发，不做静态化或缓存
export const dynamic = "force-dynamic";

type Context = { params: Promise<{ path: string[] }> };

async function handler(request: Request, { params }: Context) {
  const { path } = await params;
  return proxyToBackend(request, path);
}

export {
  handler as GET,
  handler as POST,
  handler as PUT,
  handler as PATCH,
  handler as DELETE,
};
