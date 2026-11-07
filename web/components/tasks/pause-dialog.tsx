"use client";

import { useTranslations } from "next-intl";
import { useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Field } from "@/components/ui/field";
import { Textarea } from "@/components/ui/input";

type PauseDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  pending: boolean;
  onConfirm: (reason: string) => void;
};

/** Pausing always asks for a reason (kraal-tasks.md, Statuses). */
export function PauseDialog({ open, onOpenChange, pending, onConfirm }: PauseDialogProps) {
  const t = useTranslations("detail.pauseDialog");
  const tCommon = useTranslations("common");
  const [reason, setReason] = useState("");
  const [showError, setShowError] = useState(false);

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = reason.trim();
    if (!trimmed) {
      setShowError(true);
      return;
    }
    onConfirm(trimmed);
  }

  function change(next: boolean) {
    if (!next) {
      setReason("");
      setShowError(false);
    }
    onOpenChange(next);
  }

  return (
    <Dialog
      open={open}
      onOpenChange={change}
      title={t("title")}
      description={t("body")}
      footer={
        <>
          <Button onClick={() => change(false)}>{tCommon("cancel")}</Button>
          <Button variant="primary" type="submit" form="pause-form" disabled={pending}>
            {t("confirm")}
          </Button>
        </>
      }
    >
      <form id="pause-form" onSubmit={submit} noValidate>
        <Field label={t("reason")} hint={t("reasonHint")} error={showError ? t("reasonRequired") : undefined}>
          {(control) => (
            <Textarea
              {...control}
              required
              maxLength={500}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              className="min-h-24"
            />
          )}
        </Field>
      </form>
    </Dialog>
  );
}
