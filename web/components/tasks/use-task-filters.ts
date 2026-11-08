"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo } from "react";
import type { TaskFilters } from "@/lib/api/queries";
import { isTaskStatus } from "@/lib/task-status";

export type FilterKey = "owner" | "status" | "tag" | "linked" | "from" | "to" | "overdue" | "view";
export type TaskView = "board" | "list";

/** `owner` values: `all` (default), `me`, `unassigned`, `person:<id>`, `position:<id>`. */
export const OWNER_ALL = "all";
export const OWNER_ME = "me";

/** What «Quitar filtros» keeps: who the tasks are for and how they are shown. */
const KEPT_ON_CLEAR: readonly FilterKey[] = ["owner", "view"];

/** Filters and the view live in the URL so a view can be shared and survives a reload. */
export function useTaskFilters() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const owner = params.get("owner") ?? OWNER_ALL;
  const view: TaskView = params.get("view") === "list" ? "list" : "board";
  const statusParam = params.get("status") ?? "";
  const status = useMemo(() => statusParam.split(",").filter(isTaskStatus), [statusParam]);
  const tag = params.get("tag") ?? "";
  const linked = params.get("linked") ?? "";
  const from = params.get("from") ?? "";
  const to = params.get("to") ?? "";
  const overdue = params.get("overdue") === "1";

  const apiFilters = useMemo<TaskFilters>(
    () => ({
      assignedTo: owner === OWNER_ALL ? undefined : owner,
      status: status.length > 0 ? status : undefined,
      tagId: tag || undefined,
      linked: linked || undefined,
      dueFrom: from || undefined,
      dueTo: to || undefined,
      overdue: overdue || undefined,
    }),
    [owner, status, tag, linked, from, to, overdue],
  );

  const replace = useCallback(
    (next: URLSearchParams) => {
      const query = next.toString();
      router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    },
    [pathname, router],
  );

  const setFilter = useCallback(
    (key: FilterKey, value: string) => {
      const next = new URLSearchParams(params);
      if (value) next.set(key, value);
      else next.delete(key);
      replace(next);
    },
    [params, replace],
  );

  const clearRefinements = useCallback(() => {
    const next = new URLSearchParams();
    for (const key of KEPT_ON_CLEAR) {
      const value = params.get(key);
      if (value) next.set(key, value);
    }
    replace(next);
  }, [params, replace]);

  const refined = status.length > 0 || Boolean(tag || linked || from || to || overdue);
  /** Filters that only show inside «Más filtros» (the toolbar shows the rest). */
  const hiddenCount = [owner !== OWNER_ALL && owner !== OWNER_ME, status.length > 0, from, to, overdue].filter(
    Boolean,
  ).length;

  return {
    owner,
    view,
    status,
    tag,
    linked,
    from,
    to,
    overdue,
    refined,
    hiddenCount,
    apiFilters,
    setFilter,
    clearRefinements,
  };
}
