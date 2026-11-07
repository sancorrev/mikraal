import { ChevronDown } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";
import { controlClass } from "./input";

type SelectProps = ComponentProps<"select"> & { dense?: boolean };

/** Native select: best keyboard and screen reader support on every platform. */
export function Select({ className, dense = false, ...props }: SelectProps) {
  return (
    <div className="relative">
      <select
        className={cn(
          controlClass,
          "appearance-none pr-10",
          dense ? "h-11 text-small lg:h-8" : "h-12 lg:h-10",
          className,
        )}
        {...props}
      />
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-ink-muted"
      />
    </div>
  );
}
