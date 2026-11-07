import type { ReactNode } from "react";
import { GroupShell } from "@/components/shell/group-shell";

export default async function GroupLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ groupId: string }>;
}) {
  const { groupId } = await params;
  return <GroupShell groupId={groupId}>{children}</GroupShell>;
}
