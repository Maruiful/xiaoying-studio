// Java 后端地址，运行时读取。只在服务端使用（转发 Route Handler 和 Server Component）。
export function backendUrl(): string {
  return (process.env.BACKEND_URL ?? "http://localhost:8080").replace(
    /\/+$/,
    "",
  );
}
