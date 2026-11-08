"use client";

import { ArrowLeftRight, ChevronsUpDown, LogOut } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useSignOut } from "@/components/session/logout-button";
import { Avatar } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useGroup } from "./group-context";

/** Desktop: who is signed in, at the foot of the side navigation. Opens group change and logout. */
export function AccountDropdown() {
  const t = useTranslations("shell");
  const { session } = useGroup();
  const { pending, signOut } = useSignOut();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex w-full items-center gap-2.5 rounded-control bg-surface-raised p-2.5 text-left ring-1 ring-border transition-colors duration-[120ms] hover:bg-surface-muted data-[state=open]:bg-surface-muted">
        <Avatar name={session.person.name} />
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="sr-only">{t("account")}: </span>
          <span className="truncate text-small font-semibold">{session.person.name}</span>
          <span className="text-caption text-ink-muted">{t("role")}</span>
        </span>
        <ChevronsUpDown aria-hidden className="size-4 text-ink-muted" />
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start" className="w-(--radix-dropdown-menu-trigger-width)">
        {session.groups.length > 1 && (
          <DropdownMenuItem asChild>
            <Link href="/">
              <ArrowLeftRight aria-hidden />
              {t("changeGroup")}
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuItem disabled={pending} onSelect={signOut}>
          <LogOut aria-hidden />
          {t("logout")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
