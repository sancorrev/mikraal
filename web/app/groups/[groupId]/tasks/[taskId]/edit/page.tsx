import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { EditTaskScreen } from "@/components/tasks/task-form-screens";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("form");
  return { title: t("metaTitleEdit") };
}

export default async function EditTaskPage({ params }: { params: Promise<{ taskId: string }> }) {
  const { taskId } = await params;
  return <EditTaskScreen taskId={taskId} />;
}
