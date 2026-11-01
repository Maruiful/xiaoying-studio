import type { Metadata } from "next";
import { TaskHistory } from "@/features/tasks/task-history";

export const metadata: Metadata = { title: "制作记录" };

/** 独立的历史页，和详情页右栏共用 TaskHistory。 */
export default function HistoryPage() {
  return (
    <main className="mx-auto max-w-[1600px] p-2">
      <section
        aria-labelledby="history-page-heading"
        className="rounded-2xl bg-surface p-5"
      >
        <TaskHistory headingId="history-page-heading" />
      </section>
    </main>
  );
}
