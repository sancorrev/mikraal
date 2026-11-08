"use client";

import { ChevronsUpDown, Plus } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { useCapabilities, useTaskSummary } from "@/lib/api/queries";
import { cn } from "@/lib/utils";
import { AppIcon } from "@/components/app-icon";
import { Button } from "@/components/ui/button";
import { Wordmark } from "@/components/wordmark";
import { AccountDropdown } from "./account-dropdown";
import { AccountMenu } from "./account-menu";
import { useGroup } from "./group-context";
import { isActive, navItems, type NavItem } from "./nav-items";

const hover = "hover:bg-[color-mix(in_srgb,var(--ink)_5%,transparent)]";

/**
 * Desktop: grey frame with the side navigation (248 px), a top bar with the path and the main
 * action, and the page in a panel. Mobile: top bar and bottom navigation.
 */
export function AppShell({ children }: { children: ReactNode }) {
  const t = useTranslations("shell");
  const tApp = useTranslations("app");
  const pathname = usePathname();
  const { group } = useGroup();
  const capabilities = useCapabilities(group.id).data;
  const openTasks = useTaskSummary(group.id, group.timeZone).data?.open;
  const items = navItems(group.id, capabilities);
  const current = items.find((item) => isActive(pathname, item.href));
  const newTaskHref = `/groups/${group.id}/tasks/new`;

  return (
    <div className="min-h-dvh lg:bg-canvas">
      <a
        href="#main"
        className="fixed top-2 left-2 z-50 -translate-y-20 rounded-control bg-ink px-4 py-3 text-surface focus:translate-y-0"
      >
        {tApp("skipToContent")}
      </a>

      <aside className="fixed inset-y-0 left-0 hidden w-[248px] flex-col gap-4 overflow-y-auto px-3 py-3.5 lg:flex">
        <GroupBlock />
        <nav aria-label={t("mainNav")} className="flex flex-col gap-0.5">
          <p id="nav-section" className="flex h-7 items-center px-2.5 text-small font-medium text-ink-muted">
            {t("section")}
          </p>
          <ul aria-labelledby="nav-section" className="flex flex-col gap-0.5">
            {items.map((item) => (
              <li key={item.key}>
                <SideLink
                  item={item}
                  active={isActive(pathname, item.href)}
                  count={item.key === "tasks" ? openTasks : undefined}
                />
              </li>
            ))}
          </ul>
        </nav>
        <div className="mt-auto border-t border-border pt-3">
          <AccountDropdown />
        </div>
      </aside>

      <div className="lg:pl-[248px]">
        <div className="flex min-h-dvh flex-col lg:border-l lg:border-border">
          <header className="hidden min-h-16 items-center gap-3 py-3 pr-4 pl-5 lg:flex">
            <Breadcrumb item={current} isPage={current?.href === pathname} />
            {capabilities?.canCreateTasks && pathname !== newTaskHref && (
              <Button asChild variant="strong">
                <Link href={newTaskHref}>
                  <Plus aria-hidden />
                  {t("newTask")}
                </Link>
              </Button>
            )}
          </header>

          <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-border bg-surface px-4 md:px-6 lg:hidden">
            <Wordmark className="text-h3" />
            <AccountMenu />
          </header>

          <main
            id="main"
            tabIndex={-1}
            className="flex-1 pb-24 focus:outline-none lg:mr-3 lg:mb-3 lg:rounded-card lg:border lg:border-border lg:bg-panel lg:pb-0"
          >
            <div className="mx-auto max-w-[1120px] px-4 py-6 md:px-6 lg:px-7 lg:pt-7 lg:pb-9">{children}</div>
          </main>
        </div>
      </div>

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

/** The group being worked on; a link to change it when the person is a scouter in several. */
function GroupBlock() {
  const t = useTranslations("shell");
  const { group, session } = useGroup();
  const several = session.groups.length > 1;
  const body = (
    <>
      <AppIcon className="size-10" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-small font-semibold break-words">{group.name}</span>
        {several && <span className="text-caption text-ink-muted">{t("changeGroup")}</span>}
      </span>
      {several && <ChevronsUpDown aria-hidden className="size-4 text-ink-muted" />}
    </>
  );
  return several ? (
    <Link
      href="/"
      className={cn(
        "flex items-center gap-2.5 rounded-control p-1 no-underline transition-colors duration-[120ms]",
        hover,
      )}
    >
      {body}
    </Link>
  ) : (
    <div className="flex items-center gap-2.5 p-1">{body}</div>
  );
}

function SideLink({ item, active, count }: { item: NavItem; active: boolean; count?: number }) {
  const t = useTranslations("shell");
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex h-9 items-center gap-2.5 rounded-control px-2.5 font-medium no-underline transition-colors duration-[120ms]",
        active ? "bg-surface-raised font-semibold shadow-xs ring-1 ring-border" : hover,
      )}
    >
      <Icon aria-hidden className="size-5" />
      <span className="flex-1">{t(item.key)}</span>
      {count !== undefined && (
        <>
          <span aria-hidden className="text-caption font-medium text-ink-muted tabular-nums">
            {count}
          </span>
          <span className="sr-only">{t("openCount", { count })}</span>
        </>
      )}
    </Link>
  );
}

/** «Kraal / Tareas»: the module links back to its first page from deeper pages. */
function Breadcrumb({ item, isPage }: { item?: NavItem; isPage: boolean }) {
  const t = useTranslations("shell");
  return (
    <nav aria-label={t("breadcrumb")} className="min-w-0 flex-1">
      <ol className="flex flex-wrap items-center gap-2 text-ink-muted">
        <li>{t("section")}</li>
        {item && (
          <li className="flex items-center gap-2">
            <span aria-hidden>/</span>
            {isPage ? (
              <span aria-current="page" className="text-ink">
                {t(item.key)}
              </span>
            ) : (
              <Link href={item.href} className="text-ink no-underline hover:underline">
                {t(item.key)}
              </Link>
            )}
          </li>
        )}
      </ol>
    </nav>
  );
}
