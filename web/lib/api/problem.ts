import type { Schemas } from "./client";

export type FieldError = { field: string; code: string };

/** An HTTP error from the BFF, with its problem details (RFC 9457) when present. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem?: Partial<Schemas["ValidationProblem"]>,
  ) {
    super(problem?.title ?? `HTTP ${status}`);
    this.name = "ApiError";
  }

  get fieldErrors(): FieldError[] {
    return Array.isArray(this.problem?.errors) ? this.problem.errors : [];
  }
}

type Result<T> = { data?: T; error?: unknown; response: Response };

/** Returns the data of an openapi-fetch result or throws an ApiError. */
export function unwrap<T>(result: Result<T>): T {
  if (result.response.ok) return result.data as T;
  const problem = typeof result.error === "object" && result.error !== null ? result.error : undefined;
  throw new ApiError(result.response.status, problem as ApiError["problem"]);
}

export function statusOf(error: unknown): number | undefined {
  return error instanceof ApiError ? error.status : undefined;
}

/** First segment of a field path: `owners[0].id` → `owners`. */
export function fieldKey(field: string): string {
  return field.replace(/^\//, "").split(/[.[/]/)[0] ?? field;
}
