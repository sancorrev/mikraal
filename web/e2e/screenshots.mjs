/**
 * Screenshots for visual review, with every API call served from e2e/fixtures.mjs.
 * Needs only the dev server (`pnpm dev` or `pnpm dev:mock`). Output: docs/screenshots/.
 */
import { mkdirSync } from "node:fs";
import { BASE_URL, chromium, desktop, mobile } from "./lib.mjs";
import { groupId, routeApi, today } from "./fixtures.mjs";

const out = new URL("../docs/screenshots/", import.meta.url).pathname;
mkdirSync(out, { recursive: true });

const tasksUrl = `${BASE_URL}/groups/${groupId}/tasks`;
const shots = [
  ["login", `${BASE_URL}/login`],
  ["task-list", tasksUrl],
  ["task-list-table", `${tasksUrl}?view=list`],
  ["task-list-filtered", `${tasksUrl}?owner=me&status=pending,paused`],
  ["task-detail", `${tasksUrl}/0193a000-0000-7000-8000-0000000000a2`],
  ["task-create", `${tasksUrl}/new`],
  ["task-edit", `${tasksUrl}/0193a000-0000-7000-8000-0000000000a2/edit`],
  ["tags", `${BASE_URL}/groups/${groupId}/tags`],
];

async function settle(page) {
  await page.waitForLoadState("networkidle");
  await page.waitForFunction(() => !document.body.innerText.includes("Cargando…"));
  await page.evaluate(() => document.fonts.ready);
}

const browser = await chromium.launch();
for (const [size, options] of [
  ["desktop", desktop],
  ["mobile", mobile],
]) {
  for (const scheme of ["light", "dark"]) {
    const context = await browser.newContext({
      ...options,
      locale: "es-ES",
      colorScheme: scheme,
      reducedMotion: "reduce",
    });
    const page = await context.newPage();
    await page.clock.setFixedTime(new Date(today));
    await routeApi(page);
    const suffix = scheme === "dark" ? "-dark" : "";
    const list = scheme === "dark" ? shots.filter(([name]) => ["task-list", "task-detail"].includes(name)) : shots;

    for (const [name, url] of list) {
      await page.goto(url);
      await settle(page);
      await page.screenshot({ path: `${out}${size}-${name}${suffix}.png`, fullPage: size === "desktop" });
    }

    if (scheme === "light") {
      await page.goto(`${tasksUrl}/0193a000-0000-7000-8000-0000000000a3`);
      await settle(page);
      await page.getByRole("button", { name: "Pausar tarea" }).click();
      await page.getByRole("dialog").waitFor();
      await page.getByRole("button", { name: "Pausar tarea" }).last().click();
      await page.screenshot({ path: `${out}${size}-pause-dialog.png` });

      await page.goto(`${tasksUrl}/new`);
      await settle(page);
      await page.getByLabel("Título").fill("Comprar los pañuelos de la ronda");
      await page.getByRole("button", { name: "Añadir responsable" }).click();
      await page.getByLabel("Buscar en Responsables").fill("mar");
      await page.screenshot({ path: `${out}${size}-owner-picker.png` });

      await page.route(/\/api\/session$/, (route) =>
        route.fulfill({
          json: { person: { id: "0193a000-0000-7000-8000-000000000018", name: "Ana López Díaz" }, groups: [] },
        }),
      );
      await page.goto(`${BASE_URL}/`);
      await settle(page);
      await page.screenshot({ path: `${out}${size}-family.png` });
    }
    await context.close();
  }
}
await browser.close();
console.log(`screenshots in ${out}`);
