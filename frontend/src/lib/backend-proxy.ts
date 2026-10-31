// 把同源的 /api/* 请求原样转发给 Java 后端。业务逻辑全部在 Java，这里只做转发。
// 后端地址在运行时读取 BACKEND_URL，同一个镜像可以部署到不同环境。

// 这些头只对单跳连接有意义，或者会由 fetch 重新计算，不能原样转发
const HOP_BY_HOP_HEADERS = [
  "connection",
  "keep-alive",
  "transfer-encoding",
  "upgrade",
  "proxy-connection",
  "te",
  "trailer",
  "host",
  "content-length",
];

// Node 的 fetch 会自动解压响应体，所以不能保留原来的压缩相关头
const STRIPPED_RESPONSE_HEADERS = [...HOP_BY_HOP_HEADERS, "content-encoding"];

export function backendUrl(): string {
  return (process.env.BACKEND_URL ?? "http://localhost:8080").replace(
    /\/+$/,
    "",
  );
}

export async function proxyToBackend(
  request: Request,
  pathSegments: string[],
): Promise<Response> {
  if (pathSegments.some((s) => s === ".." || s === "." || s === "")) {
    return errorResponse(400, "BAD_PATH", "非法的请求路径");
  }

  const incoming = new URL(request.url);
  const path = pathSegments.map(encodeURIComponent).join("/");
  const target = `${backendUrl()}/api/${path}${incoming.search}`;

  const headers = new Headers(request.headers);
  for (const name of HOP_BY_HOP_HEADERS) headers.delete(name);

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? request.body : undefined,
      redirect: "manual",
      cache: "no-store",
      // 流式转发请求体（上传文件）时 Node 要求声明 duplex
      ...(hasBody ? { duplex: "half" } : {}),
    } as RequestInit);
  } catch {
    return errorResponse(502, "BACKEND_UNAVAILABLE", "后端服务暂时不可用");
  }

  const responseHeaders = new Headers();
  upstream.headers.forEach((value, name) => {
    if (name === "set-cookie") return;
    if (STRIPPED_RESPONSE_HEADERS.includes(name)) return;
    responseHeaders.set(name, value);
  });
  // 多个 Set-Cookie 必须逐个追加，合并成一个头会被浏览器解析错误
  for (const cookie of upstream.headers.getSetCookie()) {
    responseHeaders.append("set-cookie", cookie);
  }

  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  });
}

function errorResponse(
  status: number,
  code: string,
  message: string,
): Response {
  return Response.json({ code, message }, { status });
}
