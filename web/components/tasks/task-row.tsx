"use client";

import { CalendarDays, Repeat } from "lucide-react";
import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import type { Task } from "@/lib/api/client";
import { formatShortDate } from "@/lib/dates";
import { StateChip, TagChip } from "@/components/ui/chip";
import { TaskState } from "./task-state";

export function TaskRow({ task, href }: { task: Task; href: string }) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  const locale = useLocale();
  const owners = task.owners.map((owner) => owner.name).join(", ");

  return (
    <li className="relative flex flex-col gap-2 px-4 py-4 transition-colors duration-[120ms] hover:bg-surface-muted md:flex-row md:items-start md:gap-6 lg:px-5">
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <Link
          href={href}
          className="font-semibold break-words no-underline after:absolute after:inset-0 hover:underline"
        >
          {task.title}
        </Link>
        <p className="text-small text-ink-muted">
          {task.unassigned || task.owners.length === 0 ? (
            <StateChip tone="neutral">{tStatus("unassigned")}</StateChip>
          ) : (
            <span>{t("owners", { names: owners })}</span>
          )}
        </p>
        {task.tags.length > 0 && (
          <ul className="flex flex-wrap gap-1.5">
            {task.tags.map((tag) => (
              <li key={tag.id}>
                <TagChip>{tag.name}</TagChip>
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 md:w-72 md:justify-end">
        <span className="inline-flex items-center gap-1.5 text-small text-ink-muted">
          <CalendarDays aria-hidden className="size-4" />
          {task.dueDate ? t("due", { date: formatShortDate(task.dueDate, locale) }) : t("noDue")}
        </span>
        {task.recurrence?.active && (
          <span className="inline-flex items-center gap-1.5 text-small text-ink-muted">
            <Repeat aria-hidden className="size-4" />
            {t("repeats")}
          </span>
        )}
        <TaskState task={task} />
      </div>
    </li>
  );
}
