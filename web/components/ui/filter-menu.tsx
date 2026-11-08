"use client";

import { ChevronDown } from "lucide-react";
import { useTranslations } from "next-intl";
import { Fragment } from "react";
import { cn } from "@/lib/utils";
import { Button } from "./button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./dropdown-menu";
import type { Option } from "./multi-select";

type FilterMenuProps = {
  /** «Etiqueta»: the trigger reads «Etiqueta: todas». */
  label: string;
  /** Short value shown on the trigger when nothing is chosen («todas»). */
  anyValue: string;
  /** First option of the menu («Cualquier etiqueta»). */
  anyOption: string;
  options: Option[];
  /** "" means any. */
  value: string;
  onChange: (value: string) => void;
};

/** Compact single-choice filter for toolbars: a «Label: value» button that opens a menu. */
export function FilterMenu({ label, anyValue, anyOption, options, value, onChange }: FilterMenuProps) {
  const t = useTranslations("common");
  const selected = value ? options.find((option) => option.value === value) : undefined;
  const groups = [...new Set(options.map((option) => option.group ?? ""))];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          size="dense"
          className={cn("max-w-full min-w-0", selected && "border-violet bg-violet-light hover:bg-violet-light")}
        >
          <span className="truncate">{t("filterValue", { label, value: selected?.label ?? anyValue })}</span>
          <ChevronDown aria-hidden />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="max-w-[min(360px,calc(100vw-32px))]">
        <DropdownMenuRadioGroup value={value} onValueChange={onChange} aria-label={label}>
          <DropdownMenuRadioItem value="">{anyOption}</DropdownMenuRadioItem>
        </DropdownMenuRadioGroup>
        {groups.map((group) => (
          <Fragment key={group}>
            <DropdownMenuSeparator />
            {group && <DropdownMenuLabel>{group}</DropdownMenuLabel>}
            <DropdownMenuRadioGroup value={value} onValueChange={onChange} aria-label={group || label}>
              {options
                .filter((option) => (option.group ?? "") === group)
                .map((option) => (
                  <DropdownMenuRadioItem key={option.value} value={option.value}>
                    {option.label}
                  </DropdownMenuRadioItem>
                ))}
            </DropdownMenuRadioGroup>
          </Fragment>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
