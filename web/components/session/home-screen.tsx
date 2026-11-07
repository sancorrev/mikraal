"use client";

import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { useSession } from "@/lib/api/queries";
import { QueryState } from "@/components/query-state";
import { SimplePage } from "@/components/simple-page";
import { Notice } from "@/components/ui/notice";
import { LogoutButton } from "./logout-button";

/** Sends scouters to their group's tasks; families get a clear notice. */
export function HomeScreen() {
  const t = useTranslations("home");
  const router = useRouter();
  const notFound = useSearchParams().get("group") === "not-found";
  const session = useSession();
  const groups = session.data?.groups ?? [];
  const onlyGroup = groups.length === 1 && !notFound ? groups[0] : undefined;

  useEffect(() => {
    if (onlyGroup) router.replace(`/groups/${onlyGroup.id}/tasks`);
  }, [onlyGroup, router]);

  if (!session.data || onlyGroup) {
    return (
      <SimplePage>
        <QueryState error={session.error} onRetry={() => void session.refetch()} />
      </SimplePage>
    );
  }

  if (groups.length === 0) {
    return (
      <SimplePage>
        <h1 className="text-h1">{t("familyTitle")}</h1>
        <p className="mt-3">{t("familyBody", { name: session.data.person.name })}</p>
        <LogoutButton className="mt-8 -ml-4" />
      </SimplePage>
    );
  }

  return (
    <SimplePage>
      <h1 className="text-h1">{t("chooseGroup")}</h1>
      <p className="mt-2 text-ink-muted">{t("chooseGroupHint")}</p>
      {notFound && <Notice className="mt-6">{t("groupNotFound")}</Notice>}
      <ul className="mt-8 divide-y divide-border overflow-hidden rounded-card border border-border bg-surface-raised">
        {groups.map((group) => (
          <li key={group.id}>
            <Link
              href={`/groups/${group.id}/tasks`}
              className="flex min-h-16 items-center gap-4 px-4 py-3 font-semibold no-underline hover:bg-surface-muted focus-visible:-outline-offset-2 lg:px-5"
            >
              <span className="flex-1">{group.name}</span>
              <ChevronRight aria-hidden className="size-5 text-ink-muted" />
            </Link>
          </li>
        ))}
      </ul>
      <LogoutButton className="mt-8 -ml-4" />
    </SimplePage>
  );
}
