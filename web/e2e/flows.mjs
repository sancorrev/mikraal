/**
 * Behaviour checks with deterministic API answers (e2e/fixtures.mjs). Needs only the dev server.
 */
import assert from "node:assert/strict";
import { BASE_URL, chromium, desktop, watchErrors } from "./lib.mjs";
import { groupId, routeApi, tasks, today } from "./fixtures.mjs";

const tasksUrl = `${BASE_URL}/groups/${groupId}/tasks`;
const [overdueTask, pausedTask, pendingTask, , finishedTask] = tasks;
const problem = (status, extra = {}) => ({
  status,
  contentType: "application/problem+json",
  body: JSON.stringify({ type: "about:blank", title: "Problem", status, ...extra }),
});

const browser = await chromium.launch();
const failures = [];

async function flow(name, fn) {
  const context = await browser.newContext({ ...desktop, locale: "es-ES" });
  const page = await context.newPage();
  const errors = watchErrors(page);
  await routeApi(page);
  try {
    await fn(page);
    assert.deepEqual(
      errors.filter((e) => !e.includes("status of 4")),
      [],
    );
    console.log(`ok    ${name}`);
  } catch (error) {
    failures.push(name);
    console.log(`FAIL  ${name}\n      ${error.message.split("\n").slice(0, 6).join(" ")}`);
  }
  await context.close();
}

function capture(page, method, pattern, reply) {
  const calls = [];
  page.route(pattern, (route) => {
    if (route.request().method() !== method) return route.fallback();
    calls.push(route.request());
    return reply(route);
  });
  return calls;
}

await flow("the dashboard counts the kraal's open tasks and groups them by status", async (page) => {
  await page.clock.setFixedTime(new Date(today));
  await page.goto(tasksUrl);
  await page.getByRole("heading", { level: 1, name: "Tareas del kraal" }).waitFor();
  const stats = page.getByRole("region", { name: "Resumen" });
  await stats.getByText("12 Tareas abiertas").waitFor();
  for (const text of ["3 Vencen esta semana", "3 Pausadas", "1 Vencidas"]) await stats.getByText(text).waitFor();
  await page.getByText("Actualizado hoy a las 12:40").waitFor();
  await page.getByRole("link", { name: "Tareas 12 abiertas" }).waitFor();
  const column = (name) => page.getByRole("region", { name: new RegExp(`^${name}`) });
  for (const [name, count] of [
    ["Pendiente", 5],
    ["En curso", 4],
    ["Pausada", 3],
    ["Terminada", 3],
  ]) {
    assert.equal(await column(name).getByRole("listitem").count(), count, name);
  }
  await column("En curso").getByText("Vencida: jue 5 nov").waitFor();
  await column("Pausada").getByText("Faltan tres familias por firmar.").waitFor();
  assert.equal(await column("Pendiente").getByRole("link", { name: "Añadir tarea" }).count(), 1);
  assert.equal(await column("Terminada").getByRole("link", { name: "Añadir tarea" }).count(), 0);
});

await flow("«Mis tareas», menus and the list view live in the URL", async (page) => {
  const requests = [];
  page.on("request", (request) => {
    if (request.url().includes("/tasks?")) requests.push(new URL(request.url()).searchParams);
  });
  await page.goto(tasksUrl);
  await page.getByRole("heading", { level: 1, name: "Tareas del kraal" }).waitFor();
  await page.getByRole("button", { name: "Mis tareas" }).click();
  await page.waitForURL(/owner=me/);
  await page.getByRole("button", { name: /^Etiqueta: todas/ }).click();
  await page.getByRole("menuitemradio", { name: "Tesorería" }).click();
  await page.waitForURL(/tag=/);
  await page.getByRole("button", { name: "Etiqueta: Tesorería" }).waitFor();
  await page.getByRole("tab", { name: "Lista" }).click();
  await page.waitForURL(/view=list/);
  await page.reload();
  await page.getByRole("tab", { name: "Lista", selected: true }).waitFor();
  assert.equal(await page.getByRole("button", { name: "Mis tareas" }).getAttribute("aria-pressed"), "true");
  const rows = page.getByRole("table").getByRole("row");
  await rows.nth(1).waitFor();
  assert.deepEqual(await rows.locator("td:first-child").allInnerTexts(), [
    "Reclamar los recibos pendientes de octubre",
    "Pedir presupuesto del albergue de Navidad",
  ]);
  assert.ok(requests.some((params) => params.get("assignedTo") === "me" && params.get("tagId")));
  const summary = (params) => params.get("status") === "pending,in_progress,paused" && !params.has("assignedTo");
  assert.ok(requests.some(summary), "the counts ignore the filters");
  await page.getByRole("button", { name: "Quitar filtros" }).click();
  await page.waitForURL((url) => !url.searchParams.has("tag") && url.searchParams.get("view") === "list");
});

await flow("picked statuses are the board's columns", async (page) => {
  await page.goto(`${tasksUrl}?status=pending,paused`);
  await page.getByRole("heading", { level: 1, name: "Tareas del kraal" }).waitFor();
  const more = page.getByRole("button", { name: /^Más filtros/ });
  assert.equal(await more.getAttribute("aria-expanded"), "true", "opens when a hidden filter is on");
  assert.equal(await more.getAttribute("aria-label"), "Más filtros, 1 activo");
  const columns = page.getByRole("region", { name: /^(Pendiente|En curso|Pausada|Terminada)\b/ });
  await page.getByRole("region", { name: /^Pausada/ }).waitFor();
  assert.equal(await columns.count(), 2);
  await page.getByRole("region", { name: /^Pendiente/ }).waitFor();
});

await flow("pause asks for a reason and sends it with If-Match", async (page) => {
  const calls = capture(page, "POST", /status-changes$/, (route) =>
    route.fulfill({ json: { ...pendingTask, status: "paused", pauseReason: "Esperando presupuesto." } }),
  );
  await page.goto(`${tasksUrl}/${pendingTask.id}`);
  await page.getByRole("button", { name: "Pausar tarea" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Pausar tarea" }).click();
  await dialog.getByText("Escribe el motivo de la pausa.").waitFor();
  assert.equal(calls.length, 0, "no request without a reason");
  await dialog.getByLabel("Motivo").fill("Esperando presupuesto.");
  await dialog.getByRole("button", { name: "Pausar tarea" }).click();
  await page.getByRole("dialog").waitFor({ state: "detached" });
  assert.deepEqual(calls[0].postDataJSON(), { status: "paused", pauseReason: "Esperando presupuesto." });
  assert.equal(calls[0].headers()["if-match"], '"3"');
});

await flow("a finished task can only be reopened, to «En curso»", async (page) => {
  const calls = capture(page, "POST", /status-changes$/, (route) =>
    route.fulfill({ json: { ...finishedTask, status: "in_progress" } }),
  );
  await page.goto(`${tasksUrl}/${finishedTask.id}`);
  const buttons = page.getByRole("region", { name: "Estado" }).getByRole("button");
  await buttons.first().waitFor();
  assert.deepEqual(await buttons.allInnerTexts(), ["Reabrir tarea"]);
  await buttons.first().click();
  await page.getByText("Estado cambiado a «En curso».").waitFor({ state: "attached" });
  assert.equal(calls[0].postDataJSON().status, "in_progress");
});

await flow("only allowed actions are shown", async (page) => {
  await page.route(/\/api\/.*\/tasks\/[^/]+$/, (route) =>
    route.request().method() === "GET"
      ? route.fulfill({ json: { ...overdueTask, allowedActions: [] } })
      : route.fallback(),
  );
  await page.goto(`${tasksUrl}/${overdueTask.id}`);
  await page.getByRole("heading", { level: 2, name: "Historial" }).waitFor();
  assert.equal(await page.getByRole("button", { name: /Editar|Eliminar|Terminar|Pausar/ }).count(), 0);
  assert.equal(await page.getByRole("link", { name: "Editar tarea" }).count(), 0);
  await page.getByText("Vencida").first().waitFor();
});

await flow("edit sends a merge patch with If-Match and handles 412", async (page) => {
  const calls = capture(page, "PATCH", /\/tasks\/[^/]+$/, (route) => route.fulfill(problem(412)));
  await page.goto(`${tasksUrl}/${pausedTask.id}/edit`);
  await page.getByLabel("Título").fill("Pedir presupuesto de dos albergues");
  await page.getByRole("button", { name: "Guardar cambios" }).click();
  await page.getByText("Alguien ha cambiado esta tarea mientras la editabas", { exact: false }).waitFor();
  assert.deepEqual(calls[0].postDataJSON(), { title: "Pedir presupuesto de dos albergues" });
  assert.equal(calls[0].headers()["if-match"], '"3"');
  assert.match(calls[0].headers()["content-type"], /application\/merge-patch\+json/);
  await page.getByRole("button", { name: "Recargar tarea" }).click();
  await page.getByRole("button", { name: "Recargar tarea" }).waitFor({ state: "detached" });
  assert.equal(await page.getByLabel("Título").inputValue(), pausedTask.title);
});

await flow("validation errors from problem+json appear next to the field", async (page) => {
  capture(page, "POST", /\/tasks$/, (route) =>
    route.fulfill(problem(400, { errors: [{ field: "title", code: "too_long" }] })),
  );
  await page.goto(`${tasksUrl}/new`);
  await page.getByLabel("Título").fill("Renovar el seguro");
  await page.getByRole("button", { name: "Crear tarea" }).last().click();
  await page.getByText("Es demasiado largo.").waitFor();
  assert.equal(await page.getByLabel("Título").getAttribute("aria-invalid"), "true");
  await page.getByText("Revisa los campos marcados.").waitFor();
});

await flow("create without a deadline sends no reminder or recurrence", async (page) => {
  const calls = capture(page, "POST", /\/tasks$/, (route) => route.fulfill({ status: 201, json: pendingTask }));
  await page.goto(`${tasksUrl}/new`);
  assert.equal(await page.getByLabel("Repetición").isDisabled(), true);
  await page.getByLabel("Título").fill("Comprar pañuelos");
  await page.getByRole("button", { name: "Añadir etiqueta" }).click();
  await page.getByRole("checkbox", { name: "Material" }).check();
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "Crear tarea" }).last().click();
  await page.waitForURL(`**/tasks/${pendingTask.id}`);
  const body = calls[0].postDataJSON();
  assert.equal(body.reminderDaysBefore, undefined);
  assert.equal(body.recurrence, undefined);
  assert.equal(body.tagIds.length, 1);
});

await flow("delete confirms that it is final", async (page) => {
  const calls = capture(page, "DELETE", /\/tasks\/[^/]+$/, (route) => route.fulfill({ status: 204 }));
  await page.goto(`${tasksUrl}/${pendingTask.id}`);
  await page.getByRole("button", { name: "Eliminar tarea" }).click();
  await page.getByRole("dialog").getByText("se eliminará para siempre", { exact: false }).waitFor();
  await page.getByRole("dialog").getByRole("button", { name: "Eliminar tarea" }).click();
  await page.waitForURL(`**/groups/${groupId}/tasks`);
  assert.equal(calls[0].headers()["if-match"], '"3"');
});

await flow("removing a tag warns that it leaves every task", async (page) => {
  const calls = capture(page, "DELETE", /task-tags\/[^/]+$/, (route) => route.fulfill({ status: 204 }));
  await page.goto(`${BASE_URL}/groups/${groupId}/tags`);
  await page.getByRole("button", { name: "Quitar Material" }).click();
  await page.getByRole("dialog").getByText("todas las tareas", { exact: false }).waitFor();
  await page.getByRole("dialog").getByRole("button", { name: "Quitar etiqueta" }).click();
  await page.getByRole("dialog").waitFor({ state: "detached" });
  assert.equal(calls.length, 1);
});

await flow("a duplicate tag name is explained", async (page) => {
  capture(page, "POST", /task-tags$/, (route) => route.fulfill(problem(409)));
  await page.goto(`${BASE_URL}/groups/${groupId}/tags`);
  await page.getByLabel("Nueva etiqueta").fill("Material");
  await page.getByRole("button", { name: "Crear etiqueta" }).click();
  await page.getByText("Ya existe una etiqueta con ese nombre.").waitFor();
});

await flow("families see that tasks are for the kraal", async (page) => {
  await page.route(/\/api\/session$/, (route) =>
    route.fulfill({
      json: { person: { id: "0193a000-0000-7000-8000-000000000018", name: "Ana López Díaz" }, groups: [] },
    }),
  );
  await page.goto(`${tasksUrl}`);
  await page.getByRole("heading", { name: "Las tareas son para el kraal" }).waitFor();
});

await flow("without a session the app goes to the login", async (page) => {
  await page.route(/\/api\/session$/, (route) => route.fulfill(problem(401)));
  await page.goto(tasksUrl);
  await page.waitForURL("**/login");
});

await browser.close();
if (failures.length) {
  console.log(`\n${failures.length} failed`);
  process.exit(1);
}
console.log("\nall passed");
