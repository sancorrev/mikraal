"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { statusOf } from "@/lib/api/problem";

const LOGIN_PATH = "/login";

export function QueryProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            retry: (count, error) => (statusOf(error) ?? 500) >= 500 && count < 2,
          },
        },
      }),
  );

  // Without a session, any call sends the person to the login.
  useEffect(() => {
    const onError = (error: unknown) => {
      if (statusOf(error) === 401 && pathname !== LOGIN_PATH) router.replace(LOGIN_PATH);
    };
    const stopQueries = client.getQueryCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") onError(event.action.error);
    });
    const stopMutations = client.getMutationCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") onError(event.action.error);
    });
    return () => {
      stopQueries();
      stopMutations();
    };
  }, [client, pathname, router]);

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
