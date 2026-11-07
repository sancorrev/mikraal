"use client";

import { Dialog as Primitive } from "radix-ui";
import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

type DialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  footer: ReactNode;
};

/** Modal on desktop, bottom sheet on mobile. */
export function Dialog({ open, onOpenChange, title, description, children, footer }: DialogProps) {
  const t = useTranslations("common");
  return (
    <Primitive.Root open={open} onOpenChange={onOpenChange}>
      <Primitive.Portal>
        <Primitive.Overlay className="fixed inset-0 z-40 animate-fade-in bg-black/40" />
        <Primitive.Content className="fixed inset-x-0 bottom-0 z-50 flex max-h-[90dvh] animate-rise-in flex-col gap-4 overflow-y-auto rounded-t-sheet bg-surface-raised p-4 pb-[max(1rem,env(safe-area-inset-bottom))] shadow-modal md:inset-x-auto md:top-1/2 md:bottom-auto md:left-1/2 md:w-[calc(100%-48px)] md:max-w-[480px] md:-translate-x-1/2 md:-translate-y-1/2 md:rounded-card md:p-6">
          <div className="flex items-start justify-between gap-4">
            <Primitive.Title className="font-heading text-h3 font-semibold">{title}</Primitive.Title>
            <Primitive.Close
              aria-label={t("close")}
              className="-mt-2 -mr-2 grid size-11 place-items-center rounded-control hover:bg-surface-muted"
            >
              <X aria-hidden className="size-5" />
            </Primitive.Close>
          </div>
          {description ? (
            <Primitive.Description className="text-body">{description}</Primitive.Description>
          ) : (
            <Primitive.Description className="sr-only">{title}</Primitive.Description>
          )}
          {children}
          <div className="flex flex-col-reverse gap-2 md:flex-row md:justify-end">{footer}</div>
        </Primitive.Content>
      </Primitive.Portal>
    </Primitive.Root>
  );
}
