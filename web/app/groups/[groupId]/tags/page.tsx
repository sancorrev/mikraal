import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { TagCatalogueScreen } from "@/components/tags/tag-catalogue-screen";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("tags");
  return { title: t("metaTitle") };
}

export default function TagsPage() {
  return <TagCatalogueScreen />;
}
