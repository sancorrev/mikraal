"use client";

import { createContext, useContext } from "react";
import type { Group, Session } from "@/lib/api/client";

type GroupContextValue = { group: Group; session: Session };

export const GroupContext = createContext<GroupContextValue | null>(null);

/** The current group and session; only inside GroupShell. */
export function useGroup(): GroupContextValue {
  const value = useContext(GroupContext);
  if (!value) throw new Error("useGroup must be used inside GroupShell");
  return value;
}
