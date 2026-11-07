"use client";

import { useTranslations } from "next-intl";
import { useState, type FormEvent } from "react";
import { useCreateTag } from "@/lib/api/mutations";
import { useCapabilities, useTags } from "@/lib/api/queries";
import { PageHeader } from "@/components/page-header";
import { QueryState } from "@/components/query-state";
import { useGroup } from "@/components/shell/group-context";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Notice } from "@/components/ui/notice";
import { TagRow } from "./tag-row";
import { TAG_NAME_MAX, tagWriteError, validateTagName, type TagNameErrorKey } from "./tag-name-error";

export function TagCatalogueScreen() {
  const t = useTranslations("tags");
  const { group } = useGroup();
  const capabilities = useCapabilities(group.id);
  const tags = useTags(group.id);

  if (capabilities.data && !capabilities.data.canManageTags) {
    return (
      <div className="flex flex-col gap-8">
        <PageHeader title={t("title")} />
        <Notice>{t("forbidden")}</Notice>
      </div>
    );
  }

  return (
    <div className="flex max-w-[680px] flex-col gap-8">
      <div className="flex flex-col gap-2">
        <PageHeader title={t("title")} />
        <p className="text-ink-muted">{t("intro")}</p>
      </div>
      <NewTagForm groupId={group.id} />
      <section aria-label={t("title")}>
        {!tags.data ? (
          <QueryState error={tags.error} onRetry={() => void tags.refetch()} />
        ) : tags.data.length === 0 ? (
          <p className="rounded-card border border-border bg-surface-raised p-4 lg:p-5">{t("empty")}</p>
        ) : (
          <ul className="divide-y divide-border rounded-card border border-border bg-surface-raised">
            {tags.data.map((tag) => (
              <TagRow key={tag.id} groupId={group.id} tag={tag} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function NewTagForm({ groupId }: { groupId: string }) {
  const t = useTranslations("tags");
  const tCommon = useTranslations("common");
  const create = useCreateTag(groupId);
  const [name, setName] = useState("");
  const [invalid, setInvalid] = useState<TagNameErrorKey>();
  const serverError = tagWriteError(create.error);
  const error = invalid ?? serverError;

  function submit(event: FormEvent) {
    event.preventDefault();
    const problem = validateTagName(name);
    setInvalid(problem);
    if (problem) return;
    create.mutate(name.trim(), { onSuccess: () => setName("") });
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-3 sm:flex-row sm:items-start">
      <Field
        label={t("newLabel")}
        error={error ? (error === "generic" ? tCommon("saveError") : t(error)) : undefined}
        className="flex-1"
      >
        {(control) => (
          <Input
            {...control}
            maxLength={TAG_NAME_MAX}
            value={name}
            onChange={(event) => {
              setName(event.target.value);
              setInvalid(undefined);
              create.reset();
            }}
          />
        )}
      </Field>
      <Button type="submit" variant="primary" disabled={create.isPending} className="sm:mt-8">
        {t("create")}
      </Button>
    </form>
  );
}
