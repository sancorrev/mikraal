# mikraal · Product monorepo

The mikraal product code: SaaS for Scout Groups in Spain (positions, secretary, treasury, activities, families app). The repo is empty for now; its structure is set in the Phase 0 architecture workshops (see below).

## Shared memory
If `../mikraal-memory` exists, read `../mikraal-memory/README.md` at the start and consult only the files
the task needs. The decisions in `../mikraal-memory/decisions.md` are closed: do not contradict them; if something in this repo conflicts with them, flag it. For details specific to this repo, this file wins.

Before writing code, check `../mikraal-memory/conventions/code-style.md`: rules that already follow from ARQ-01 (RLS, UUIDv7, amounts in cents, logs without personal data…).

## Commands
Pending: defined once the technical skeleton exists (29 Jan 2027).
- Install: `<command>`
- Tests: `<command>`
- Lint / format: `<command>`
- Run locally: `<command>`

## Architecture
Provisional layout (see `../mikraal-memory/repo-map.md`); workshops 2, 7 and 8 confirm it.

- `contracts/openapi/`: OpenAPI contracts, written first. Clients and server interfaces are generated from them; never edit generated code.
- `services/activities/`: Java 21 + Spring Boot, Gradle (Kotlin DSL), PostgreSQL with RLS, Flyway. Holds the `tasks` module.
- `bff/web/`: Spring Boot BFF for the web. Resolves names of people, positions, councils and events; the web only talks to it.
- `web/`: Next.js + React + TypeScript + shadcn/ui, pnpm.

Login is a development login with synthetic users for now. Permissions go through OpenFGA (ARQ-04).

## Watch out for
- This product handles minors' and health data: never commit real personal data, fixtures included. Test data is always synthetic.
- No secrets in the repo.
