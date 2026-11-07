import { cookies } from "next/headers";
import { getRequestConfig } from "next-intl/server";
import { defaultLocale, fallbackChain, isLocale, localeCookie, type Locale } from "./config";

type Messages = Record<string, unknown>;

/** Add an entry here when a new catalogue is translated. */
const catalogues: Partial<Record<Locale, () => Promise<Messages>>> = {
  "es-ES": () => import("../messages/es-ES.json").then((m) => m.default),
};

function isObject(value: unknown): value is Messages {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function merge(base: Messages, override: Messages): Messages {
  const result: Messages = { ...base };
  for (const [key, value] of Object.entries(override)) {
    const current = result[key];
    result[key] = isObject(current) && isObject(value) ? merge(current, value) : value;
  }
  return result;
}

async function loadMessages(locale: Locale): Promise<Messages> {
  const chain = [...fallbackChain[locale]].reverse();
  let messages: Messages = {};
  for (const step of chain) {
    const load = catalogues[step];
    if (load) messages = merge(messages, await load());
  }
  return messages;
}

export default getRequestConfig(async () => {
  const requested = (await cookies()).get(localeCookie)?.value;
  const locale: Locale = isLocale(requested) && catalogues[requested] ? requested : defaultLocale;
  return { locale, messages: await loadMessages(locale) };
});
