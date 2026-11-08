import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PageHeader({
  title,
  description,
  children,
  className,
}: {
  title: ReactNode;
  /** One line under the title saying what the page is for. */
  description?: ReactNode;
  children?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-wrap items-end justify-between gap-4", className)}>
      <div className="flex min-w-0 flex-col gap-1.5">
        <h1 className="text-h1 break-words">{title}</h1>
        {description && <p className="text-ink-muted">{description}</p>}
      </div>
      {children}
    </div>
  );
}
