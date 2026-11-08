"use client";

import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { addDays, todayIn } from "@/lib/dates";
import { api, type Task } from "./client";
import { unwrap } from "./problem";

export type TaskWithEtag = { task: Task; etag: string };

export type TaskFilters = {
  status?: Task["status"][];
  assignedTo?: string;
  tagId?: string;
  linked?: string;
  dueFrom?: string;
  dueTo?: string;
  overdue?: boolean;
};

export const keys = {
  session: ["session"] as const,
  devPeople: ["dev-people"] as const,
  capabilities: (groupId: string) => ["capabilities", groupId] as const,
  directory: (groupId: string) => ["directory", groupId] as const,
  tags: (groupId: string) => ["tags", groupId] as const,
  tasks: (groupId: string) => ["tasks", groupId] as const,
  task: (groupId: string, taskId: string) => ["task", groupId, taskId] as const,
  history: (groupId: string, taskId: string) => ["history", groupId, taskId] as const,
};

/** The ETag is the task version, quoted; prefer the header when the BFF sends it. */
export function withEtag(task: Task, response: Response): TaskWithEtag {
  return { task, etag: response.headers.get("ETag") ?? `"${task.version}"` };
}

export function useSession() {
  return useQuery({
    queryKey: keys.session,
    queryFn: async () => unwrap(await api.GET("/api/session")),
    retry: false,
  });
}

export function useDevPeople() {
  return useQuery({
    queryKey: keys.devPeople,
    queryFn: async () => unwrap(await api.GET("/api/dev/people")).items,
  });
}

export function useCapabilities(groupId: string) {
  return useQuery({
    queryKey: keys.capabilities(groupId),
    queryFn: async () =>
      unwrap(await api.GET("/api/groups/{groupId}/task-capabilities", { params: { path: { groupId } } })),
  });
}

export function useDirectory(groupId: string) {
  return useQuery({
    queryKey: keys.directory(groupId),
    queryFn: async () => unwrap(await api.GET("/api/groups/{groupId}/directory", { params: { path: { groupId } } })),
    staleTime: 5 * 60_000,
  });
}

export function useTags(groupId: string) {
  return useQuery({
    queryKey: keys.tags(groupId),
    queryFn: async () =>
      unwrap(await api.GET("/api/groups/{groupId}/task-tags", { params: { path: { groupId } } })).items,
  });
}

export function useTaskPages(groupId: string, filters: TaskFilters) {
  return useInfiniteQuery({
    queryKey: [...keys.tasks(groupId), filters],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET("/api/groups/{groupId}/tasks", {
          params: { path: { groupId }, query: { ...filters, cursor: pageParam } },
        }),
      ),
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}

/** Kraal-wide counts of open tasks, as of when they were fetched. */
export type TaskSummary = { open: number; dueThisWeek: number; paused: number; overdue: number };

const OPEN_STATUSES: Task["status"][] = ["pending", "in_progress", "paused"];
/** 10 pages of 100: past that, the counts are a lower bound. */
const SUMMARY_MAX_PAGES = 10;

/**
 * Counts for the dashboard and the navigation, whatever the list filters. The API has no
 * counts, so this reads every open task. «This week» is the next seven days, today included,
 * in the group's time zone.
 */
export function useTaskSummary(groupId: string, timeZone: string) {
  return useQuery({
    queryKey: [...keys.tasks(groupId), "summary"],
    queryFn: async (): Promise<TaskSummary> => {
      const open: Task[] = [];
      let cursor: string | undefined;
      for (let page = 0; page < SUMMARY_MAX_PAGES; page++) {
        const result = unwrap(
          await api.GET("/api/groups/{groupId}/tasks", {
            params: { path: { groupId }, query: { status: OPEN_STATUSES, limit: 100, cursor } },
          }),
        );
        open.push(...result.items);
        cursor = result.nextCursor ?? undefined;
        if (!cursor) break;
      }
      const today = todayIn(timeZone);
      const weekEnd = addDays(today, 6);
      return {
        open: open.length,
        dueThisWeek: open.filter((task) => task.dueDate && task.dueDate >= today && task.dueDate <= weekEnd).length,
        paused: open.filter((task) => task.status === "paused").length,
        overdue: open.filter((task) => task.overdue).length,
      };
    },
  });
}

export function useTask(groupId: string, taskId: string) {
  return useQuery({
    queryKey: keys.task(groupId, taskId),
    queryFn: async () => {
      const result = await api.GET("/api/groups/{groupId}/tasks/{taskId}", {
        params: { path: { groupId, taskId } },
      });
      return withEtag(unwrap(result), result.response);
    },
  });
}

export function useTaskHistory(groupId: string, taskId: string) {
  return useInfiniteQuery({
    queryKey: keys.history(groupId, taskId),
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET("/api/groups/{groupId}/tasks/{taskId}/history", {
          params: { path: { groupId, taskId }, query: { cursor: pageParam } },
        }),
      ),
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}
