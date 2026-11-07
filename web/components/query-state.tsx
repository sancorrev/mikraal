"use client";

import { useTranslations } from "next-intl";
import { statusOf } from "@/lib/api/problem";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/ui/notice";

/** Loading line or a load error with a retry, for any query. */
export function QueryState({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const t = useTranslations("common");
  if (!error) {
    return (
      <p role="status" className="py-8 text-ink-muted">
        {t("loading")}
      </p>
    );
  }
  const status = statusOf(error);
  if (status === 401) return null;
  const message = status === 403 ? t("forbidden") : status === 404 ? t("notFound") : t("loadError");
  return (
    <Notice
      action={
        status === 403 || status === 404 ? undefined : (
          <Button size="dense" onClick={onRetry}>
            {t("retry")}
          </Button>
        )
      }
    >
      {message}
    </Notice>
  );
}
