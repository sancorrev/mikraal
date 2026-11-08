"use client";

import { Check, Columns3, List, SlidersHorizontal, User, type LucideIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { Tabs } from "radix-ui";
import { useState } from "react";
import type { Directory, NamedRef, TaskStatus } from "@/lib/api/client";
import { taskStatuses } from "@/lib/task-status";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Field } from "@/components/ui/field";
import { FilterMenu } from "@/components/ui/filter-menu";
import { Input } from "@/components/ui/input";
import type { Option } from "@/components/ui/multi-select";
import { Select } from "@/components/ui/select";
import { OWNER_ALL, OWNER_ME, type TaskView, type useTaskFilters } from "./use-task-filters";

type Props = {
  filters: ReturnType<typeof useTaskFilters>;
  directory?: Directory;
  tags?: NamedRef[];
};

const dense = "h-11 text-small lg:h-8";
const pressed = "border-violet bg-violet-light hover:bg-violet-light";

/**
 * View switch (inside the page's Tabs root), «Mis tareas», tag and link menus, and the rest of
 * the filters behind «Más filtros». Every value lives in the URL.
 */
export function TaskToolbar({ filters, directory, tags }: Props) {
  const t = useTranslations("list");
  const [open, setOpen] = useState(filters.hiddenCount > 0);
  const mine = filters.owner === OWNER_ME;
  const tagOptions: Option[] = (tags ?? []).map((tag) => ({ value: tag.id, label: tag.name }));
  const linkedOptions: Option[] = [
    ...refOptions("council", t("councils"), directory?.councils),
    ...refOptions("event", t("events"), directory?.events),
    ...refOptions("position", t("positions"), directory?.positions),
  ];

  return (
    <section aria-label={t("filters")} className="flex flex-col gap-4 border-b border-border pb-4">
      <div className="flex flex-wrap items-center gap-2.5">
        <Tabs.List aria-label={t("view")} className="inline-flex gap-0.5 rounded-control bg-well p-1">
          <ViewTab value="board" icon={Columns3} label={t("viewBoard")} />
          <ViewTab value="list" icon={List} label={t("viewList")} />
        </Tabs.List>
        <Button
          size="dense"
          aria-pressed={mine}
          onClick={() => filters.setFilter("owner", mine ? "" : OWNER_ME)}
          className={cn(mine && pressed)}
        >
          <User aria-hidden />
          {t("ownerMe")}
        </Button>
        <FilterMenu
          label={t("tag")}
          anyValue={t("anyTagShort")}
          anyOption={t("anyTag")}
          options={tagOptions}
          value={filters.tag}
          onChange={(value) => filters.setFilter("tag", value)}
        />
        <FilterMenu
          label={t("linked")}
          anyValue={t("anyLinkedShort")}
          anyOption={t("anyLinked")}
          options={linkedOptions}
          value={filters.linked}
          onChange={(value) => filters.setFilter("linked", value)}
        />
        {filters.refined && (
          <Button size="dense" variant="ghost" onClick={filters.clearRefinements}>
            {t("clearFilters")}
          </Button>
        )}
        <Button
          size="iconDense"
          aria-expanded={open}
          aria-controls="task-filters-more"
          aria-label={
            filters.hiddenCount > 0 ? t("moreFiltersActive", { count: filters.hiddenCount }) : t("moreFilters")
          }
          title={t("moreFilters")}
          onClick={() => setOpen(!open)}
          className={cn("relative ml-auto", open && "bg-surface-muted")}
        >
          <SlidersHorizontal aria-hidden />
          {filters.hiddenCount > 0 && (
            <span
              aria-hidden
              className="absolute -top-1.5 -right-1.5 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-violet px-1 text-overline leading-none font-semibold text-on-violet"
            >
              {filters.hiddenCount}
            </span>
          )}
        </Button>
      </div>

      <div
        id="task-filters-more"
        hidden={!open}
        className="flex flex-col gap-4 rounded-card border border-border bg-surface-raised p-4 lg:p-5"
      >
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label={t("owner")}>
            {(control) => (
              <Select
                dense
                {...control}
                value={filters.owner}
                onChange={(event) =>
                  filters.setFilter("owner", event.target.value === OWNER_ALL ? "" : event.target.value)
                }
              >
                <option value={OWNER_ALL}>{t("ownerAll")}</option>
                <option value={OWNER_ME}>{t("ownerMe")}</option>
                <option value="unassigned">{t("ownerUnassigned")}</option>
                <RefGroup label={t("people")} prefix="person" items={directory?.people} />
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
        <div className="flex flex-wrap items-end justify-between gap-4">
          <StatusFilter selected={filters.status} onChange={(next) => filters.setFilter("status", next.join(","))} />
          <label className="flex min-h-11 items-center gap-3 lg:min-h-8">
            <input
              type="checkbox"
              className="size-5 accent-violet"
              checked={filters.overdue}
              onChange={(event) => filters.setFilter("overdue", event.target.checked ? "1" : "")}
            />
            {t("onlyOverdue")}
          </label>
        </div>
      </div>
    </section>
  );
}

function ViewTab({ value, icon: Icon, label }: { value: TaskView; icon: LucideIcon; label: string }) {
  return (
    <Tabs.Trigger
      value={value}
      className="inline-flex h-11 items-center gap-2 rounded-lg px-3 text-small font-medium transition-colors duration-[120ms] hover:bg-[color-mix(in_srgb,var(--ink)_5%,transparent)] data-[state=active]:bg-surface-raised data-[state=active]:shadow-xs lg:h-9 [&_svg]:size-4"
    >
      <Icon aria-hidden />
      {label}
    </Tabs.Trigger>
  );
}

/** In the board, the statuses picked here are the columns shown. */
function StatusFilter({ selected, onChange }: { selected: TaskStatus[]; onChange: (next: TaskStatus[]) => void }) {
  const t = useTranslations("list");
  const tStatus = useTranslations("status");
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-body font-semibold">{t("status")}</legend>
      <div className="flex flex-wrap gap-2">
        {taskStatuses.map((status) => {
          const on = selected.includes(status);
          return (
            <Button
              key={status}
              size="dense"
              aria-pressed={on}
              onClick={() => onChange(on ? selected.filter((value) => value !== status) : [...selected, status])}
              className={cn(on && pressed)}
            >
              {on && <Check aria-hidden />}
              {tStatus(status)}
            </Button>
          );
        })}
      </div>
    </fieldset>
  );
}

function refOptions(prefix: string, group: string, items?: NamedRef[]): Option[] {
  return (items ?? []).map((item) => ({ value: `${prefix}:${item.id}`, label: item.name, group }));
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
