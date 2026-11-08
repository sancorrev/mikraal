import { cn } from "@/lib/utils";
import { initials, type AvatarTone } from "@/lib/people";

const tones: Record<AvatarTone, string> = {
  violet: "bg-violet-light",
  soft: "bg-violet-soft",
  neutral: "bg-surface-muted",
};

type AvatarProps = {
  name: string;
  tone?: AvatarTone;
  /** People are round; positions are square so they never pass for a person. */
  shape?: "round" | "square";
  size?: "sm" | "md";
  className?: string;
};

/** Initials on a soft background. Decorative: say who it is in text next to it. */
export function Avatar({ name, tone = "violet", shape = "round", size = "md", className }: AvatarProps) {
  return (
    <span
      aria-hidden
      title={name}
      className={cn(
        "inline-grid shrink-0 place-items-center font-semibold text-ink select-none",
        size === "md" ? "size-9 text-small" : "size-[30px] text-[10px] leading-none",
        shape === "round" ? "rounded-full" : "rounded-chip",
        tones[tone],
        className,
      )}
    >
      {initials(name)}
    </span>
  );
}
