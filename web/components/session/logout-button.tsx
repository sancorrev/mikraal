"use client";

import { LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useLogout } from "@/lib/api/mutations";
import { Button } from "@/components/ui/button";

export function LogoutButton({ className }: { className?: string }) {
  const t = useTranslations("shell");
  const router = useRouter();
  const logout = useLogout();
  return (
    <Button
      variant="ghost"
      className={className}
      disabled={logout.isPending}
      onClick={() => logout.mutate(undefined, { onSettled: () => router.push("/login") })}
    >
      <LogOut aria-hidden />
      {t("logout")}
    </Button>
  );
}
