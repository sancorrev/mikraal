/**
 * Smoke test against the Prism mock (`pnpm mock` + `pnpm dev:mock`).
 * Prism answers with random data (sometimes empty lists), so the people list, the session and the
 * capabilities are pinned from the seed: everything else comes from the mock.
 */
import assert from "node:assert/strict";
import { capabilities, devPeople, groupId, session } from "./fixtures.mjs";
import { BASE_URL, axe, chromium, desktop, mobile, watchErrors } from "./lib.mjs";

async function pinFromSeed(page) {
  await page.route(/\/api\/dev\/people$/, (route) => route.fulfill({ json: { items: devPeople } }));
  await page.route(/\/task-capabilities$/, (route) => route.fulfill({ json: capabilities }));
  await page.route(/\/api\/session(\/dev-login)?$/, (route) =>
    route.request().method() === "DELETE" ? route.fulfill({ status: 204 }) : route.fulfill({ json: session }),
  );
}

const failures = [];
async function step(name, fn) {
  try {
    await fn();
    console.log(`ok    ${name}`);
  } catch (error) {
    failures.push(name);
    console.log(`FAIL  ${name}\n      ${error.message.split("\n")[0]}`);
  }
}

async function checkAxe(page, label) {
  const violations = await axe(page);
  assert.equal(violations.length, 0, `${label}: ${JSON.stringify(violations)}`);
}

const browser = await chromium.launch();
for (const [name, options] of [
  ["desktop", desktop],
  ["mobile", mobile],
]) {
  const context = await browser.newContext({ ...options, locale: "es-ES" });
  const page = await context.newPage();
  const errors = watchErrors(page);
  await pinFromSeed(page);

  await step(`${name}: login lists synthetic people`, async () => {
    await page.goto(`${BASE_URL}/login`);
    await page.getByRole("heading", { level: 1, name: "Entrar en mikraal" }).waitFor();
    await page
      .getByRole("button", { name: /Entrar como/ })
      .first()
      .waitFor();
    await checkAxe(page, "login");
  });

  await step(`${name}: login leads to the task dashboard`, async () => {
    await page
      .getByRole("button", { name: /Entrar como/ })
      .first()
      .click();
    await page.waitForURL(`**/groups/${groupId}/tasks`);
    await page.getByRole("heading", { level: 1, name: "Tareas del kraal" }).waitFor();
    await page.getByRole("tab", { name: "Tablero", selected: true }).waitFor();
    await page.waitForFunction(() => !document.body.innerText.includes("Cargando…"));
    await checkAxe(page, "board");
  });

  await step(`${name}: «Mis tareas» and the list view go into the URL`, async () => {
    await page.getByRole("button", { name: "Mis tareas" }).click();
    await page.waitForURL(/owner=me/);
    assert.equal(await page.getByRole("button", { name: "Mis tareas" }).getAttribute("aria-pressed"), "true");
    await page.getByRole("tab", { name: "Lista" }).click();
    await page.waitForURL(/view=list/);
    await page.waitForFunction(() => !document.body.innerText.includes("Cargando…"));
    await checkAxe(page, "list");
  });

  await step(`${name}: filter menus and «Más filtros» open`, async () => {
    await page.getByRole("button", { name: /^Etiqueta:/ }).click();
    await page.getByRole("menuitemradio", { name: "Cualquier etiqueta" }).waitFor();
    await checkAxe(page, "tag menu");
    await page.keyboard.press("Escape");
    await page.getByRole("button", { name: /^Más filtros/ }).click();
    await page.getByLabel("Responsable").waitFor();
    await checkAxe(page, "more filters");
  });

  await step(`${name}: task detail renders`, async () => {
    await page.goto(`${BASE_URL}/groups/${groupId}/tasks/0193a000-0000-7000-8000-0000000000a1`);
    await page.getByRole("heading", { level: 2, name: "Historial" }).waitFor();
    await page.waitForFunction(() => !document.body.innerText.includes("Cargando…"));
    await checkAxe(page, "detail");
  });

  await step(`${name}: create form submits and opens the new task`, async () => {
    await page.goto(`${BASE_URL}/groups/${groupId}/tasks/new`);
    await page.getByRole("heading", { level: 1, name: "Crear tarea" }).waitFor();
    await checkAxe(page, "form");
    await page.getByRole("button", { name: "Crear tarea" }).last().click();
    await page.getByText("Escribe un título.").waitFor();
    await page.getByLabel("Título").fill("Renovar el seguro del grupo");
    await page.getByLabel(/Fecha límite/).fill("2026-11-20");
    const request = page.waitForRequest((req) => req.method() === "POST" && req.url().endsWith("/tasks"));
    await page.getByRole("button", { name: "Crear tarea" }).last().click();
    const body = (await request).postDataJSON();
    assert.match(body.id, /^[0-9a-f-]{14}7/, "client-generated UUIDv7");
    assert.equal(body.reminderDaysBefore, 2, "default reminder");
    assert.equal((await request).headers()["x-requested-with"], "mikraal-web");
    await page.waitForURL(/\/tasks\/[0-9a-f-]{36}$/);
  });

  await step(`${name}: tag catalogue renders`, async () => {
    await page.goto(`${BASE_URL}/groups/${groupId}/tags`);
    await page.getByRole("heading", { level: 1 }).waitFor();
    await page.waitForFunction(() => !document.body.innerText.includes("Cargando…"));
    await checkAxe(page, "tags");
  });

  await step(`${name}: no runtime errors`, async () => {
    assert.deepEqual(errors, []);
  });
  await context.close();
}
await browser.close();

if (failures.length) {
  console.log(`\n${failures.length} failed`);
  process.exit(1);
}
console.log("\nall passed");
