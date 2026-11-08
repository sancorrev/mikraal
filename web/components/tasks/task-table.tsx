"use client";

import { Repeat } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import type { Task } from "@/lib/api/client";
import { cn } from "@/lib/utils";
import { StateChip } from "@/components/ui/chip";
import { DueChip, OwnerAvatars } from "./task-meta";

const columns = ["task", "tags", "status", "due", "owners"] as const;
const widths: Record<(typeof columns)[number], string> = {
  task: "",
  tags: "w-[20%]",
  status: "w-36",
  due: "w-44",
  owners: "w-36",
};
const cell = "px-3 py-2.5 align-middle";

/** The list view: one row per task; the table scrolls sideways on narrow screens. */
export function TaskTable({ tasks, base, today }: { tasks: Task[]; base: string; today: string }) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  const tCommon = useTranslations("common");

  return (
    <div className="overflow-x-auto rounded-card border border-border bg-surface-raised p-2">
      <table className="w-full min-w-[760px] border-collapse text-left">
        <caption className="sr-only">{t("metaTitle")}</caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column}
                scope="col"
                className={cn(cell, "text-overline font-semibold text-ink-muted uppercase", widths[column])}
              >
                {t(`table.${column}`)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr
              key={task.id}
              className="border-t border-border transition-colors duration-[120ms] hover:bg-[color-mix(in_srgb,var(--ink)_3%,transparent)]"
            >
              <td className={cell}>
                <span className="flex items-center gap-2">
                  <Link href={`${base}/${task.id}`} className="font-semibold break-words no-underline hover:underline">
                    {task.title}
                  </Link>
                  {task.recurrence?.active && (
                    <span className="inline-flex text-ink-muted" title={t("repeats")}>
                      <Repeat aria-hidden className="size-4" />
                      <span className="sr-only">{t("repeats")}</span>
                    </span>
                  )}
                </span>
              </td>
              <td className={cn(cell, "text-small text-ink-muted")}>
                {task.tags.length > 0 ? task.tags.map((tag) => tag.name).join(" · ") : tCommon("none")}
              </td>
              <td className={cell}>
                <StateChip tone={task.status === "finished" ? "done" : "neutral"}>{tStatus(task.status)}</StateChip>
              </td>
              <td className={cell}>
                <DueChip task={task} today={today} />
              </td>
              <td className={cell}>
                <OwnerAvatars task={task} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
