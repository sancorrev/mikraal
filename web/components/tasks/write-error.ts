import { statusOf } from "@/lib/api/problem";

export type WriteErrorKind = "conflict" | "transition" | "forbidden" | "generic";

/** 412: someone changed the task; 409: the status no longer allows it. */
export function writeErrorKind(error: unknown): WriteErrorKind | undefined {
  if (!error) return undefined;
  switch (statusOf(error)) {
    case 412:
      return "conflict";
    case 409:
      return "transition";
    case 403:
      return "forbidden";
    default:
      return "generic";
  }
}
