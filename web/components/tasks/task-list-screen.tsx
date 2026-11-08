"use client";

import { Plus } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Tabs } from "radix-ui";
import type { ReactNode } from "react";
import { useCapabilities, useDirectory, useTags, useTaskPages, useTaskSummary } from "@/lib/api/queries";
import { todayIn } from "@/lib/dates";
import { MobileActionBar } from "@/components/mobile-action-bar";
import { PageHeader } from "@/components/page-header";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Button } from "@/components/ui/button";
import { TaskBoard } from "./task-board";
import { TaskStats, UpdatedAt } from "./task-stats";
import { TaskTable } from "./task-table";
import { TaskToolbar } from "./task-toolbar";
import { OWNER_ME, useTaskFilters } from "./use-task-filters";

/** The kraal's tasks: counts, then a board by status or a list, with filters in the URL. */
export function TaskListScreen() {
  const t = useTranslations("list");
  const tShell = useTranslations("shell");
  const { group } = useGroup();
  const filters = useTaskFilters();
  const capabilities = useCapabilities(group.id).data;
  const directory = useDirectory(group.id).data;
  const tags = useTags(group.id).data;
  const summary = useTaskSummary(group.id, group.timeZone);
  const canCreate = capabilities?.canCreateTasks ?? false;

  return (
    <div className="flex flex-col gap-5">
      <PageHeader title={t("title")} description={t("intro")}>
        <UpdatedAt at={summary.dataUpdatedAt} />
      </PageHeader>

      <TaskStats summary={summary.data} loading={summary.isPending} />

      <Tabs.Root
        value={filters.view}
        onValueChange={(view) => filters.setFilter("view", view === "list" ? "list" : "")}
        className="flex flex-col gap-5"
      >
        <TaskToolbar filters={filters} directory={directory} tags={tags} />
        <Tabs.Content value="board" className="focus-visible:outline-offset-4">
          <TaskBoard filters={filters.apiFilters} statuses={filters.status} canCreate={canCreate} />
        </Tabs.Content>
        <Tabs.Content value="list" className="focus-visible:outline-offset-4">
          <TaskList filters={filters} canCreate={canCreate} />
        </Tabs.Content>
      </Tabs.Root>

      {canCreate && (
        <div className="lg:hidden">
          <MobileActionBar>
            <Button asChild variant="primary">
              <Link href={`/groups/${group.id}/tasks/new`}>
                <Plus aria-hidden />
                {tShell("newTask")}
              </Link>
            </Button>
          </MobileActionBar>
          <div aria-hidden className="h-16" />
        </div>
      )}
    </div>
  );
}

function TaskList({ filters, canCreate }: { filters: ReturnType<typeof useTaskFilters>; canCreate: boolean }) {
  const t = useTranslations("list");
  const tCommon = useTranslations("common");
  const { group } = useGroup();
  const pages = useTaskPages(group.id, filters.apiFilters);
  const tasks = pages.data?.pages.flatMap((page) => page.items) ?? [];
  const base = `/groups/${group.id}/tasks`;
  const mine = filters.owner === OWNER_ME;

  return (
    <div aria-busy={pages.isFetching} className="flex flex-col gap-4">
      {!pages.data ? (
        <QueryState error={pages.error} onRetry={() => void pages.refetch()} />
      ) : tasks.length === 0 ? (
        <EmptyState
          message={filters.refined ? t("emptyFiltered") : mine ? t("emptyMine") : t("emptyAll")}
          action={
            filters.refined ? (
              <Button onClick={filters.clearRefinements}>{t("clearFilters")}</Button>
            ) : mine ? (
              <Button onClick={() => filters.setFilter("owner", "")}>{t("emptyMineAction")}</Button>
            ) : canCreate ? (
              <Button asChild variant="primary">
                <Link href={`${base}/new`}>{t("createFirst")}</Link>
              </Button>
            ) : null
          }
        />
      ) : (
        <TaskTable tasks={tasks} base={base} today={todayIn(group.timeZone)} />
      )}
      {pages.hasNextPage && (
        <Button className="self-center" disabled={pages.isFetchingNextPage} onClick={() => void pages.fetchNextPage()}>
          {pages.isFetchingNextPage ? tCommon("loading") : tCommon("loadMore")}
        </Button>
      )}
    </div>
  );
}

function EmptyState({ message, action }: { message: string; action: ReactNode }) {
  return (
    <div className="flex flex-col items-start gap-4 rounded-card border border-border bg-surface-raised p-4 lg:p-5">
      <p>{message}</p>
      {action}
    </div>
  );
}
