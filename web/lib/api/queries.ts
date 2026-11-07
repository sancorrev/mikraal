"use client";

import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
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
