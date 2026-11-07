/** Launch locales (ARQ-05). Only those with a catalogue in `messages/` can be served. */
export const launchLocales = ["es-ES", "ca-ES", "ca-ES-valencia", "eu-ES"] as const;
export type Locale = (typeof launchLocales)[number];

export const defaultLocale: Locale = "es-ES";

/** Strings missing in a locale come from the next one in its chain. */
export const fallbackChain: Record<Locale, readonly Locale[]> = {
  "es-ES": ["es-ES"],
  "ca-ES": ["ca-ES", "es-ES"],
  "ca-ES-valencia": ["ca-ES-valencia", "ca-ES", "es-ES"],
  "eu-ES": ["eu-ES", "es-ES"],
};

/** Cookie holding the person's language until the profile service provides it. */
export const localeCookie = "MIKRAAL_LOCALE";

export function isLocale(value: string | undefined): value is Locale {
  return launchLocales.includes(value as Locale);
}
