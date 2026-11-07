"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import type { Task, TaskStatus } from "@/lib/api/client";
import { transitions } from "@/lib/task-status";
import { Button } from "@/components/ui/button";
import { PauseDialog } from "./pause-dialog";

type StatusActionsProps = {
  task: Task;
  pending: boolean;
  onChange: (status: TaskStatus, pauseReason?: string) => Promise<unknown>;
};

/** One button per allowed transition; finishing (or reopening) is the primary one. */
export function StatusActions({ task, pending, onChange }: StatusActionsProps) {
  const t = useTranslations("detail");
  const [pausing, setPausing] = useState(false);

  function label(target: TaskStatus) {
    if (target === "in_progress" && task.status === "finished") return t("reopen");
    if (target === "in_progress" && task.status === "paused") return t("resume");
    return t(`to.${target}`);
  }

  const isPrimary = (target: TaskStatus) => target === "finished" || task.status === "finished";
  const targets = [...transitions[task.status]].sort((a, b) => Number(isPrimary(b)) - Number(isPrimary(a)));

  return (
    <div className="flex flex-col gap-2">
      {targets.map((target) => (
        <Button
          key={target}
          variant={isPrimary(target) ? "primary" : "secondary"}
          disabled={pending}
          onClick={() => (target === "paused" ? setPausing(true) : void onChange(target))}
        >
          {label(target)}
        </Button>
      ))}
      <PauseDialog
        open={pausing}
        onOpenChange={setPausing}
        pending={pending}
        onConfirm={(reason) =>
          void onChange("paused", reason).then(
            () => setPausing(false),
            () => setPausing(false),
          )
        }
      />
    </div>
  );
}
