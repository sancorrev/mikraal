import { cn } from "@/lib/utils";

/** Text wordmark until the outlined logo asset is in the repo. */
export function Wordmark({ className }: { className?: string }) {
  return <span className={cn("font-heading text-h2 font-normal tracking-normal", className)}>mikraal</span>;
}
