"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo } from "react";
import type { TaskFilters } from "@/lib/api/queries";
import { isTaskStatus } from "@/lib/task-status";

export type FilterKey = "owner" | "status" | "tag" | "linked" | "from" | "to" | "overdue";

/** `owner` values: `me` (default), `all`, `unassigned`, `person:<id>`, `position:<id>`. */
export const OWNER_ALL = "all";
export const OWNER_ME = "me";

/** Filters live in the URL so a view can be shared and survives a reload. */
export function useTaskFilters() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const owner = params.get("owner") ?? OWNER_ME;
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

  const setFilter = useCallback(
    (key: FilterKey, value: string) => {
      const next = new URLSearchParams(params);
      if (value) next.set(key, value);
      else next.delete(key);
      const query = next.toString();
      router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    },
    [params, pathname, router],
  );

  const clearRefinements = useCallback(() => {
    const next = new URLSearchParams();
    if (params.has("owner")) next.set("owner", owner);
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [owner, params, pathname, router]);

  const refined = status.length > 0 || Boolean(tag || linked || from || to || overdue);

  return { owner, status, tag, linked, from, to, overdue, refined, apiFilters, setFilter, clearRefinements };
}
