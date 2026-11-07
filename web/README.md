# web

The mikraal web app (Next.js App Router, React, TypeScript strict, Tailwind, shadcn/ui-style components on Radix). It talks only to `bff/web` through `contracts/openapi/bff-web.yaml`. First module: kraal tasks.

## Run

Requirements: Node 22 and pnpm 10. Ports follow `docs/dev-conventions.md`.

```sh
pnpm install
```

Against the Prism mock of the contract (no BFF needed):

```sh
pnpm mock       # terminal 1: Prism on :4010, random data from the contract
pnpm dev:mock   # terminal 2: web on :3000 with BFF_URL=http://localhost:4010
```

Prism never sets the session cookie, so `dev:mock` sets `MIKRAAL_MOCK_SESSION=1` and `proxy.ts` adds a fake `MIKRAAL_SESSION` cookie to `/api/*` requests that have none. Data is random: names are Latin filler, and a page may show a family session or empty lists.

Against the real BFF (`bff/web` on :8080):

```sh
pnpm dev        # BFF_URL defaults to http://localhost:8080
```

Open http://localhost:3000/login and pick a synthetic person.

## Scripts

| Script                                        | What it does                                                                                                                                                         |
| :-------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `generate`                                    | Generates `generated/bff-web.ts` from the contract (runs before `dev`, `build` and `typecheck`)                                                                      |
| `dev`, `dev:mock`                             | Dev server against the BFF or the mock                                                                                                                               |
| `mock`                                        | Prism mock of `bff-web.yaml` on :4010                                                                                                                                |
| `build`, `start`                              | Production build and server                                                                                                                                          |
| `lint`, `typecheck`, `format`, `format:check` | ESLint (Next config), `tsc --noEmit`, Prettier                                                                                                                       |
| `e2e`                                         | Smoke test against the mock: login, list, detail, create form, tags, plus axe (WCAG 2.2 AA). Needs `mock` and `dev:mock` running                                     |
| `e2e:flows`                                   | Behaviour checks with fixed API answers: pause reason, reopen, `allowedActions`, `If-Match`, 412, validation errors, delete, tags, families, 401. Needs a dev server |
| `screenshots`                                 | Desktop 1440×900 and mobile 390×844, light and dark, into `docs/screenshots/`. Needs a dev server                                                                    |

The e2e scripts use the Playwright and Chromium installed on the machine (`require('playwright')` through `NODE_PATH`); they are not project dependencies. Fixtures come from `dev/seed/seed.json` (synthetic people only).

## Structure

```
app/                 routes: /login, / (group choice or family notice), /groups/[groupId]/tasks[/new|/[taskId][/edit]], /groups/[groupId]/tags
components/ui/       primitives: button, input, select, field, chip, notice, dialog, multi-select
components/shell/    app shell: side navigation (desktop), top bar and bottom navigation (mobile)
components/tasks/    task list, filters, detail, status actions, history, form
components/tags/     tag catalogue
lib/api/             typed client (openapi-fetch), queries and mutations (TanStack Query), problem details
lib/                 dates, status transitions, cn()
i18n/, messages/     next-intl setup and catalogues
e2e/                 smoke test, flows, screenshots, fixtures
```

Filters live in the URL (`?owner=all&status=pending,paused&tag=…`), so a view can be shared.

## i18n

Every UI text is in `messages/es-ES.json` (ARQ-01, 11). Locales are in `i18n/config.ts`: `es-ES`, `ca-ES`, `ca-ES-valencia`, `eu-ES`, with the fallback valencian → catalan → spanish. To add a language, add `messages/<locale>.json` (it may be partial) and register it in `catalogues` in `i18n/request.ts`. Until profiles carry a language, the `MIKRAAL_LOCALE` cookie selects it. Only `es-ES` exists today. Dates use `Intl` in the active locale: «lun 2 nov» in lists, «2 de noviembre de 2026» in detail, 24 h times in the group's time zone.

## Design

Tokens from `mikraal-memory/visual-identity.md` are CSS variables in `app/globals.css` (light and dark via `prefers-color-scheme`), exposed to Tailwind with `@theme`: colours, type scale (`text-h1`, `text-caption`…), radii (`rounded-chip`, `rounded-control`, `rounded-card`, `rounded-sheet`), motion easings. Fonts via `next/font/google`. States always carry a word (`StateChip`).

## Deviations and open questions

- shadcn/ui components are written by hand on Radix, following shadcn conventions (`components.json` is present): the shadcn registry was not reachable from the build environment.
- The logo is a text wordmark in Bricolage Grotesque Regular: the outlined logo and the kraal circle symbol are not in the repo yet.
- The tasks block inside councils, events and positions is not built: those pages do not exist yet.
- The contract does not list validation `code` values. The form maps `required`, `too_long`, `too_many`, `out_of_range`, `requires_due_date`, `not_found` and `invalid`, and shows a generic message for any other code.
- History `changes` values are free-form; the web shows names, status labels and dates where it can recognise them.
- Recurrence cannot be changed when editing (`UpdateTask` has no `recurrence`); the form shows it read-only.
- Date fields are native inputs: their format follows the browser language.
- The mobile bottom navigation has one or two destinations (Tareas, Etiquetas); the identity asks for 4–5, which will come with more modules.
