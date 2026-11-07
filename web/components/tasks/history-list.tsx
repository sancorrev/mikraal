"use client";

import { useLocale, useTranslations } from "next-intl";
import type { RecurrencePeriod, TaskHistoryEntry } from "@/lib/api/client";
import { useTaskHistory } from "@/lib/api/queries";
import { formatDateTime, formatLongDate } from "@/lib/dates";
import { isTaskStatus } from "@/lib/task-status";
import type { Messages } from "next-intl";
import { QueryState } from "@/components/query-state";
import { Button } from "@/components/ui/button";

const knownFields: readonly string[] = [
  "title",
  "description",
  "status",
  "pauseReason",
  "owners",
  "dueDate",
  "reminderDaysBefore",
  "tags",
  "links",
  "recurrence",
] satisfies (keyof Messages["history"]["fields"])[];
type KnownField = Exclude<keyof Messages["history"]["fields"], "other">;

const isoDate = /^\d{4}-\d{2}-\d{2}$/;
const recurrencePeriods: readonly string[] = ["weekly", "monthly", "round"] satisfies RecurrencePeriod[];

/** History, newest first, with actor names resolved by the BFF. */
export function HistoryList({ groupId, taskId, timeZone }: { groupId: string; taskId: string; timeZone: string }) {
  const t = useTranslations("history");
  const tCommon = useTranslations("common");
  const history = useTaskHistory(groupId, taskId);
  const entries = history.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <section aria-labelledby="history-heading" className="flex flex-col gap-4">
      <h2 id="history-heading" className="text-h2">
        {t("title")}
      </h2>
      {!history.data ? (
        <QueryState error={history.error} onRetry={() => void history.refetch()} />
      ) : entries.length === 0 ? (
        <p className="text-ink-muted">{t("empty")}</p>
      ) : (
        <ol className="flex flex-col">
          {entries.map((entry) => (
            <HistoryItem key={entry.id} entry={entry} timeZone={timeZone} />
          ))}
        </ol>
      )}
      {history.hasNextPage && (
        <Button
          className="self-start"
          disabled={history.isFetchingNextPage}
          onClick={() => void history.fetchNextPage()}
        >
          {history.isFetchingNextPage ? tCommon("loading") : tCommon("loadMore")}
        </Button>
      )}
    </section>
  );
}

function HistoryItem({ entry, timeZone }: { entry: TaskHistoryEntry; timeZone: string }) {
  const t = useTranslations("history");
  const locale = useLocale();
  const format = useValueFormatter();
  const changes = Object.entries(entry.changes ?? {});

  return (
    <li className="relative border-l border-border-control pb-6 pl-5 last:pb-0">
      <span
        aria-hidden
        className="absolute top-1.5 -left-[5px] size-2.5 rounded-full border-2 border-surface bg-ink-muted"
      />
      <p className="font-semibold">
        {t(`kind.${entry.kind}`, { actor: entry.actor.name })}
        {entry.onBehalfOfPosition && (
          <span className="font-normal text-ink-muted">
            {" "}
            {t("onBehalfOf", { position: entry.onBehalfOfPosition.name })}
          </span>
        )}
      </p>
      <p className="text-small text-ink-muted">
        <time dateTime={entry.at}>{formatDateTime(entry.at, locale, timeZone)}</time>
      </p>
      {changes.length > 0 && (
        <ul className="mt-2 flex flex-col gap-1 text-small">
          {changes.map(([field, change]) => (
            <li key={field} className="break-words">
              {t("change", {
                field: fieldLabel(t, field),
                from: format(field, change.from),
                to: format(field, change.to),
              })}
            </li>
          ))}
        </ul>
      )}
    </li>
  );
}

function fieldLabel(t: ReturnType<typeof useTranslations<"history">>, field: string) {
  return knownFields.includes(field) ? t(`fields.${field as KnownField}`) : t("fields.other");
}

/** History values are free-form JSON: show names, labels and dates, never raw ids. */
function useValueFormatter() {
  const tStatus = useTranslations("status");
  const tRecurrence = useTranslations("recurrence");
  const tCommon = useTranslations("common");
  const locale = useLocale();

  function format(field: string, value: unknown): string {
    if (value === null || value === undefined || value === "") return tCommon("none");
    if (Array.isArray(value)) {
      return value.length ? value.map((item) => format(field, item)).join(", ") : tCommon("none");
    }
    if (typeof value === "object") {
      const record = value as Record<string, unknown>;
      if (typeof record.name === "string") return record.name;
      if (typeof record.period === "string") return format(field, record.period);
      return tCommon("none");
    }
    const text = String(value);
    if (field === "status" && isTaskStatus(text)) return tStatus(text);
    if (field === "recurrence" && recurrencePeriods.includes(text)) return tRecurrence(text as RecurrencePeriod);
    if (isoDate.test(text)) return formatLongDate(text, locale);
    return text;
  }

  return format;
}
