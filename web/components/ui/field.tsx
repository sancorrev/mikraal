import { CircleAlert } from "lucide-react";
import { useId, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export type ControlProps = {
  id: string;
  "aria-describedby"?: string;
  "aria-invalid"?: true;
};

type FieldProps = {
  label: ReactNode;
  hint?: ReactNode;
  error?: ReactNode;
  className?: string;
  children: (control: ControlProps) => ReactNode;
};

/** Label, hint and error wired to one control (visual-identity.md: black message, orange icon). */
export function Field({ label, hint, error, className, children }: FieldProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;
  const control: ControlProps = { id, "aria-describedby": describedBy, ...(error ? { "aria-invalid": true } : {}) };

  return (
    <div className={cn("flex min-w-0 flex-col gap-2", className)}>
      <label htmlFor={id} className="text-body font-semibold">
        {label}
      </label>
      {hint && (
        <p id={hintId} className="-mt-1 text-small text-ink-muted">
          {hint}
        </p>
      )}
      {children(control)}
      {error && <FieldError id={errorId}>{error}</FieldError>}
    </div>
  );
}

export function FieldError({ id, children }: { id?: string; children: ReactNode }) {
  return (
    <p id={id} className="flex items-start gap-1.5 text-small text-ink">
      <CircleAlert aria-hidden className="mt-px size-4 text-orange" />
      <span>{children}</span>
    </p>
  );
}
