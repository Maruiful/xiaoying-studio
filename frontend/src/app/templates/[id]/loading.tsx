export default function Loading() {
  return (
    <main
      aria-busy="true"
      aria-label="模板加载中"
      className="mx-auto grid max-w-[1600px] gap-4 px-4 py-6 sm:px-6 md:grid-cols-[minmax(320px,420px)_1fr]"
    >
      <div className="flex flex-col gap-4 rounded-2xl bg-surface p-5">
        <div className="h-6 w-40 animate-pulse rounded bg-surface-raised" />
        <div className="aspect-[16/10] animate-pulse rounded-xl bg-surface-raised" />
        <div className="h-16 animate-pulse rounded bg-surface-raised" />
      </div>
      <div className="min-h-64 animate-pulse rounded-2xl bg-surface" />
    </main>
  );
}
