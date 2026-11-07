"use client";

import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { Button } from "./button";
import { Dialog } from "./dialog";

type ConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description: ReactNode;
  confirmLabel: string;
  pending?: boolean;
  onConfirm: () => void;
  children?: ReactNode;
};

export function ConfirmDialog({ confirmLabel, pending, onConfirm, onOpenChange, ...props }: ConfirmDialogProps) {
  const t = useTranslations("common");
  return (
    <Dialog
      {...props}
      onOpenChange={onOpenChange}
      footer={
        <>
          <Button onClick={() => onOpenChange(false)}>{t("cancel")}</Button>
          <Button variant="strong" disabled={pending} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    />
  );
}
