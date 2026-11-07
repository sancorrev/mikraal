"use client";

import { ChevronRight } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useDevLogin } from "@/lib/api/mutations";
import { useDevPeople } from "@/lib/api/queries";
import { QueryState } from "@/components/query-state";
import { SimplePage } from "@/components/simple-page";
import { StateChip } from "@/components/ui/chip";
import { Notice } from "@/components/ui/notice";

export function LoginScreen() {
  const t = useTranslations("login");
  const router = useRouter();
  const people = useDevPeople();
  const login = useDevLogin();

  function enter(personId: string) {
    login.mutate(personId, { onSuccess: () => router.push("/") });
  }

  return (
    <SimplePage>
      <h1 className="text-h1">{t("title")}</h1>
      <p className="mt-2 text-ink-muted">{t("devNotice")}</p>

      {login.isError && <Notice className="mt-6">{t("error")}</Notice>}

      <section aria-labelledby="people-heading" className="mt-8">
        <h2 id="people-heading" className="sr-only">
          {t("peopleLabel")}
        </h2>
        {!people.data ? (
          <QueryState error={people.error} onRetry={() => void people.refetch()} />
        ) : people.data.length === 0 ? (
          <p>{t("empty")}</p>
        ) : (
          <ul className="divide-y divide-border overflow-hidden rounded-card border border-border bg-surface-raised">
            {people.data.map((person) => (
              <li key={person.id}>
                <button
                  type="button"
                  disabled={login.isPending}
                  onClick={() => enter(person.id)}
                  className="flex min-h-16 w-full items-center gap-4 px-4 py-3 text-left transition-colors duration-[120ms] hover:bg-surface-muted focus-visible:-outline-offset-2 disabled:text-ink-disabled lg:px-5"
                >
                  <span className="sr-only">{t("enterAs")}</span>
                  <span className="flex flex-1 flex-col">
                    <span className="font-semibold">{person.name}</span>
                    {person.positions && person.positions.length > 0 && (
                      <span className="text-small text-ink-muted">{person.positions.join(", ")}</span>
                    )}
                  </span>
                  <StateChip tone="neutral">{person.isScouter ? t("scouter") : t("family")}</StateChip>
                  <ChevronRight aria-hidden className="size-5 text-ink-muted" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </SimplePage>
  );
}
