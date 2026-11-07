"use client";

import { Plus, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { Popover } from "radix-ui";
import { useId, useMemo, useState } from "react";
import { Button } from "./button";
import { TagChip } from "./chip";
import { FieldError } from "./field";
import { Input } from "./input";

export type Option = { value: string; label: string; group?: string };

type MultiSelectProps = {
  label: string;
  hint?: string;
  error?: string;
  addLabel: string;
  emptyLabel: string;
  options: Option[];
  value: string[];
  onChange: (value: string[]) => void;
  /** Names for selected values that are no longer offered (e.g. a removed position). */
  fallbackLabels?: Record<string, string>;
  optional?: boolean;
};

/** Selected items as removable chips plus a searchable checklist in a popover. */
export function MultiSelect({
  label,
  hint,
  error,
  addLabel,
  emptyLabel,
  options,
  value,
  onChange,
  fallbackLabels = {},
  optional = false,
}: MultiSelectProps) {
  const t = useTranslations("common");
  const tForm = useTranslations("form");
  const id = useId();
  const [search, setSearch] = useState("");
  const labels = useMemo(
    () => ({ ...fallbackLabels, ...Object.fromEntries(options.map((option) => [option.value, option.label])) }),
    [fallbackLabels, options],
  );
  const visible = options.filter((option) =>
    option.label.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()),
  );
  const groups = [...new Set(visible.map((option) => option.group ?? ""))];
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(" ") || undefined;

  function toggle(optionValue: string) {
    onChange(value.includes(optionValue) ? value.filter((item) => item !== optionValue) : [...value, optionValue]);
  }

  return (
    <fieldset className="flex min-w-0 flex-col gap-2" aria-describedby={describedBy}>
      <legend className="mb-2 text-body font-semibold">
        {label} {optional && <span className="font-normal text-ink-muted">{t("optional")}</span>}
      </legend>
      {hint && (
        <p id={`${id}-hint`} className="-mt-1 text-small text-ink-muted">
          {hint}
        </p>
      )}
      {value.length === 0 ? (
        <p className="text-small text-ink-muted">{emptyLabel}</p>
      ) : (
        <ul className="flex flex-wrap gap-2">
          {value.map((item) => (
            <li key={item}>
              <TagChip className="h-auto min-h-8 gap-1 py-0.5 pr-0.5 text-small">
                {labels[item] ?? item}
                <button
                  type="button"
                  onClick={() => toggle(item)}
                  aria-label={t("remove", { name: labels[item] ?? item })}
                  className="grid size-8 place-items-center rounded-chip hover:bg-[color-mix(in_srgb,var(--violet)_20%,transparent)]"
                >
                  <X aria-hidden className="size-4" />
                </button>
              </TagChip>
            </li>
          ))}
        </ul>
      )}
      <Popover.Root onOpenChange={(open) => !open && setSearch("")}>
        <Popover.Trigger asChild>
          <Button size="dense" className="self-start" aria-invalid={error ? true : undefined}>
            <Plus aria-hidden />
            {addLabel}
          </Button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            align="start"
            sideOffset={8}
            collisionPadding={16}
            className="z-50 flex max-h-[min(400px,var(--radix-popover-content-available-height))] w-[min(360px,calc(100vw-32px))] animate-fade-in flex-col gap-3 overflow-hidden rounded-control border border-border bg-surface-raised p-3 shadow-popover"
          >
            <Input
              type="search"
              aria-label={tForm("searchIn", { label })}
              placeholder={t("search")}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="h-11 lg:h-9"
            />
            <div className="-mx-3 overflow-y-auto px-3">
              {visible.length === 0 && <p className="py-2 text-small text-ink-muted">{tForm("noOptions")}</p>}
              {groups.map((group) => (
                <div key={group} role="group" aria-label={group || label} className="py-1">
                  {group && <p className="py-1 text-overline font-semibold text-ink-muted uppercase">{group}</p>}
                  {visible
                    .filter((option) => (option.group ?? "") === group)
                    .map((option) => (
                      <label
                        key={option.value}
                        className="flex min-h-11 cursor-pointer items-center gap-3 rounded-control px-2 hover:bg-surface-muted lg:min-h-9"
                      >
                        <input
                          type="checkbox"
                          className="size-5 shrink-0 accent-violet"
                          checked={value.includes(option.value)}
                          onChange={() => toggle(option.value)}
                        />
                        <span className="text-body">{option.label}</span>
                      </label>
                    ))}
                </div>
              ))}
            </div>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
    </fieldset>
  );
}
