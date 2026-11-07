import { CircleAlert, Info } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type NoticeProps = {
  tone?: "warning" | "neutral";
  children: ReactNode;
  action?: ReactNode;
  className?: string;
};

/** Alert card: light orange with black text and an orange icon; never colour alone. */
export function Notice({ tone = "warning", children, action, className }: NoticeProps) {
  const Icon = tone === "warning" ? CircleAlert : Info;
  return (
    <div
      role={tone === "warning" ? "alert" : "status"}
      className={cn(
        "flex flex-col gap-3 rounded-card p-4 sm:flex-row sm:items-center lg:p-5",
        tone === "warning" ? "bg-orange-light" : "bg-surface-muted",
        className,
      )}
    >
      <div className="flex flex-1 items-start gap-3">
        <Icon aria-hidden className={cn("mt-px size-5", tone === "warning" ? "text-orange" : "text-ink-muted")} />
        <div className="flex-1">{children}</div>
      </div>
      {action}
    </div>
  );
}
