import { statusOf } from "@/lib/api/problem";

export type TagNameErrorKey = "nameRequired" | "duplicate" | "tooLong" | "forbidden";

export const TAG_NAME_MAX = 40;

export function validateTagName(name: string): TagNameErrorKey | undefined {
  if (!name.trim()) return "nameRequired";
  if (name.trim().length > TAG_NAME_MAX) return "tooLong";
  return undefined;
}

export function tagWriteError(error: unknown): TagNameErrorKey | "generic" | undefined {
  if (!error) return undefined;
  switch (statusOf(error)) {
    case 409:
      return "duplicate";
    case 403:
      return "forbidden";
    case 400:
      return "nameRequired";
    default:
      return "generic";
  }
}
