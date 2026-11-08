"use client";

import { CalendarDays } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import type { ReactNode } from "react";
import type { Task } from "@/lib/api/client";
import { addDays, formatShortDate } from "@/lib/dates";
import { avatarTone } from "@/lib/people";
import { cn } from "@/lib/utils";
import { Avatar } from "@/components/ui/avatar";
import { StateChip } from "@/components/ui/chip";

/** For `t.rich`: words only screen readers need («Etiquetas: »). */
export const srOnly = (chunks: ReactNode) => <span className="sr-only">{chunks}</span>;

type DueTone = "done" | "overdue" | "soon" | "plain";

function dueTone(task: Pick<Task, "status" | "overdue" | "dueDate">, today: string): DueTone {
  if (task.status === "finished") return "done";
  if (task.overdue) return "overdue";
  if (task.dueDate && task.dueDate >= today && task.dueDate <= addDays(today, 6)) return "soon";
  return "plain";
}

const dueTones: Record<DueTone, string> = {
  done: "bg-violet-light",
  overdue: "bg-orange-light font-semibold",
  soon: "bg-violet-soft font-semibold",
  plain: "bg-surface-muted",
};

/**
 * The deadline as a chip: violet when it falls in the next seven days, orange with the word
 * «Vencida» once it has passed, light violet when the task is finished.
 */
export function DueChip({ task, today }: { task: Pick<Task, "status" | "overdue" | "dueDate">; today: string }) {
  const t = useTranslations("list");
  const locale = useLocale();
  if (!task.dueDate) {
    return (
      <span className="inline-flex h-6 items-center gap-1.5 self-start rounded-chip bg-surface-muted px-2 text-caption font-medium whitespace-nowrap text-ink-muted">
        <CalendarDays aria-hidden className="size-4" />
        {t("noDue")}
      </span>
    );
  }
  const tone = dueTone(task, today);
  const date = formatShortDate(task.dueDate, locale);
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center gap-1.5 self-start rounded-chip px-2 text-caption font-medium whitespace-nowrap text-ink",
        dueTones[tone],
      )}
    >
      <CalendarDays aria-hidden className={cn("size-4", tone === "overdue" && "text-orange")} />
      {t(tone === "overdue" ? "dueOverdue" : "due", { date })}
    </span>
  );
}

/** Owners as overlapping initials (people round, positions square); the names are read out. */
export function OwnerAvatars({ task, max = 3 }: { task: Pick<Task, "owners" | "unassigned">; max?: number }) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  if (task.unassigned || task.owners.length === 0) {
    return <StateChip tone="neutral">{tStatus("unassigned")}</StateChip>;
  }
  const shown = task.owners.slice(0, max);
  const rest = task.owners.length - shown.length;
  return (
    <span className="inline-flex items-center">
      <span className="sr-only">{t("owners", { names: task.owners.map((owner) => owner.name).join(", ") })}</span>
      {shown.map((owner, index) => (
        <Avatar
          key={`${owner.type}:${owner.id}`}
          name={owner.name}
          size="sm"
          tone={avatarTone(owner.id)}
          shape={owner.type === "position" ? "square" : "round"}
          className={cn("ring-2 ring-surface-raised", index > 0 && "-ml-1.5")}
        />
      ))}
      {rest > 0 && (
        <span
          aria-hidden
          className="-ml-1.5 inline-grid size-[30px] place-items-center rounded-full border border-border bg-surface-raised text-[10px] leading-none font-semibold ring-2 ring-surface-raised"
        >
          +{rest}
        </span>
      )}
    </span>
  );
}
