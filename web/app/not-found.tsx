import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { SimplePage } from "@/components/simple-page";
import { buttonVariants } from "@/components/ui/button";

export default async function NotFound() {
  const t = await getTranslations("common");
  return (
    <SimplePage>
      <h1 className="text-h1">{t("notFound")}</h1>
      <Link href="/" className={buttonVariants({ className: "mt-6" })}>
        {t("goToTasks")}
      </Link>
    </SimplePage>
  );
}
