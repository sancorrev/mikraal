/** Date formats from visual-identity.md: «lun 2 nov» in lists, «2 de noviembre de 2026» in detail, 24 h. */

const FALLBACK_TIME_ZONE = "Europe/Madrid";

/** Deadlines are local dates (YYYY-MM-DD): format them in UTC so the day never shifts. */
function parseLocalDate(value: string): Date {
  const [year = 0, month = 1, day = 1] = value.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

export function safeTimeZone(timeZone: string | undefined): string {
  if (!timeZone) return FALLBACK_TIME_ZONE;
  try {
    new Intl.DateTimeFormat("es", { timeZone });
    return timeZone;
  } catch {
    return FALLBACK_TIME_ZONE;
  }
}

export function formatShortDate(value: string, locale: string): string {
  const parts = new Intl.DateTimeFormat(locale, {
    weekday: "short",
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  }).formatToParts(parseLocalDate(value));
  return parts
    .filter((part) => part.type !== "literal")
    .map((part) => part.value.replace(/\.$/, ""))
    .join(" ");
}

export function formatLongDate(value: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: "long", timeZone: "UTC" }).format(parseLocalDate(value));
}

export function formatDateTime(value: string, locale: string, timeZone: string): string {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "long",
    timeStyle: "short",
    hourCycle: "h23",
    timeZone: safeTimeZone(timeZone),
  }).format(new Date(value));
}

/** The calendar day of an instant, in the group's time zone. */
export function formatInstantDate(value: string, locale: string, timeZone: string): string {
  return new Intl.DateTimeFormat(locale, { dateStyle: "long", timeZone: safeTimeZone(timeZone) }).format(
    new Date(value),
  );
}

/** Today (YYYY-MM-DD) in the group's time zone, for comparing with deadlines. */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: safeTimeZone(timeZone),
  }).formatToParts(now);
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
}

/** A local date (YYYY-MM-DD) moved by a number of days. */
export function addDays(value: string, days: number): string {
  const date = parseLocalDate(value);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

/** 24 h time of an instant in the group's time zone: «12:40». */
export function formatTime(value: number | string | Date, locale: string, timeZone: string): string {
  return new Intl.DateTimeFormat(locale, {
    timeStyle: "short",
    hourCycle: "h23",
    timeZone: safeTimeZone(timeZone),
  }).format(new Date(value));
}
