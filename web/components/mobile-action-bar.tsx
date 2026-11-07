import type { ReactNode } from "react";

/**
 * Mobile: the primary action sits in a fixed bar above the bottom navigation.
 * Desktop: it flows inline where it is placed.
 */
export function MobileActionBar({ children }: { children: ReactNode }) {
  return (
    <div className="fixed inset-x-0 bottom-[calc(4rem+env(safe-area-inset-bottom))] z-20 border-t border-border bg-surface px-4 py-3 md:px-6 lg:static lg:border-0 lg:bg-transparent lg:p-0 [&>*]:w-full lg:[&>*]:w-auto">
      {children}
    </div>
  );
}
