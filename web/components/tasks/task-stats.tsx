"use client";

import { CalendarX2, ClipboardList, Clock, Pause, type LucideIcon } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import type { TaskSummary } from "@/lib/api/queries";
import { formatInstantDate, formatTime, todayIn } from "@/lib/dates";
import { cn } from "@/lib/utils";
import { useGroup } from "@/components/shell/group-context";

const stats: { key: keyof TaskSummary; icon: LucideIcon; tile: string }[] = [
  { key: "open", icon: ClipboardList, tile: "bg-ink text-surface" },
  { key: "dueThisWeek", icon: Clock, tile: "bg-violet text-on-violet" },
  { key: "paused", icon: Pause, tile: "bg-ink text-surface" },
  { key: "overdue", icon: CalendarX2, tile: "bg-orange-light text-orange" },
];

/** Kraal-wide counts of open tasks: they do not follow the list filters. */
export function TaskStats({ summary, loading }: { summary?: TaskSummary; loading: boolean }) {
  const t = useTranslations("list");
  return (
    <section aria-label={t("summary")} aria-busy={loading}>
      <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-3.5">
        {stats.map(({ key, icon: Icon, tile }) => (
          <li
            key={key}
            className="flex min-w-0 items-center gap-3.5 rounded-card border border-border bg-surface-raised p-4"
          >
            <span aria-hidden className={cn("grid size-10 shrink-0 place-items-center rounded-control", tile)}>
              <Icon className="size-5" />
            </span>
            <p className="flex min-w-0 flex-col">
              <span className="font-heading text-h2 font-semibold tabular-nums">{summary ? summary[key] : "–"}</span>{" "}
              <span className="text-small text-ink-muted">{t(`stats.${key}`)}</span>
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** «Actualizado hoy a las 12:40»: when the counts were read, in the group's time zone. */
export function UpdatedAt({ at }: { at: number }) {
  const t = useTranslations("list");
  const locale = useLocale();
  const { group } = useGroup();
  if (!at) return null;
  const instant = new Date(at);
  const time = formatTime(instant, locale, group.timeZone);
  const today = todayIn(group.timeZone) === todayIn(group.timeZone, instant);
  return (
    <p className="text-small text-ink-muted">
      {today
        ? t("updatedToday", { time })
        : t("updatedOn", { date: formatInstantDate(instant.toISOString(), locale, group.timeZone), time })}
    </p>
  );
}
