import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "小影素材工坊",
  description: "浏览模板、上传图片，模拟生成素材",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen antialiased">{children}</body>
    </html>
  );
}
