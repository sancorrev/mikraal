"use client";

import { Pencil, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState, type FormEvent } from "react";
import type { NamedRef } from "@/lib/api/client";
import { useDeleteTag, useRenameTag } from "@/lib/api/mutations";
import { Button } from "@/components/ui/button";
import { TagChip } from "@/components/ui/chip";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Field, FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { TAG_NAME_MAX, tagWriteError, validateTagName, type TagNameErrorKey } from "./tag-name-error";

export function TagRow({ groupId, tag }: { groupId: string; tag: NamedRef }) {
  const t = useTranslations("tags");
  const tCommon = useTranslations("common");
  const rename = useRenameTag(groupId);
  const remove = useDeleteTag(groupId);
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [name, setName] = useState(tag.name);
  const [invalid, setInvalid] = useState<TagNameErrorKey>();
  const renameError = invalid ?? tagWriteError(rename.error);
  const removeError = tagWriteError(remove.error);
  const message = (key: TagNameErrorKey | "generic") => (key === "generic" ? tCommon("saveError") : t(key));

  function startEditing() {
    setName(tag.name);
    setInvalid(undefined);
    rename.reset();
    setEditing(true);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const problem = validateTagName(name);
    setInvalid(problem);
    if (problem) return;
    if (name.trim() === tag.name) return setEditing(false);
    rename.mutate({ tagId: tag.id, name: name.trim() }, { onSuccess: () => setEditing(false) });
  }

  if (editing) {
    return (
      <li className="p-4 lg:p-5">
        <form onSubmit={submit} noValidate className="flex flex-col gap-3">
          <Field label={t("renameLabel", { name: tag.name })} error={renameError ? message(renameError) : undefined}>
            {(control) => (
              <Input
                {...control}
                autoFocus
                maxLength={TAG_NAME_MAX}
                value={name}
                onChange={(event) => {
                  setName(event.target.value);
                  setInvalid(undefined);
                }}
                onKeyDown={(event) => event.key === "Escape" && setEditing(false)}
              />
            )}
          </Field>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="primary" disabled={rename.isPending}>
              {t("save")}
            </Button>
            <Button variant="ghost" onClick={() => setEditing(false)}>
              {tCommon("cancel")}
            </Button>
          </div>
        </form>
      </li>
    );
  }

  return (
    <li className="flex flex-col gap-2 px-4 py-3 lg:px-5">
      <div className="flex items-center gap-2">
        <span className="flex-1">
          <TagChip className="text-small">{tag.name}</TagChip>
        </span>
        <Button variant="ghost" size="dense" onClick={startEditing} aria-label={t("renameAction", { name: tag.name })}>
          <Pencil aria-hidden />
          {t("rename")}
        </Button>
        <Button
          variant="ghost"
          size="dense"
          onClick={() => setConfirming(true)}
          aria-label={t("removeAction", { name: tag.name })}
        >
          <Trash2 aria-hidden />
          {t("remove")}
        </Button>
      </div>
      {removeError && <FieldError>{message(removeError)}</FieldError>}
      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={t("removeDialog.title", { name: tag.name })}
        description={t("removeDialog.body")}
        confirmLabel={t("removeDialog.confirm")}
        pending={remove.isPending}
        onConfirm={() => remove.mutate(tag.id, { onSettled: () => setConfirming(false) })}
      />
    </li>
  );
}
