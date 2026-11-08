/**
 * Deterministic API answers for screenshots, built from the synthetic seed (dev/seed/seed.json).
 * Synthetic people only: never real data.
 */
import { readFileSync } from "node:fs";

const seed = JSON.parse(readFileSync(new URL("../../dev/seed/seed.json", import.meta.url), "utf8"));

export const group = seed.groups[0];
export const groupId = group.id;
const fullName = (person) => `${person.firstName} ${person.lastName}`;
const person = (id) => {
  const found = seed.people.find((candidate) => candidate.id === id);
  return { id: found.id, name: fullName(found) };
};
const position = (name) => {
  const found = seed.positions.find((candidate) => candidate.name === name);
  return { id: found.id, name: found.name };
};
const event = (index) => ({ id: seed.events[index].id, name: seed.events[index].title });

const council = (index) => ({ id: seed.councils[index].id, name: seed.councils[index].title });

const lucia = person("0193a000-0000-7000-8000-000000000011");
const javier = person("0193a000-0000-7000-8000-000000000012");
const marta = person("0193a000-0000-7000-8000-000000000013");
const pablo = person("0193a000-0000-7000-8000-000000000014");
const tags = seed.taskTags.map((name, index) => ({ id: `0193a000-0000-7000-8000-0000000000${51 + index}`, name }));
const tag = (name) => tags.find((candidate) => candidate.name === name);

export const devPeople = seed.people.map((candidate) => ({
  id: candidate.id,
  name: fullName(candidate),
  isScouter: candidate.scouterOf.length > 0,
  positions: seed.positions.filter((p) => p.holders.includes(candidate.id)).map((p) => p.name),
}));

export const session = { person: lucia, groups: [group] };
export const capabilities = { canCreateTasks: true, canManageTags: true };
export const directory = {
  people: seed.people.filter((p) => p.scouterOf.length > 0).map((p) => ({ id: p.id, name: fullName(p) })),
  positions: seed.positions.map((p) => ({ id: p.id, name: p.name })),
  councils: seed.councils.map((c) => ({ id: c.id, name: c.title, date: c.date })),
  events: seed.events.map((e) => ({ id: e.id, name: e.title, date: e.date })),
};
export const tagList = { items: [...tags].sort((a, b) => a.name.localeCompare(b.name, "es")) };

const allActions = ["change_status", "edit", "delete", "end_recurrence"];
const base = {
  description: null,
  pauseReason: null,
  reminderDaysBefore: 2,
  links: [],
  recurrence: null,
  overdue: false,
  unassigned: false,
  createdBy: lucia,
  createdAt: "2026-10-28T17:42:00Z",
  updatedAt: "2026-11-04T19:10:00Z",
  version: 3,
  allowedActions: allActions,
};

const ownerOf = (ref, type) => ({ type, ...ref });

export const tasks = [
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a1",
    title: "Renovar el seguro del grupo",
    description: "Pedir la póliza nueva a la aseguradora y subir el justificante a tesorería.",
    status: "in_progress",
    owners: [ownerOf(position("Tesorería"), "position")],
    dueDate: "2026-11-05",
    overdue: true,
    tags: [tag("Tesorería")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a2",
    title: "Pedir presupuesto del albergue de Navidad",
    description: "Comparar al menos dos albergues cerca de la sierra. Plazas para 45 personas.",
    status: "paused",
    pauseReason: "Esperando presupuesto del albergue.",
    owners: [ownerOf(lucia, "person"), ownerOf(position("Coordinación de grupo"), "position")],
    dueDate: "2026-11-16",
    reminderDaysBefore: 3,
    tags: [tag("Campamento"), tag("Tesorería")],
    links: [
      { type: "event", ...event(1) },
      { type: "council", id: seed.councils[0].id, name: seed.councils[0].title },
    ],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a3",
    title: "Preparar el material de la salida de manada",
    status: "pending",
    owners: [ownerOf(position("Material"), "position"), ownerOf(pablo, "person")],
    dueDate: "2026-11-26",
    tags: [tag("Material"), tag("Campamento")],
    links: [{ type: "event", ...event(0) }],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a4",
    title: "Revisar las autorizaciones de las familias",
    status: "pending",
    owners: [],
    unassigned: true,
    dueDate: "2026-11-30",
    tags: [tag("Familias")],
    recurrence: { seriesId: "0193a000-0000-7000-8000-0000000000b1", period: "monthly", active: true },
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a5",
    title: "Enviar el acta del consejo de octubre",
    status: "finished",
    owners: [ownerOf(position("Secretaría"), "position")],
    dueDate: "2026-11-02",
    tags: [],
  },
  // From here on, only to fill the dashboard: overdue flags assume today is 2026-11-08.
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a6",
    title: "Revisar los cambios de datos de cuatro familias",
    status: "pending",
    owners: [ownerOf(position("Secretaría"), "position")],
    dueDate: "2026-11-10",
    tags: [tag("Familias")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a7",
    title: "Renovar dos certificados de delitos de naturaleza sexual",
    status: "pending",
    owners: [ownerOf(position("LOPIVI"), "position")],
    dueDate: "2026-11-30",
    tags: [],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a8",
    title: "Preparar la circular de diciembre",
    status: "pending",
    owners: [ownerOf(lucia, "person")],
    dueDate: "2026-11-27",
    tags: [tag("Familias")],
    links: [{ type: "council", ...council(0) }],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000a9",
    title: "Revisar seis justificantes de transferencia",
    status: "in_progress",
    owners: [ownerOf(position("Tesorería"), "position"), ownerOf(javier, "person")],
    dueDate: "2026-11-13",
    tags: [tag("Tesorería")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000aa",
    title: "Reservar el autobús del campamento",
    status: "in_progress",
    owners: [ownerOf(pablo, "person"), ownerOf(lucia, "person")],
    dueDate: "2026-11-20",
    tags: [tag("Campamento")],
    links: [{ type: "event", ...event(1) }],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000ab",
    title: "Preparar el cierre del trimestre",
    status: "in_progress",
    owners: [ownerOf(position("Tesorería"), "position")],
    dueDate: "2026-12-31",
    tags: [tag("Tesorería")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000ac",
    title: "Recoger las autorizaciones del campamento",
    status: "paused",
    pauseReason: "Faltan tres familias por firmar.",
    owners: [ownerOf(position("Coordinación de sección · Manada"), "position")],
    dueDate: "2026-12-11",
    tags: [tag("Campamento"), tag("Familias")],
    links: [{ type: "event", ...event(1) }],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000ad",
    title: "Reclamar los recibos pendientes de octubre",
    status: "paused",
    pauseReason: "Esperando respuesta de cuatro familias.",
    owners: [ownerOf(position("Tesorería"), "position"), ownerOf(lucia, "person")],
    dueDate: "2026-11-12",
    tags: [tag("Tesorería"), tag("Familias")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000ae",
    title: "Abrir el censo de la ronda",
    status: "finished",
    owners: [ownerOf(position("Secretaría"), "position"), ownerOf(lucia, "person")],
    dueDate: "2026-10-01",
    tags: [tag("Familias")],
  },
  {
    ...base,
    id: "0193a000-0000-7000-8000-0000000000af",
    title: "Cerrar la inscripción de la salida de manada",
    status: "finished",
    owners: [ownerOf(marta, "person")],
    dueDate: "2026-10-28",
    tags: [tag("Campamento")],
    links: [{ type: "event", ...event(0) }],
  },
];

/** The day the fixtures were written for: freeze the browser clock here for stable screenshots. */
export const today = "2026-11-08T12:40:00+01:00";

const myOwners = new Set([lucia.id, position("Coordinación de grupo").id]);

/** Same filters and order as the API (deadline, none last, then id). */
function listTasks(params) {
  const statuses = params.get("status")?.split(",") ?? [];
  const assignedTo = params.get("assignedTo");
  const [ownerType, ownerId] = assignedTo?.split(":") ?? [];
  const linked = params.get("linked");
  const tagId = params.get("tagId");
  const dueFrom = params.get("dueFrom");
  const dueTo = params.get("dueTo");
  return tasks
    .filter((task) => statuses.length === 0 || statuses.includes(task.status))
    .filter((task) => {
      if (!assignedTo) return true;
      if (assignedTo === "me") return task.owners.some((owner) => myOwners.has(owner.id));
      if (assignedTo === "unassigned") return task.owners.length === 0;
      return task.owners.some((owner) => owner.type === ownerType && owner.id === ownerId);
    })
    .filter((task) => !tagId || task.tags.some((candidate) => candidate.id === tagId))
    .filter((task) => !linked || task.links.some((link) => `${link.type}:${link.id}` === linked))
    .filter((task) => params.get("overdue") !== "true" || task.overdue)
    .filter((task) => !dueFrom || (task.dueDate && task.dueDate >= dueFrom))
    .filter((task) => !dueTo || (task.dueDate && task.dueDate <= dueTo))
    .sort((a, b) => (a.dueDate ?? "9999").localeCompare(b.dueDate ?? "9999") || a.id.localeCompare(b.id));
}

export const history = {
  items: [
    {
      id: "0193a000-0000-7000-8000-0000000000c3",
      at: "2026-11-04T19:10:00Z",
      actor: lucia,
      onBehalfOfPosition: position("Coordinación de grupo"),
      kind: "status_changed",
      changes: {
        status: { from: "in_progress", to: "paused" },
        pauseReason: { from: null, to: "Esperando presupuesto del albergue." },
      },
    },
    {
      id: "0193a000-0000-7000-8000-0000000000c2",
      at: "2026-11-01T10:05:00Z",
      actor: lucia,
      kind: "edited",
      changes: {
        dueDate: { from: "2026-11-30", to: "2026-11-16" },
        owners: { from: [{ name: lucia.name }], to: [{ name: lucia.name }, { name: "Coordinación de grupo" }] },
      },
    },
    {
      id: "0193a000-0000-7000-8000-0000000000c1",
      at: "2026-10-28T17:42:00Z",
      actor: lucia,
      kind: "created",
    },
  ],
  nextCursor: null,
};

/** Serves every /api call from the fixtures. */
export async function routeApi(page) {
  await page.route("**/api/**", (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(`/api/groups/${groupId}`, "");
    const method = route.request().method();
    const json = (body, status = 200) => route.fulfill({ status, json: body, headers: { ETag: '"3"' } });

    if (url.pathname === "/api/dev/people") return json({ items: devPeople });
    if (url.pathname.startsWith("/api/session"))
      return method === "DELETE" ? route.fulfill({ status: 204 }) : json(session);
    if (path === "/task-capabilities") return json(capabilities);
    if (path === "/directory") return json(directory);
    if (path === "/task-tags") return json(tagList);
    if (path === "/tasks" && method === "GET") {
      // A second page is empty. The unfiltered list claims one, so «Cargar más» shows up.
      if (url.searchParams.has("cursor")) return json({ items: [], nextCursor: null });
      const unfiltered = [...url.searchParams.keys()].length === 0;
      return json({ items: listTasks(url.searchParams), nextCursor: unfiltered ? "next" : null });
    }
    const detail = path.match(/^\/tasks\/([^/]+)(\/history)?$/);
    if (detail) {
      if (detail[2]) return json(history);
      return json(tasks.find((task) => task.id === detail[1]) ?? tasks[0]);
    }
    return route.fulfill({
      status: 404,
      contentType: "application/problem+json",
      body: '{"type":"about:blank","title":"Not found","status":404}',
    });
  });
}
