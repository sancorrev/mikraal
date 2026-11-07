import createClient from "openapi-fetch";
import type { components, paths } from "@/generated/bff-web";

export type Schemas = components["schemas"];
export type Task = Schemas["Task"];
export type TaskStatus = Schemas["TaskStatus"];
export type TaskAction = Schemas["TaskAction"];
export type NamedRef = Schemas["NamedRef"];
export type Directory = Schemas["Directory"];
export type Session = Schemas["Session"];
export type Group = Session["groups"][number];
export type OwnerRef = Schemas["OwnerRef"];
export type LinkRef = Schemas["LinkRef"];
export type RecurrencePeriod = Schemas["RecurrencePeriod"];
export type TaskHistoryEntry = Schemas["TaskHistoryEntry"];

/** Same-origin client: Next.js rewrites /api/* to the BFF. */
export const api = createClient<paths>({
  baseUrl: typeof window === "undefined" ? "http://localhost" : window.location.origin,
  credentials: "same-origin",
  // The contract declares arrays as `style: form, explode: false` (status=a,b).
  querySerializer: { array: { style: "form", explode: false } },
});

/** CSRF guard required by the BFF on every write. */
export const writeHeader = { "X-Requested-With": "mikraal-web" } as const;
