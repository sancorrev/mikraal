import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type Tone = "done" | "warning" | "neutral";

const tones: Record<Tone, { box: string; dot: string }> = {
  done: { box: "bg-violet-light", dot: "bg-violet" },
  warning: { box: "bg-orange-light", dot: "bg-orange" },
  neutral: { box: "bg-surface-muted", dot: "bg-ink-muted" },
};

/** A state always carries its word; the dot only reinforces it. */
export function StateChip({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center gap-1.5 rounded-chip px-2 text-caption font-medium whitespace-nowrap text-ink",
        tones[tone].box,
      )}
    >
      <span aria-hidden className={cn("size-1.5 rounded-full", tones[tone].dot)} />
      {children}
    </span>
  );
}

export function TagChip({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-6 items-center rounded-chip bg-violet-light px-2 text-caption font-medium text-ink",
        className,
      )}
    >
      {children}
    </span>
  );
}
