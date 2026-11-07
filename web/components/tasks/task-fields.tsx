"use client";

import { useLocale, useTranslations } from "next-intl";
import type { ReactNode } from "react";
import type { Task } from "@/lib/api/client";
import { formatLongDate } from "@/lib/dates";
import { StateChip, TagChip } from "@/components/ui/chip";

/** All task fields with names, as a description list. */
export function TaskFields({ task }: { task: Task }) {
  const t = useTranslations("detail");
  const tStatus = useTranslations("status");
  const tRecurrence = useTranslations("recurrence");
  const locale = useLocale();

  const recurrence = task.recurrence
    ? task.recurrence.active
      ? tRecurrence(task.recurrence.period)
      : tRecurrence("ended", { period: tRecurrence(task.recurrence.period) })
    : tRecurrence("none");

  return (
    <dl className="grid gap-x-6 gap-y-5 md:grid-cols-[180px_1fr]">
      <Row label={t("description")}>
        {task.description ? (
          <p className="whitespace-pre-line">{task.description}</p>
        ) : (
          <Muted>{t("noDescription")}</Muted>
        )}
      </Row>
      <Row label={t("owners")}>
        {task.owners.length === 0 ? (
          <StateChip tone="neutral">{tStatus("unassigned")}</StateChip>
        ) : (
          <ul className="flex flex-col gap-1">
            {task.owners.map((owner) => (
              <li key={`${owner.type}:${owner.id}`}>
                {owner.name} <span className="text-small text-ink-muted">· {t(`ownerType.${owner.type}`)}</span>
              </li>
            ))}
          </ul>
        )}
      </Row>
      <Row label={t("dueDate")}>
        {task.dueDate ? formatLongDate(task.dueDate, locale) : <Muted>{t("noDueDate")}</Muted>}
      </Row>
      <Row label={t("reminder")}>
        {task.dueDate && task.reminderDaysBefore != null ? (
          t("reminderValue", { days: task.reminderDaysBefore })
        ) : (
          <Muted>{t("noReminder")}</Muted>
        )}
      </Row>
      <Row label={t("recurrence")}>{recurrence}</Row>
      <Row label={t("tags")}>
        {task.tags.length === 0 ? (
          <Muted>{t("noTags")}</Muted>
        ) : (
          <ul className="flex flex-wrap gap-1.5">
            {task.tags.map((tag) => (
              <li key={tag.id}>
                <TagChip>{tag.name}</TagChip>
              </li>
            ))}
          </ul>
        )}
      </Row>
      <Row label={t("links")}>
        {task.links.length === 0 ? (
          <Muted>{t("noLinks")}</Muted>
        ) : (
          <ul className="flex flex-col gap-1">
            {task.links.map((link) => (
              <li key={`${link.type}:${link.id}`}>
                {link.name} <span className="text-small text-ink-muted">· {t(`linkType.${link.type}`)}</span>
              </li>
            ))}
          </ul>
        )}
      </Row>
    </dl>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="contents">
      <dt className="text-small font-semibold text-ink-muted md:pt-px">{label}</dt>
      <dd className="-mt-4 min-w-0 break-words md:mt-0">{children}</dd>
    </div>
  );
}

function Muted({ children }: { children: ReactNode }) {
  return <span className="text-ink-muted">{children}</span>;
}
