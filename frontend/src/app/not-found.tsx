import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto max-w-md px-6 py-24 text-center">
      <h1 className="text-lg font-medium">页面不存在</h1>
      <Link
        href="/"
        className="mt-6 inline-block rounded-lg bg-surface-raised px-4 py-2 text-sm"
      >
        返回模板列表
      </Link>
    </main>
  );
}
