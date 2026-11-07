"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, type ReactNode } from "react";
import { useSession } from "@/lib/api/queries";
import { QueryState } from "@/components/query-state";
import { SimplePage } from "@/components/simple-page";
import { AppShell } from "./app-shell";
import { GroupContext } from "./group-context";

export function GroupShell({ groupId, children }: { groupId: string; children: ReactNode }) {
  const router = useRouter();
  const session = useSession();
  const group = session.data?.groups.find((candidate) => candidate.id === groupId);
  const missing = session.data !== undefined && !group;

  useEffect(() => {
    if (missing) router.replace("/?group=not-found");
  }, [missing, router]);

  const value = useMemo(() => (session.data && group ? { session: session.data, group } : null), [session.data, group]);

  if (!value) {
    return (
      <SimplePage>
        <QueryState error={session.error} onRetry={() => void session.refetch()} />
      </SimplePage>
    );
  }

  return (
    <GroupContext.Provider value={value}>
      <AppShell>{children}</AppShell>
    </GroupContext.Provider>
  );
}
