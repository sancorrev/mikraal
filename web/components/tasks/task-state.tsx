import { useTranslations } from "next-intl";
import type { Task } from "@/lib/api/client";
import { StateChip } from "@/components/ui/chip";

/** Status word, plus «Vencida» with the warning style when an open task is past its deadline. */
export function TaskState({ task }: { task: Pick<Task, "status" | "overdue"> }) {
  const t = useTranslations("status");
  const showOverdue = task.overdue && task.status !== "finished";
  return (
    <span className="inline-flex flex-wrap gap-2">
      {showOverdue && <StateChip tone="warning">{t("overdue")}</StateChip>}
      <StateChip tone={task.status === "finished" ? "done" : "neutral"}>{t(task.status)}</StateChip>
    </span>
  );
}
