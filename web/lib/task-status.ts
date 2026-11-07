import type { TaskStatus } from "@/lib/api/client";

export const taskStatuses: readonly TaskStatus[] = ["pending", "in_progress", "paused", "finished"];

/** Allowed transitions from the spec (kraal-tasks.md, Statuses). */
export const transitions: Record<TaskStatus, readonly TaskStatus[]> = {
  pending: ["in_progress", "paused", "finished"],
  in_progress: ["pending", "paused", "finished"],
  paused: ["in_progress", "pending", "finished"],
  finished: ["in_progress"],
};

export function isTaskStatus(value: string): value is TaskStatus {
  return (taskStatuses as readonly string[]).includes(value);
}
