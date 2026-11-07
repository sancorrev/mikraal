import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { TaskDetailScreen } from "@/components/tasks/task-detail-screen";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("detail");
  return { title: t("metaTitle") };
}

export default async function TaskPage({ params }: { params: Promise<{ taskId: string }> }) {
  const { taskId } = await params;
  return <TaskDetailScreen taskId={taskId} />;
}
