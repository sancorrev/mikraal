"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, writeHeader, type Schemas, type TaskStatus } from "./client";
import { keys, withEtag, type TaskWithEtag } from "./queries";
import { unwrap } from "./problem";

export function useDevLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (personId: string) =>
      unwrap(await api.POST("/api/session/dev-login", { params: { header: writeHeader }, body: { personId } })),
    onSuccess: (session) => {
      queryClient.clear();
      queryClient.setQueryData(keys.session, session);
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => unwrap(await api.DELETE("/api/session", { params: { header: writeHeader } })),
    onSettled: () => queryClient.clear(),
  });
}

/** Keeps the cached task and lists in step after a write that returns the task. */
function useTaskWritten(groupId: string) {
  const queryClient = useQueryClient();
  return (value: TaskWithEtag) => {
    queryClient.setQueryData(keys.task(groupId, value.task.id), value);
    void queryClient.invalidateQueries({ queryKey: keys.tasks(groupId) });
    void queryClient.invalidateQueries({ queryKey: keys.history(groupId, value.task.id) });
  };
}

export function useCreateTask(groupId: string) {
  const written = useTaskWritten(groupId);
  return useMutation({
    mutationFn: async (body: Schemas["CreateTask"]) => {
      const result = await api.POST("/api/groups/{groupId}/tasks", {
        params: { path: { groupId }, header: writeHeader },
        body,
      });
      return withEtag(unwrap(result), result.response);
    },
    onSuccess: written,
  });
}

export function useUpdateTask(groupId: string, taskId: string) {
  const written = useTaskWritten(groupId);
  return useMutation({
    mutationFn: async ({ etag, body }: { etag: string; body: Schemas["UpdateTask"] }) => {
      const result = await api.PATCH("/api/groups/{groupId}/tasks/{taskId}", {
        params: { path: { groupId, taskId }, header: { ...writeHeader, "If-Match": etag } },
        body,
        headers: { "Content-Type": "application/merge-patch+json" },
      });
      return withEtag(unwrap(result), result.response);
    },
    onSuccess: written,
  });
}

export function useChangeStatus(groupId: string, taskId: string) {
  const written = useTaskWritten(groupId);
  return useMutation({
    mutationFn: async ({ etag, status, pauseReason }: { etag: string; status: TaskStatus; pauseReason?: string }) => {
      const result = await api.POST("/api/groups/{groupId}/tasks/{taskId}/status-changes", {
        params: { path: { groupId, taskId }, header: { ...writeHeader, "If-Match": etag } },
        body: { status, pauseReason },
      });
      return withEtag(unwrap(result), result.response);
    },
    onSuccess: written,
  });
}

export function useDeleteTask(groupId: string, taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (etag: string) =>
      unwrap(
        await api.DELETE("/api/groups/{groupId}/tasks/{taskId}", {
          params: { path: { groupId, taskId }, header: { ...writeHeader, "If-Match": etag } },
        }),
      ),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: keys.task(groupId, taskId) });
      void queryClient.invalidateQueries({ queryKey: keys.tasks(groupId) });
    },
  });
}

export function useEndRecurrence(groupId: string, taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(
        await api.POST("/api/groups/{groupId}/tasks/{taskId}/recurrence/end", {
          params: { path: { groupId, taskId }, header: writeHeader },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: keys.task(groupId, taskId) });
      void queryClient.invalidateQueries({ queryKey: keys.history(groupId, taskId) });
    },
  });
}

function useTagsChanged(groupId: string) {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: keys.tags(groupId) });
    void queryClient.invalidateQueries({ queryKey: keys.tasks(groupId) });
    void queryClient.invalidateQueries({ queryKey: ["task", groupId] });
  };
}

export function useCreateTag(groupId: string) {
  const onSuccess = useTagsChanged(groupId);
  return useMutation({
    mutationFn: async (name: string) =>
      unwrap(
        await api.POST("/api/groups/{groupId}/task-tags", {
          params: { path: { groupId }, header: writeHeader },
          body: { name },
        }),
      ),
    onSuccess,
  });
}

export function useRenameTag(groupId: string) {
  const onSuccess = useTagsChanged(groupId);
  return useMutation({
    mutationFn: async ({ tagId, name }: { tagId: string; name: string }) =>
      unwrap(
        await api.PATCH("/api/groups/{groupId}/task-tags/{tagId}", {
          params: { path: { groupId, tagId }, header: writeHeader },
          body: { name },
        }),
      ),
    onSuccess,
  });
}

export function useDeleteTag(groupId: string) {
  const onSuccess = useTagsChanged(groupId);
  return useMutation({
    mutationFn: async (tagId: string) =>
      unwrap(
        await api.DELETE("/api/groups/{groupId}/task-tags/{tagId}", {
          params: { path: { groupId, tagId }, header: writeHeader },
        }),
      ),
    onSuccess,
  });
}
