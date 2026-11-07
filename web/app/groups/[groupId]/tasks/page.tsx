import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { Suspense } from "react";
import { TaskListScreen } from "@/components/tasks/task-list-screen";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("list");
  return { title: t("metaTitle") };
}

export default function TasksPage() {
  return (
    <Suspense>
      <TaskListScreen />
    </Suspense>
  );
}
