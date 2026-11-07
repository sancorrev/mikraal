"use client";

import { CircleUser } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { LogoutButton } from "@/components/session/logout-button";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { useGroup } from "./group-context";

/** Mobile account sheet: who is signed in, group and logout. */
export function AccountMenu() {
  const t = useTranslations("shell");
  const { group, session } = useGroup();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button variant="ghost" size="icon" aria-label={t("account")} onClick={() => setOpen(true)} className="-mr-3">
        <CircleUser aria-hidden />
      </Button>
      <Dialog
        open={open}
        onOpenChange={setOpen}
        title={t("account")}
        footer={<LogoutButton className="justify-start" />}
      >
        <dl className="flex flex-col gap-3">
          <div>
            <dt className="text-small text-ink-muted">{t("signedInAs")}</dt>
            <dd className="font-semibold">{session.person.name}</dd>
          </div>
          <div>
            <dt className="text-small text-ink-muted">{t("group")}</dt>
            <dd className="font-semibold">{group.name}</dd>
          </div>
        </dl>
        {session.groups.length > 1 && (
          <Link href="/" className="underline">
            {t("changeGroup")}
          </Link>
        )}
      </Dialog>
    </>
  );
}
