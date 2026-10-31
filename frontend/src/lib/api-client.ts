// 统一的请求函数。组件不直接调用 fetch，统一通过各模块 api.ts 中的函数请求。
//
// 两类错误都属于"请求异常"，和任务的 FAILED 状态是两回事：
// - ApiError：后端（或转发层）返回了非 2xx，带有结构化的 code 和 message
// - NetworkError：请求没有拿到响应，比如断网、后端不可达

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export class NetworkError extends Error {
  constructor(message = "网络连接失败，请检查网络后重试") {
    super(message);
    this.name = "NetworkError";
  }
}

export async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(url, init);
  } catch (error) {
    // 主动取消的请求原样抛出，调用方据此忽略结果
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new NetworkError();
  }

  if (!res.ok) {
    throw await toApiError(res);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

async function toApiError(res: Response): Promise<ApiError> {
  const body: unknown = await res.json().catch(() => null);
  if (
    body &&
    typeof body === "object" &&
    "code" in body &&
    "message" in body &&
    typeof body.code === "string" &&
    typeof body.message === "string"
  ) {
    const details =
      "details" in body && body.details && typeof body.details === "object"
        ? (body.details as Record<string, unknown>)
        : undefined;
    return new ApiError(res.status, body.code, body.message, details);
  }
  return new ApiError(
    res.status,
    "HTTP_ERROR",
    `请求失败（HTTP ${res.status}）`,
  );
}
