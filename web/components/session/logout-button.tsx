"use client";

import { LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useLogout } from "@/lib/api/mutations";
import { Button } from "@/components/ui/button";

/** Ends the session and goes to the login, whatever the answer. */
export function useSignOut() {
  const router = useRouter();
  const logout = useLogout();
  return {
    pending: logout.isPending,
    signOut: () => logout.mutate(undefined, { onSettled: () => router.push("/login") }),
  };
}

export function LogoutButton({ className }: { className?: string }) {
  const t = useTranslations("shell");
  const { pending, signOut } = useSignOut();
  return (
    <Button variant="ghost" className={className} disabled={pending} onClick={signOut}>
      <LogOut aria-hidden />
      {t("logout")}
    </Button>
  );
}
