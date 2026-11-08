const connectors = new Set(["de", "del", "la", "las", "el", "los", "y", "e", "i", "d'", "en"]);

/** Initials for avatars: «Lucía Martín Ortega» → «LM», «Coordinación de grupo» → «CG», «Tesorería» → «TE». */
export function initials(name: string): string {
  const words = name.split(/[\s·]+/).filter((word) => word && !connectors.has(word.toLocaleLowerCase()));
  const letters =
    words.length === 1 ? Array.from(words[0] ?? "").slice(0, 2) : words.slice(0, 2).map((word) => Array.from(word)[0]);
  return letters.join("").toLocaleUpperCase();
}

const avatarTones = ["violet", "soft", "neutral"] as const;
export type AvatarTone = (typeof avatarTones)[number];

/** A stable background for an avatar, so the same person or position always looks the same. */
export function avatarTone(id: string): AvatarTone {
  let hash = 0;
  for (const char of id) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return avatarTones[hash % avatarTones.length] ?? "violet";
}
