"use client";

import { Plus } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { useCapabilities } from "@/lib/api/queries";
import { cn } from "@/lib/utils";
import { LogoutButton } from "@/components/session/logout-button";
import { Button } from "@/components/ui/button";
import { Wordmark } from "@/components/wordmark";
import { AccountMenu } from "./account-menu";
import { useGroup } from "./group-context";
import { isActive, navItems } from "./nav-items";

/** Side navigation (248 px) on desktop; top bar and bottom navigation on mobile. */
export function AppShell({ children }: { children: ReactNode }) {
  const t = useTranslations("shell");
  const tApp = useTranslations("app");
  const pathname = usePathname();
  const { group, session } = useGroup();
  const capabilities = useCapabilities(group.id).data;
  const items = navItems(group.id, capabilities);
  const several = session.groups.length > 1;

  return (
    <div className="min-h-dvh">
      <a
        href="#main"
        className="fixed top-2 left-2 z-50 -translate-y-20 rounded-control bg-ink px-4 py-3 text-surface focus:translate-y-0"
      >
        {tApp("skipToContent")}
      </a>

      <aside className="fixed inset-y-0 left-0 hidden w-[248px] flex-col border-r border-border bg-surface lg:flex">
        <div className="flex h-16 items-center px-6">
          <Wordmark />
        </div>
        <div className="px-6 pb-4">
          <p className="text-overline font-semibold text-ink-muted uppercase">{t("group")}</p>
          <p className="mt-1 text-small font-semibold">{group.name}</p>
          {several && (
            <Link href="/" className="text-small underline">
              {t("changeGroup")}
            </Link>
          )}
        </div>
        <nav aria-label={t("mainNav")} className="flex flex-col gap-1 px-3">
          {items.map(({ key, href, icon: Icon }) => {
            const active = isActive(pathname, href);
            return (
              <Link
                key={key}
                href={href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex h-10 items-center gap-3 rounded-control px-3 font-medium no-underline transition-colors duration-[120ms]",
                  active ? "bg-violet-light" : "hover:bg-surface-muted",
                )}
              >
                <Icon aria-hidden className={cn("size-5", active ? "text-violet" : "text-ink")} />
                {t(key)}
              </Link>
            );
          })}
        </nav>
        {capabilities?.canCreateTasks && (
          <div className="px-3 pt-4">
            <Button asChild variant="primary" className="w-full">
              <Link href={`/groups/${group.id}/tasks/new`}>
                <Plus aria-hidden />
                {t("createTask")}
              </Link>
            </Button>
          </div>
        )}
        <div className="mt-auto border-t border-border px-3 py-4">
          <p className="px-3 text-caption text-ink-muted">{t("signedInAs")}</p>
          <p className="px-3 text-small font-semibold">{session.person.name}</p>
          <LogoutButton className="mt-2 w-full justify-start px-3" />
        </div>
      </aside>

      <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-border bg-surface px-4 md:px-6 lg:hidden">
        <Wordmark className="text-h3" />
        <AccountMenu />
      </header>

      <main id="main" tabIndex={-1} className="pb-24 focus:outline-none lg:pb-0 lg:pl-[248px]">
        <div className="mx-auto max-w-[1120px] px-4 py-6 md:px-6 lg:py-8 xl:px-8">{children}</div>
      </main>

      <nav
        aria-label={t("mainNav")}
        className="fixed inset-x-0 bottom-0 z-30 flex border-t border-border bg-surface pb-[env(safe-area-inset-bottom)] lg:hidden"
      >
        {items.map(({ key, href, icon: Icon }) => {
          const active = isActive(pathname, href);
          return (
            <Link
              key={key}
              href={href}
              aria-current={active ? "page" : undefined}
              className="flex h-16 flex-1 flex-col items-center justify-center gap-1 text-caption font-medium no-underline focus-visible:-outline-offset-4"
            >
              <span
                className={cn(
                  "grid h-8 w-14 place-items-center rounded-full transition-colors duration-[120ms]",
                  active && "bg-violet-light",
                )}
              >
                <Icon aria-hidden className={cn("size-5", active ? "text-violet" : "text-ink")} />
              </span>
              {t(key)}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
