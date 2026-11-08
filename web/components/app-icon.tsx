import { cn } from "@/lib/utils";

/** The mikraal app icon: the kraal circle around a flame, white on violet. Decorative. */
export function AppIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 1024 1024" aria-hidden className={cn("shrink-0", className)}>
      <rect width="1024" height="1024" rx="230" className="fill-violet" />
      <g transform="translate(152 152) scale(7.2)">
        <path
          d="M70.3 28.73A34 34 0 1 1 29.7 28.73"
          fill="none"
          stroke="#fff"
          strokeWidth="7.6"
          strokeLinecap="round"
        />
        <g transform="translate(6 -2) scale(0.88)" fill="#fff">
          <path d="M50 6C58 7 64.5 16 65 28C65.6 44 61 62 51.3 77L51.3 16Z" />
          <path d="M50 6C42 7 36 17 36 32C36 48 41 63 48.7 77L48.7 16Z" />
          <path d="M50 74V95" fill="none" stroke="#fff" strokeWidth="4.3" strokeLinecap="round" />
        </g>
      </g>
    </svg>
  );
}
