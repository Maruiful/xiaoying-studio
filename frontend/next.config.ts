import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker 构建时生成自包含的 server.js，运行镜像只需复制 .next/standalone。
  // 本地 Windows 上 standalone 需要创建符号链接权限，所以只在设置了环境变量时开启。
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
};

export default nextConfig;
