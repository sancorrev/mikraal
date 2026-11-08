"use client";

import { Pause, Plus, Repeat } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMemo, type CSSProperties } from "react";
import type { Task, TaskStatus } from "@/lib/api/client";
import { useTaskPages, type TaskFilters } from "@/lib/api/queries";
import { todayIn } from "@/lib/dates";
import { taskStatuses } from "@/lib/task-status";
import { cn } from "@/lib/utils";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Button, buttonVariants } from "@/components/ui/button";
import { DueChip, OwnerAvatars, srOnly } from "./task-meta";

const columnStyles: Record<TaskStatus, { pill: string; dot: string }> = {
  pending: { pill: "bg-[color-mix(in_srgb,var(--ink)_7%,transparent)]", dot: "bg-ink-muted" },
  in_progress: { pill: "bg-surface-raised ring-1 ring-border", dot: "bg-ink" },
  paused: { pill: "bg-violet-soft", dot: "bg-violet" },
  finished: { pill: "bg-violet-light", dot: "bg-violet" },
};

type BoardProps = {
  filters: TaskFilters;
  /** Statuses picked in the filters; none means every column. */
  statuses: TaskStatus[];
  canCreate: boolean;
};

/** One column per status, each with its own page of tasks (the API lists by deadline). */
export function TaskBoard({ filters, statuses, canCreate }: BoardProps) {
  const { group } = useGroup();
  const today = todayIn(group.timeZone);
  const columns = statuses.length > 0 ? taskStatuses.filter((status) => statuses.includes(status)) : taskStatuses;
  const layout = {
    "--board-columns": `repeat(${columns.length}, minmax(0, 1fr))`,
    "--board-columns-md": `repeat(${Math.min(columns.length, 2)}, minmax(0, 1fr))`,
  } as CSSProperties;

  return (
    <div
      style={layout}
      className="grid items-start gap-3.5 md:grid-cols-(--board-columns-md) xl:grid-cols-(--board-columns)"
    >
      {columns.map((status) => (
        <BoardColumn
          key={status}
          status={status}
          filters={filters}
          today={today}
          canCreate={canCreate && status === "pending"}
        />
      ))}
    </div>
  );
}

function BoardColumn({
  status,
  filters,
  today,
  canCreate,
}: {
  status: TaskStatus;
  filters: TaskFilters;
  today: string;
  canCreate: boolean;
}) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  const tCommon = useTranslations("common");
  const { group } = useGroup();
  const columnFilters = useMemo(() => ({ ...filters, status: [status] }), [filters, status]);
  const pages = useTaskPages(group.id, columnFilters);
  const tasks = pages.data?.pages.flatMap((page) => page.items) ?? [];
  const base = `/groups/${group.id}/tasks`;
  const headingId = `board-column-${status}`;
  const style = columnStyles[status];

  return (
    <section
      aria-labelledby={headingId}
      aria-busy={pages.isFetching}
      className="flex min-w-0 flex-col gap-2.5 rounded-card bg-well p-2"
    >
      <h2
        id={headingId}
        className={cn(
          "inline-flex h-[30px] items-center gap-2 self-start rounded-full pr-1.5 pl-2.5 font-sans text-small font-semibold",
          style.pill,
        )}
      >
        <span aria-hidden className={cn("size-2 rounded-full", style.dot)} />
        {tStatus(status)}
        {pages.data && (
          <>
            <span aria-hidden className="rounded-full bg-surface-raised px-[7px] py-px text-caption tabular-nums">
              {tasks.length}
              {pages.hasNextPage && "+"}
            </span>
            <span className="sr-only">
              {", "}
              {t(pages.hasNextPage ? "columnCountMore" : "columnCount", { count: tasks.length })}
            </span>
          </>
        )}
      </h2>

      {!pages.data ? (
        <QueryState error={pages.error} onRetry={() => void pages.refetch()} />
      ) : tasks.length === 0 ? (
        <p className="px-2 pb-1 text-small text-ink-muted">{t("emptyColumn")}</p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {tasks.map((task) => (
            <li key={task.id}>
              <TaskCard task={task} href={`${base}/${task.id}`} today={today} />
            </li>
          ))}
        </ul>
      )}

      {pages.hasNextPage && (
        <Button
          size="dense"
          variant="ghost"
          disabled={pages.isFetchingNextPage}
          onClick={() => void pages.fetchNextPage()}
        >
          {pages.isFetchingNextPage ? tCommon("loading") : tCommon("loadMore")}
        </Button>
      )}

      {canCreate && (
        <Link
          href={`${base}/new`}
          className={cn(buttonVariants({ variant: "ghost", size: "dense" }), "justify-start text-ink-muted")}
        >
          <Plus aria-hidden />
          {t("addTask")}
        </Link>
      )}
    </section>
  );
}

const hoverBorder = "hover:border-[color-mix(in_srgb,var(--ink)_22%,var(--border))]";

function TaskCard({ task, href, today }: { task: Task; href: string; today: string }) {
  const t = useTranslations("list");
  const tags = task.tags.map((tag) => tag.name).join(" · ");
  const links = task.links.map((link) => link.name).join(" · ");

  return (
    <article
      className={cn(
        "relative flex flex-col gap-2.5 rounded-xl border border-border bg-surface-raised p-3.5 transition-colors duration-[120ms]",
        hoverBorder,
      )}
    >
      {tags && (
        <p className="text-caption font-medium text-ink-muted">{t.rich("tags", { names: tags, hidden: srOnly })}</p>
      )}
      <div className="flex flex-col gap-0.5">
        <Link
          href={href}
          className="font-semibold break-words no-underline after:absolute after:inset-0 after:rounded-xl hover:underline focus-visible:outline-none focus-visible:after:outline-2 focus-visible:after:outline-offset-2 focus-visible:after:outline-violet"
        >
          {task.title}
        </Link>
        {links && <p className="text-small text-ink-muted">{t.rich("links", { names: links, hidden: srOnly })}</p>}
      </div>
      {task.status === "paused" && task.pauseReason && (
        <p className="flex items-start gap-1.5 text-small text-ink-muted">
          <Pause aria-hidden className="mt-0.5 size-3.5" />
          <span className="line-clamp-2">{t.rich("pauseReason", { reason: task.pauseReason, hidden: srOnly })}</span>
        </p>
      )}
      <DueChip task={task} today={today} />
      <div className="flex min-h-7 items-center gap-2.5 border-t border-border pt-2.5">
        <div className="min-w-0 flex-1">
          <OwnerAvatars task={task} />
        </div>
        {task.recurrence?.active && (
          <span className="inline-flex items-center gap-1 text-caption font-medium text-ink-muted">
            <Repeat aria-hidden className="size-4" />
            {t("repeats")}
          </span>
        )}
      </div>
    </article>
  );
}
