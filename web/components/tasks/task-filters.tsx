"use client";

import { Check, SlidersHorizontal } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import type { Directory, NamedRef, TaskStatus } from "@/lib/api/client";
import { taskStatuses } from "@/lib/task-status";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { OWNER_ALL, OWNER_ME, type useTaskFilters } from "./use-task-filters";

type Props = {
  filters: ReturnType<typeof useTaskFilters>;
  directory?: Directory;
  tags?: NamedRef[];
};

const dense = "h-11 text-small lg:h-8";

export function TaskFilters({ filters, directory, tags }: Props) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  const [open, setOpen] = useState(filters.refined);

  function toggleStatus(status: TaskStatus) {
    const next = filters.status.includes(status)
      ? filters.status.filter((value) => value !== status)
      : [...filters.status, status];
    filters.setFilter("status", next.join(","));
  }

  return (
    <section aria-label={t("filters")} className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end gap-3">
        <Field label={t("owner")} className="w-full sm:w-72">
          {(control) => (
            <Select
              dense
              {...control}
              value={filters.owner}
              onChange={(event) =>
                filters.setFilter("owner", event.target.value === OWNER_ME ? "" : event.target.value)
              }
            >
              <option value={OWNER_ME}>{t("ownerMe")}</option>
              <option value={OWNER_ALL}>{t("ownerAll")}</option>
              <option value="unassigned">{t("ownerUnassigned")}</option>
              <RefGroup label={t("people")} prefix="person" items={directory?.people} />
              <RefGroup label={t("positions")} prefix="position" items={directory?.positions} />
            </Select>
          )}
        </Field>
        <Button
          size="dense"
          variant="ghost"
          aria-expanded={open}
          aria-controls="task-filters-more"
          onClick={() => setOpen(!open)}
          className="lg:hidden"
        >
          <SlidersHorizontal aria-hidden />
          {open ? t("hideFilters") : t("showFilters")}
        </Button>
      </div>

      <div id="task-filters-more" className={cn("flex-col gap-4 lg:flex", open ? "flex" : "hidden")}>
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-body font-semibold">{t("status")}</legend>
          <div className="flex flex-wrap gap-2">
            {taskStatuses.map((status) => {
              const pressed = filters.status.includes(status);
              return (
                <Button
                  key={status}
                  size="dense"
                  aria-pressed={pressed}
                  onClick={() => toggleStatus(status)}
                  className={cn(pressed && "border-violet bg-violet-light hover:bg-violet-light")}
                >
                  {pressed && <Check aria-hidden />}
                  {tStatus(status)}
                </Button>
              );
            })}
          </div>
        </fieldset>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Field label={t("tag")}>
            {(control) => (
              <Select
                dense
                {...control}
                value={filters.tag}
                onChange={(event) => filters.setFilter("tag", event.target.value)}
              >
                <option value="">{t("anyTag")}</option>
                {tags?.map((tag) => (
                  <option key={tag.id} value={tag.id}>
                    {tag.name}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          <Field label={t("linked")}>
            {(control) => (
              <Select
                dense
                {...control}
                value={filters.linked}
                onChange={(event) => filters.setFilter("linked", event.target.value)}
              >
                <option value="">{t("anyLinked")}</option>
                <RefGroup label={t("councils")} prefix="council" items={directory?.councils} />
                <RefGroup label={t("events")} prefix="event" items={directory?.events} />
                <RefGroup label={t("positions")} prefix="position" items={directory?.positions} />
              </Select>
            )}
          </Field>
          <Field label={t("dueFrom")}>
            {(control) => (
              <Input
                type="date"
                {...control}
                className={dense}
                value={filters.from}
                max={filters.to || undefined}
                onChange={(event) => filters.setFilter("from", event.target.value)}
              />
            )}
          </Field>
          <Field label={t("dueTo")}>
            {(control) => (
              <Input
                type="date"
                {...control}
                className={dense}
                value={filters.to}
                min={filters.from || undefined}
                onChange={(event) => filters.setFilter("to", event.target.value)}
              />
            )}
          </Field>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <label className="flex min-h-11 items-center gap-3 lg:min-h-8">
            <input
              type="checkbox"
              className="size-5 accent-violet"
              checked={filters.overdue}
              onChange={(event) => filters.setFilter("overdue", event.target.checked ? "1" : "")}
            />
            {t("onlyOverdue")}
          </label>
          {filters.refined && (
            <Button size="dense" variant="ghost" onClick={filters.clearRefinements}>
              {t("clearFilters")}
            </Button>
          )}
        </div>
      </div>
    </section>
  );
}

function RefGroup({ label, prefix, items }: { label: string; prefix: string; items?: NamedRef[] }) {
  if (!items?.length) return null;
  return (
    <optgroup label={label}>
      {items.map((item) => (
        <option key={item.id} value={`${prefix}:${item.id}`}>
          {item.name}
        </option>
      ))}
    </optgroup>
  );
}
