"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { useCapabilities, useTask } from "@/lib/api/queries";
import { PageHeader } from "@/components/page-header";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Notice } from "@/components/ui/notice";
import { TaskForm } from "./task-form";

export function CreateTaskScreen() {
  const t = useTranslations("form");
  const tCommon = useTranslations("common");
  const { group } = useGroup();
  const capabilities = useCapabilities(group.id);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={t("titleCreate")} />
      {!capabilities.data ? (
        <QueryState error={capabilities.error} onRetry={() => void capabilities.refetch()} />
      ) : capabilities.data.canCreateTasks ? (
        <TaskForm mode="create" />
      ) : (
        <Notice>{tCommon("forbidden")}</Notice>
      )}
    </div>
  );
}

export function EditTaskScreen({ taskId }: { taskId: string }) {
  const t = useTranslations("form");
  const tCommon = useTranslations("common");
  const { group } = useGroup();
  const query = useTask(group.id, taskId);
  // A new key remounts the form with the reloaded task after a conflict.
  const [formKey, setFormKey] = useState(0);

  function reload() {
    void query.refetch().then(() => setFormKey((key) => key + 1));
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader title={t("titleEdit")} />
      {!query.data ? (
        <QueryState error={query.error} onRetry={() => void query.refetch()} />
      ) : query.data.task.allowedActions.includes("edit") ? (
        <TaskForm key={formKey} mode="edit" task={query.data.task} etag={query.data.etag} onReload={reload} />
      ) : (
        <Notice>{tCommon("forbidden")}</Notice>
      )}
    </div>
  );
}
