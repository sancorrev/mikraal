import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export const controlClass =
  "w-full rounded-control border border-border-control bg-surface-raised px-3 text-body text-ink placeholder:text-ink-muted disabled:bg-surface-muted disabled:text-ink-disabled aria-invalid:border-orange";

export function Input({ className, ...props }: ComponentProps<"input">) {
  return <input className={cn(controlClass, "h-12 lg:h-10", className)} {...props} />;
}

export function Textarea({ className, ...props }: ComponentProps<"textarea">) {
  return <textarea className={cn(controlClass, "min-h-28 py-2.5", className)} {...props} />;
}
