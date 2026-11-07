# Local development conventions

Shared by `services/activities`, `bff/web` and `web`. Change them here first.

## Ports

| Piece | Port |
| :-- | :-- |
| web (Next.js) | 3000 |
| bff/web | 8080 |
| services/activities | 8081 |
| OpenFGA HTTP API | 8082 |
| PostgreSQL | 5432 |
| Prism mock of `bff-web.yaml` (web without BFF) | 4010 |

`docker compose up -d` starts PostgreSQL and OpenFGA. `dev/postgres/init.sql` creates the `activities` database with an owner role (migrations) and an app role without BYPASSRLS (runtime).

## Contracts

- `contracts/openapi/tasks.yaml`: activities service API. Consumed by `bff/web`.
- `contracts/openapi/bff-web.yaml`: BFF API. Consumed by `web`.
- Code is generated from them at build time into `generated/` folders (git-ignored). Never edit generated code; change the contract.

## Development login and tokens

1. The web shows the synthetic people from `GET /api/dev/people` and calls `POST /api/session/dev-login`.
2. `bff/web` keeps a server-side session (cookie `MIKRAAL_SESSION`, httpOnly, SameSite=Lax) holding the person id.
3. For each call to the activities service, `bff/web` sends `Authorization: Bearer <jwt>`:
   - HS256, secret from env `MIKRAAL_DEV_JWT_SECRET` (default `mikraal-dev-only-secret-change-me-0123456789abcdef`, dev profile only).
   - Claims: `iss` = `mikraal-dev`, `aud` = `mikraal-activities`, `sub` = person id, `name` = full name, `exp` = now + 1 hour.
4. The activities service validates that token and takes `sub` as the actor.
5. Writes from the browser carry `X-Requested-With: mikraal-web`.

The identity provider of workshop 5 replaces steps 1–4; keep them behind an interface on each side.

## Synthetic data

`dev/seed/seed.json` is the single source of synthetic data: group, people, positions and holders, councils, events, tag names and the OpenFGA tuples.
- `services/activities` (dev profile) creates the OpenFGA store `mikraal-dev` if missing, loads `authz/model.json` and writes the seed tuples; it seeds the tag catalogue.
- `bff/web` (dev profile) reads it as its directory of names (people, positions, councils, events) behind a `Directory` interface that the people and identity services replace later.
- Ana López Díaz is a family member, not a scouter: she must never see tasks. Use her to test denials.

## Authorization

`authz/model.json` (readable form in `authz/model.fga`) is the OpenFGA model. Only the activities service talks to OpenFGA.

## Data rules (from `mikraal-memory/decisions.md`, ARQ-01)

Tenant (group) on every row with RLS; history never overwritten; explicit commands with an actor; UUIDv7; dates in UTC, deadlines as local dates in the group's time zone; logs without personal data; UI texts outside the code.
