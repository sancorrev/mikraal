import { ClipboardCheck, Tags, type LucideIcon } from "lucide-react";
import type { Schemas } from "@/lib/api/client";

export type NavItem = { key: "tasks" | "tags"; href: string; icon: LucideIcon };

export function navItems(groupId: string, capabilities?: Schemas["TaskCapabilities"]): NavItem[] {
  const base = `/groups/${groupId}`;
  const items: NavItem[] = [{ key: "tasks", href: `${base}/tasks`, icon: ClipboardCheck }];
  if (capabilities?.canManageTags) items.push({ key: "tags", href: `${base}/tags`, icon: Tags });
  return items;
}

export function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}
