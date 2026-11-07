"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useRef, useState, type FormEvent } from "react";
import { v7 as uuidv7 } from "uuid";
import type { LinkRef, OwnerRef, RecurrencePeriod, Schemas, Task } from "@/lib/api/client";
import { useCreateTask, useUpdateTask } from "@/lib/api/mutations";
import { useDirectory, useTags } from "@/lib/api/queries";
import { ApiError, fieldKey, statusOf } from "@/lib/api/problem";
import { MobileActionBar } from "@/components/mobile-action-bar";
import { useGroup } from "@/components/shell/group-context";
import { Button, buttonVariants } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Input, Textarea } from "@/components/ui/input";
import { MultiSelect, type Option } from "@/components/ui/multi-select";
import { Notice } from "@/components/ui/notice";
import { Select } from "@/components/ui/select";

const DEFAULT_REMINDER_DAYS = 2;
const periods: RecurrencePeriod[] = ["weekly", "monthly", "round"];
/** Validation codes with their own message; any other code gets the generic one. */
const knownCodes = [
  "required",
  "too_long",
  "too_many",
  "out_of_range",
  "requires_due_date",
  "not_found",
  "invalid",
] as const;
type KnownCode = (typeof knownCodes)[number];
const isKnownCode = (code: string): code is KnownCode => (knownCodes as readonly string[]).includes(code);

type Values = {
  title: string;
  description: string;
  owners: string[];
  dueDate: string;
  reminderDays: string;
  tagIds: string[];
  links: string[];
  recurrence: RecurrencePeriod | "";
};

type TaskFormProps = { mode: "create" } | { mode: "edit"; task: Task; etag: string; onReload: () => void };

/** Owners and links travel as `type:id` strings inside the form. */
const encode = (ref: { type: string; id: string }) => `${ref.type}:${ref.id}`;
function decode<T extends { type: string; id: string }>(value: string): T {
  const [type, id] = value.split(":");
  return { type, id } as T;
}

function initialValues(task?: Task): Values {
  return {
    title: task?.title ?? "",
    description: task?.description ?? "",
    owners: task?.owners.map(encode) ?? [],
    dueDate: task?.dueDate ?? "",
    reminderDays: String(task?.reminderDaysBefore ?? DEFAULT_REMINDER_DAYS),
    tagIds: task?.tags.map((tag) => tag.id) ?? [],
    links: task?.links.map(encode) ?? [],
    recurrence: task?.recurrence?.period ?? "",
  };
}

const sameSet = (a: string[], b: string[]) => a.length === b.length && a.every((item) => b.includes(item));

function toCreate(id: string, values: Values): Schemas["CreateTask"] {
  const hasDue = values.dueDate !== "";
  return {
    id,
    title: values.title.trim(),
    description: values.description.trim() || undefined,
    owners: values.owners.map(decode<OwnerRef>),
    dueDate: hasDue ? values.dueDate : undefined,
    reminderDaysBefore: hasDue ? Number(values.reminderDays) : undefined,
    tagIds: values.tagIds,
    links: values.links.map(decode<LinkRef>),
    recurrence: hasDue && values.recurrence ? { period: values.recurrence } : undefined,
  };
}

/** Merge patch with only what changed, so the history records real edits. */
function toPatch(initial: Values, values: Values): Schemas["UpdateTask"] {
  const patch: Schemas["UpdateTask"] = {};
  const hasDue = values.dueDate !== "";
  if (values.title.trim() !== initial.title) patch.title = values.title.trim();
  if (values.description.trim() !== initial.description) patch.description = values.description.trim() || null;
  if (!sameSet(values.owners, initial.owners)) patch.owners = values.owners.map(decode<OwnerRef>);
  if (values.dueDate !== initial.dueDate) patch.dueDate = hasDue ? values.dueDate : null;
  if (hasDue && (values.reminderDays !== initial.reminderDays || !initial.dueDate)) {
    patch.reminderDaysBefore = Number(values.reminderDays);
  }
  if (!hasDue && initial.dueDate) patch.reminderDaysBefore = null;
  if (!sameSet(values.tagIds, initial.tagIds)) patch.tagIds = values.tagIds;
  if (!sameSet(values.links, initial.links)) patch.links = values.links.map(decode<LinkRef>);
  return patch;
}

export function TaskForm(props: TaskFormProps) {
  const t = useTranslations("form");
  const tList = useTranslations("list");
  const tRecurrence = useTranslations("recurrence");
  const tCommon = useTranslations("common");
  const router = useRouter();
  const { group } = useGroup();
  const directory = useDirectory(group.id).data;
  const catalogue = useTags(group.id).data;
  const task = props.mode === "edit" ? props.task : undefined;
  const [initial] = useState(() => initialValues(task));
  const [values, setValues] = useState(initial);
  // Generated once per form: retries of the same create are idempotent.
  const [newId] = useState(() => uuidv7());
  const [titleMissing, setTitleMissing] = useState(false);
  const [unchanged, setUnchanged] = useState(false);
  const summaryRef = useRef<HTMLDivElement>(null);
  const create = useCreateTask(group.id);
  const update = useUpdateTask(group.id, task?.id ?? "");
  const mutation = props.mode === "create" ? create : update;
  const base = `/groups/${group.id}/tasks`;
  const hasDue = values.dueDate !== "";

  const serverErrors = new Map(
    (mutation.error instanceof ApiError ? mutation.error.fieldErrors : []).map((error) => [
      fieldKey(error.field),
      error.code,
    ]),
  );
  const conflict = statusOf(mutation.error) === 412;

  function errorFor(field: string): string | undefined {
    if (field === "title" && titleMissing) return t("errors.titleRequired");
    const code = serverErrors.get(field);
    if (!code) return undefined;
    return isKnownCode(code) ? t(`errors.${code}`) : t("errors.generic");
  }

  function set<K extends keyof Values>(key: K, value: Values[K]) {
    setValues((current) => ({ ...current, [key]: value }));
    setUnchanged(false);
  }

  function showSummary() {
    requestAnimationFrame(() => summaryRef.current?.focus());
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const missing = values.title.trim() === "";
    setTitleMissing(missing);
    if (missing) return showSummary();
    const onError = showSummary;
    const onSuccess = ({ task: saved }: { task: Task }) => router.push(`${base}/${saved.id}`);

    if (props.mode === "create") {
      create.mutate(toCreate(newId, values), { onSuccess, onError });
      return;
    }
    const patch = toPatch(initial, values);
    if (Object.keys(patch).length === 0) {
      setUnchanged(true);
      return;
    }
    update.mutate({ etag: props.etag, body: patch }, { onSuccess, onError });
  }

  const ownerOptions: Option[] = [
    ...(directory?.people ?? []).map((person) => ({
      value: `person:${person.id}`,
      label: person.name,
      group: tList("people"),
    })),
    ...(directory?.positions ?? []).map((position) => ({
      value: `position:${position.id}`,
      label: position.name,
      group: tList("positions"),
    })),
  ];
  const linkOptions: Option[] = [
    ...(directory?.councils ?? []).map((council) => ({
      value: `council:${council.id}`,
      label: council.name,
      group: tList("councils"),
    })),
    ...(directory?.events ?? []).map((item) => ({
      value: `event:${item.id}`,
      label: item.name,
      group: tList("events"),
    })),
    ...(directory?.positions ?? []).map((position) => ({
      value: `position:${position.id}`,
      label: position.name,
      group: tList("positions"),
    })),
  ];
  const tagOptions: Option[] = (catalogue ?? []).map((tag) => ({ value: tag.id, label: tag.name }));
  const fallbackLabels = Object.fromEntries([
    ...(task?.owners ?? []).map((owner) => [encode(owner), owner.name]),
    ...(task?.links ?? []).map((link) => [encode(link), link.name]),
    ...(task?.tags ?? []).map((tag) => [tag.id, tag.name]),
  ]);

  const hasErrors = titleMissing || serverErrors.size > 0;
  const otherError = mutation.error && !conflict && serverErrors.size === 0;
  const submitLabel = mutation.isPending ? t("saving") : props.mode === "create" ? t("submitCreate") : t("submitEdit");

  return (
    <form id="task-form" onSubmit={submit} noValidate className="flex max-w-[680px] flex-col gap-8">
      <div ref={summaryRef} tabIndex={-1} className="flex flex-col gap-4 empty:hidden focus:outline-none">
        {conflict && props.mode === "edit" && (
          <Notice
            action={
              <Button size="dense" onClick={props.onReload}>
                {t("reload")}
              </Button>
            }
          >
            {t("conflict")}
          </Notice>
        )}
        {hasErrors && <Notice>{t("fixErrors")}</Notice>}
        {otherError && (
          <Notice>{statusOf(mutation.error) === 403 ? tCommon("forbidden") : tCommon("saveError")}</Notice>
        )}
        {unchanged && <Notice tone="neutral">{t("noChanges")}</Notice>}
      </div>

      <Field label={t("title")} hint={t("titleHint")} error={errorFor("title")}>
        {(control) => (
          <Input
            {...control}
            required
            maxLength={200}
            autoComplete="off"
            value={values.title}
            onChange={(event) => set("title", event.target.value)}
          />
        )}
      </Field>

      <Field
        label={
          <>
            {t("description")} <Optional />
          </>
        }
        error={errorFor("description")}
      >
        {(control) => (
          <Textarea
            {...control}
            maxLength={5000}
            value={values.description}
            onChange={(event) => set("description", event.target.value)}
          />
        )}
      </Field>

      <MultiSelect
        label={t("owners")}
        optional
        hint={t("ownersHint")}
        error={errorFor("owners")}
        addLabel={t("ownersAdd")}
        emptyLabel={t("ownersEmpty")}
        options={ownerOptions}
        value={values.owners}
        onChange={(next) => set("owners", next)}
        fallbackLabels={fallbackLabels}
      />

      <div className="grid gap-6 sm:grid-cols-2">
        <Field
          label={
            <>
              {t("dueDate")} <Optional />
            </>
          }
          error={errorFor("dueDate")}
        >
          {(control) => (
            <Input
              {...control}
              type="date"
              value={values.dueDate}
              onChange={(event) => set("dueDate", event.target.value)}
            />
          )}
        </Field>
        <Field label={t("reminder")} hint={t("reminderHint")} error={errorFor("reminderDaysBefore")}>
          {(control) => (
            <div className="flex items-center gap-3">
              <Input
                {...control}
                type="number"
                inputMode="numeric"
                min={0}
                max={30}
                disabled={!hasDue}
                value={values.reminderDays}
                onChange={(event) => set("reminderDays", event.target.value)}
                className="w-24"
              />
              <span className="text-ink-muted">{t("reminderUnit")}</span>
            </div>
          )}
        </Field>
      </div>

      <MultiSelect
        label={t("tags")}
        optional
        error={errorFor("tagIds")}
        addLabel={t("tagsAdd")}
        emptyLabel={t("tagsEmpty")}
        options={tagOptions}
        value={values.tagIds}
        onChange={(next) => set("tagIds", next)}
        fallbackLabels={fallbackLabels}
      />

      <MultiSelect
        label={t("links")}
        optional
        hint={t("linksHint")}
        error={errorFor("links")}
        addLabel={t("linksAdd")}
        emptyLabel={t("linksEmpty")}
        options={linkOptions}
        value={values.links}
        onChange={(next) => set("links", next)}
        fallbackLabels={fallbackLabels}
      />

      {props.mode === "create" ? (
        <Field
          label={t("recurrence")}
          hint={t("recurrenceHint")}
          error={errorFor("recurrence")}
          className="sm:max-w-xs"
        >
          {(control) => (
            <Select
              {...control}
              disabled={!hasDue}
              value={hasDue ? values.recurrence : ""}
              onChange={(event) => set("recurrence", event.target.value as Values["recurrence"])}
            >
              <option value="">{tRecurrence("none")}</option>
              {periods.map((period) => (
                <option key={period} value={period}>
                  {tRecurrence(period)}
                </option>
              ))}
            </Select>
          )}
        </Field>
      ) : (
        <div className="flex flex-col gap-1">
          <p className="font-semibold">{t("recurrence")}</p>
          <p>{values.recurrence ? tRecurrence(values.recurrence) : tRecurrence("none")}</p>
          <p className="text-small text-ink-muted">{t("recurrenceFixed")}</p>
        </div>
      )}

      <div className="flex items-center gap-3">
        <MobileActionBar>
          <Button type="submit" variant="primary" disabled={mutation.isPending}>
            {submitLabel}
          </Button>
        </MobileActionBar>
        <Link href={task ? `${base}/${task.id}` : base} className={buttonVariants({ variant: "ghost" })}>
          {tCommon("cancel")}
        </Link>
      </div>
      <div aria-hidden className="h-16 lg:hidden" />
    </form>
  );
}

function Optional() {
  const t = useTranslations("common");
  return <span className="font-normal text-ink-muted">{t("optional")}</span>;
}
