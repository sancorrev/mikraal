import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { HomeScreen } from "@/components/session/home-screen";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("home");
  return { title: t("metaTitle") };
}

export default function HomePage() {
  return <HomeScreen />;
}
