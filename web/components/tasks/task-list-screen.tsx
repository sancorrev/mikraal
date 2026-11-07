"use client";

import { Plus } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { useCapabilities, useDirectory, useTags, useTaskPages } from "@/lib/api/queries";
import { MobileActionBar } from "@/components/mobile-action-bar";
import { PageHeader } from "@/components/page-header";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Button } from "@/components/ui/button";
import { TaskFilters } from "./task-filters";
import { TaskRow } from "./task-row";
import { OWNER_ALL, OWNER_ME, useTaskFilters } from "./use-task-filters";

export function TaskListScreen() {
  const t = useTranslations("list");
  const tCommon = useTranslations("common");
  const { group } = useGroup();
  const filters = useTaskFilters();
  const capabilities = useCapabilities(group.id).data;
  const directory = useDirectory(group.id).data;
  const tags = useTags(group.id).data;
  const pages = useTaskPages(group.id, filters.apiFilters);
  const tasks = pages.data?.pages.flatMap((page) => page.items) ?? [];
  const base = `/groups/${group.id}/tasks`;
  const mine = filters.owner === OWNER_ME;
  const canCreate = capabilities?.canCreateTasks ?? false;

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={mine ? t("titleMine") : t("titleAll")} />

      <TaskFilters filters={filters} directory={directory} tags={tags} />

      <section aria-labelledby="results-heading" aria-busy={pages.isFetching} className="flex flex-col gap-4">
        <h2 id="results-heading" className="sr-only">
          {mine ? t("titleMine") : t("titleAll")}
        </h2>
        {!pages.data ? (
          <QueryState error={pages.error} onRetry={() => void pages.refetch()} />
        ) : tasks.length === 0 ? (
          <EmptyState
            message={filters.refined ? t("emptyFiltered") : mine ? t("emptyMine") : t("emptyAll")}
            action={
              filters.refined ? (
                <Button onClick={filters.clearRefinements}>{t("clearFilters")}</Button>
              ) : mine ? (
                <Button onClick={() => filters.setFilter("owner", OWNER_ALL)}>{t("emptyMineAction")}</Button>
              ) : canCreate ? (
                <Button asChild variant="primary">
                  <Link href={`${base}/new`}>{t("createFirst")}</Link>
                </Button>
              ) : null
            }
          />
        ) : (
          <ul className="divide-y divide-border overflow-hidden rounded-card border border-border bg-surface-raised">
            {tasks.map((task) => (
              <TaskRow key={task.id} task={task} href={`${base}/${task.id}`} />
            ))}
          </ul>
        )}
        {pages.hasNextPage && (
          <Button
            className="self-center"
            disabled={pages.isFetchingNextPage}
            onClick={() => void pages.fetchNextPage()}
          >
            {pages.isFetchingNextPage ? tCommon("loading") : tCommon("loadMore")}
          </Button>
        )}
      </section>

      {canCreate && (
        <div className="lg:hidden">
          <MobileActionBar>
            <Button asChild variant="primary">
              <Link href={`${base}/new`}>
                <Plus aria-hidden />
                {t("createFirst")}
              </Link>
            </Button>
          </MobileActionBar>
          <div aria-hidden className="h-16" />
        </div>
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
