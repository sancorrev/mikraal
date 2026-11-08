"use client";

import { Check } from "lucide-react";
import { DropdownMenu as Primitive } from "radix-ui";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

/**
 * shadcn-style dropdown menu on Radix: keyboard, focus and screen reader support come from Radix.
 * Not modal: the page is not hidden from screen readers nor locked while a menu is open.
 */
export function DropdownMenu({ modal = false, ...props }: ComponentProps<typeof Primitive.Root>) {
  return <Primitive.Root modal={modal} {...props} />;
}

export const DropdownMenuTrigger = Primitive.Trigger;
export const DropdownMenuRadioGroup = Primitive.RadioGroup;

export function DropdownMenuContent({
  className,
  sideOffset = 8,
  collisionPadding = 16,
  ...props
}: ComponentProps<typeof Primitive.Content>) {
  return (
    <Primitive.Portal>
      <Primitive.Content
        sideOffset={sideOffset}
        collisionPadding={collisionPadding}
        className={cn(
          "z-50 max-h-[min(400px,var(--radix-dropdown-menu-content-available-height))] min-w-56 animate-fade-in overflow-y-auto rounded-control border border-border bg-surface-raised p-1.5 shadow-popover",
          className,
        )}
        {...props}
      />
    </Primitive.Portal>
  );
}

const item =
  "relative flex min-h-11 cursor-pointer items-center gap-3 rounded-chip px-2.5 text-body text-ink no-underline select-none -outline-offset-2 data-[disabled]:pointer-events-none data-[disabled]:text-ink-disabled data-[highlighted]:bg-surface-muted lg:min-h-9 [&_svg]:size-4";

export function DropdownMenuItem({ className, ...props }: ComponentProps<typeof Primitive.Item>) {
  return <Primitive.Item className={cn(item, className)} {...props} />;
}

export function DropdownMenuRadioItem({ className, children, ...props }: ComponentProps<typeof Primitive.RadioItem>) {
  return (
    <Primitive.RadioItem className={cn(item, "pr-9", className)} {...props}>
      {children}
      <Primitive.ItemIndicator className="absolute right-2.5 inline-flex text-violet">
        <Check aria-hidden />
      </Primitive.ItemIndicator>
    </Primitive.RadioItem>
  );
}

export function DropdownMenuLabel({ className, ...props }: ComponentProps<typeof Primitive.Label>) {
  return (
    <Primitive.Label
      className={cn("px-2.5 pt-2 pb-1 text-overline font-semibold text-ink-muted uppercase", className)}
      {...props}
    />
  );
}

export function DropdownMenuSeparator({ className, ...props }: ComponentProps<typeof Primitive.Separator>) {
  return <Primitive.Separator className={cn("-mx-1.5 my-1.5 h-px bg-border", className)} {...props} />;
}
