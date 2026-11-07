import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { CreateTaskScreen } from "@/components/tasks/task-form-screens";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("form");
  return { title: t("metaTitleCreate") };
}

export default function NewTaskPage() {
  return <CreateTaskScreen />;
}
