import { Slot } from "radix-ui";
import { cva, type VariantProps } from "class-variance-authority";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-control font-medium whitespace-nowrap no-underline transition-colors duration-[120ms] ease-enter disabled:pointer-events-none disabled:bg-surface-muted disabled:text-ink-disabled [&_svg]:size-5",
  {
    variants: {
      variant: {
        primary: "bg-violet text-on-violet hover:bg-[color-mix(in_srgb,var(--violet)_88%,var(--ink))]",
        strong: "bg-ink text-surface hover:bg-[color-mix(in_srgb,var(--ink)_85%,var(--surface))]",
        secondary: "border border-border-control bg-surface-raised text-ink hover:bg-surface-muted",
        ghost: "text-ink hover:bg-surface-muted",
      },
      size: {
        default: "h-12 px-4 text-body lg:h-10",
        dense: "h-11 px-3 text-small lg:h-8 [&_svg]:size-4",
        icon: "size-12 lg:size-10",
        iconDense: "size-11 lg:size-8 [&_svg]:size-4",
      },
    },
    defaultVariants: { variant: "secondary", size: "default" },
  },
);

type ButtonProps = ComponentProps<"button"> & VariantProps<typeof buttonVariants> & { asChild?: boolean };

export function Button({ className, variant, size, asChild = false, type = "button", ...props }: ButtonProps) {
  const Component = asChild ? Slot.Root : "button";
  return (
    <Component
      type={asChild ? undefined : type}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  );
}
