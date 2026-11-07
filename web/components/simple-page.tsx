import type { ReactNode } from "react";
import { Wordmark } from "./wordmark";

/** Pages outside the app shell: login, group choice, family notice. */
export function SimplePage({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-dvh px-4 md:px-6 xl:px-8">
      <header className="mx-auto flex h-16 max-w-[680px] items-center">
        <Wordmark />
      </header>
      <main id="main" className="mx-auto max-w-[680px] pt-6 pb-16 md:pt-12">
        {children}
      </main>
    </div>
  );
}
