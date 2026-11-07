"use client";

import { ArrowLeft, Pencil, Repeat, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import type { TaskStatus } from "@/lib/api/client";
import { useChangeStatus, useDeleteTask, useEndRecurrence } from "@/lib/api/mutations";
import { useTask } from "@/lib/api/queries";
import { formatInstantDate } from "@/lib/dates";
import { PageHeader } from "@/components/page-header";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Notice } from "@/components/ui/notice";
import { HistoryList } from "./history-list";
import { StatusActions } from "./status-actions";
import { TaskFields } from "./task-fields";
import { TaskState } from "./task-state";
import { writeErrorKind } from "./write-error";

const card = "rounded-card border border-border bg-surface-raised p-4 lg:p-5";

export function TaskDetailScreen({ taskId }: { taskId: string }) {
  const t = useTranslations("detail");
  const tStatus = useTranslations("status");
  const tCommon = useTranslations("common");
  const locale = useLocale();
  const router = useRouter();
  const { group } = useGroup();
  const query = useTask(group.id, taskId);
  const changeStatus = useChangeStatus(group.id, taskId);
  const deleteTask = useDeleteTask(group.id, taskId);
  const endRecurrence = useEndRecurrence(group.id, taskId);
  const [confirm, setConfirm] = useState<"delete" | "endRecurrence" | null>(null);
  const [announcement, setAnnouncement] = useState("");
  const listHref = `/groups/${group.id}/tasks`;

  if (!query.data) {
    return (
      <div className="flex flex-col gap-6">
        <BackLink href={listHref} label={t("back")} />
        <QueryState error={query.error} onRetry={() => void query.refetch()} />
      </div>
    );
  }

  const { task, etag } = query.data;
  const can = (action: (typeof task.allowedActions)[number]) => task.allowedActions.includes(action);
  const errorKind = writeErrorKind(changeStatus.error ?? deleteTask.error ?? endRecurrence.error);
  const busy = changeStatus.isPending || deleteTask.isPending || endRecurrence.isPending;
  const hasActions = can("edit") || can("delete") || (can("end_recurrence") && task.recurrence?.active);

  function reload() {
    changeStatus.reset();
    deleteTask.reset();
    endRecurrence.reset();
    void query.refetch();
  }

  function onStatusChange(status: TaskStatus, pauseReason?: string) {
    setAnnouncement("");
    return changeStatus
      .mutateAsync({ etag, status, pauseReason })
      .then(() => setAnnouncement(t("statusChanged", { status: tStatus(status) })));
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-4">
        <BackLink href={listHref} label={t("back")} />
        <PageHeader title={task.title} />
        <TaskState task={task} />
        <p className="text-small text-ink-muted">
          {t("createdBy", {
            name: task.createdBy.name,
            date: formatInstantDate(task.createdAt, locale, group.timeZone),
          })}
        </p>
      </div>

      {errorKind && (
        <Notice
          action={
            errorKind === "conflict" || errorKind === "transition" ? (
              <Button size="dense" onClick={reload}>
                {t("reload")}
              </Button>
            ) : undefined
          }
        >
          {errorKind === "conflict"
            ? t("conflict")
            : errorKind === "transition"
              ? t("invalidTransition")
              : errorKind === "forbidden"
                ? tCommon("forbidden")
                : tCommon("saveError")}
        </Notice>
      )}
      <p role="status" className="sr-only">
        {announcement}
      </p>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px] lg:items-start">
        <div className="flex flex-col gap-6 lg:order-2">
          {(can("change_status") || task.status === "paused") && (
            <section aria-labelledby="status-heading" className={card}>
              <h2 id="status-heading" className="text-h3">
                {t("statusTitle")}
              </h2>
              {task.status === "paused" && task.pauseReason && (
                <div className="mt-3">
                  <p className="text-small font-semibold text-ink-muted">{t("pauseReason")}</p>
                  <p className="mt-1 whitespace-pre-line">{task.pauseReason}</p>
                </div>
              )}
              {can("change_status") && (
                <div className="mt-4">
                  <StatusActions task={task} pending={busy} onChange={onStatusChange} />
                </div>
              )}
            </section>
          )}

          {hasActions && (
            <section aria-labelledby="actions-heading" className={card}>
              <h2 id="actions-heading" className="text-h3">
                {t("actions")}
              </h2>
              <div className="mt-4 flex flex-col gap-2">
                {can("edit") && (
                  <Button asChild>
                    <Link href={`${listHref}/${task.id}/edit`}>
                      <Pencil aria-hidden />
                      {t("edit")}
                    </Link>
                  </Button>
                )}
                {can("end_recurrence") && task.recurrence?.active && (
                  <Button disabled={busy} onClick={() => setConfirm("endRecurrence")}>
                    <Repeat aria-hidden />
                    {t("endRecurrence")}
                  </Button>
                )}
                {can("delete") && (
                  <Button variant="ghost" disabled={busy} onClick={() => setConfirm("delete")}>
                    <Trash2 aria-hidden />
                    {t("delete")}
                  </Button>
                )}
              </div>
            </section>
          )}
        </div>

        <div className="flex flex-col gap-8 lg:order-1">
          <section aria-label={t("metaTitle")} className={card}>
            <TaskFields task={task} />
          </section>
          <HistoryList groupId={group.id} taskId={task.id} timeZone={group.timeZone} />
        </div>
      </div>

      <ConfirmDialog
        open={confirm === "delete"}
        onOpenChange={(open) => setConfirm(open ? "delete" : null)}
        title={t("deleteDialog.title")}
        description={t("deleteDialog.body", { title: task.title })}
        confirmLabel={t("deleteDialog.confirm")}
        pending={deleteTask.isPending}
        onConfirm={() =>
          deleteTask.mutate(etag, { onSuccess: () => router.push(listHref), onSettled: () => setConfirm(null) })
        }
      />
      <ConfirmDialog
        open={confirm === "endRecurrence"}
        onOpenChange={(open) => setConfirm(open ? "endRecurrence" : null)}
        title={t("endRecurrenceDialog.title")}
        description={t("endRecurrenceDialog.body")}
        confirmLabel={t("endRecurrenceDialog.confirm")}
        pending={endRecurrence.isPending}
        onConfirm={() => endRecurrence.mutate(undefined, { onSettled: () => setConfirm(null) })}
      />
    </div>
  );
}

function BackLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="inline-flex min-h-11 items-center gap-2 self-start text-small underline lg:min-h-8">
      <ArrowLeft aria-hidden className="size-4" />
      {label}
    </Link>
  );
}
