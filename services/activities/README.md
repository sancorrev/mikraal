# services/activities

The «Activities and communication» service. For now it holds one module: kraal tasks
(`app.mikraal.activities.tasks`). API: `contracts/openapi/tasks.yaml`. Spec: `mikraal-memory/specs/kraal-tasks.md`.

Java 21, Spring Boot 4.1, Gradle (Kotlin DSL), PostgreSQL 17 with RLS, Flyway, OpenFGA. Standalone Gradle build.

## Run

```bash
cd ../.. && docker compose up -d          # PostgreSQL + OpenFGA (see docs/dev-conventions.md)
cd services/activities
./gradlew bootRun --args='--spring.profiles.active=dev'   # http://localhost:8081
```

With the `dev` profile the service:

- migrates the `activities` schema as `activities_owner` and runs as `activities_app` (no BYPASSRLS);
- creates the OpenFGA store `mikraal-dev` if missing, loads `authz/model.json` when it changed and writes the
  seed tuples that are missing;
- adds the seed tag catalogue (`taskTags` of `dev/seed/seed.json`) to the seed group.

All of it is idempotent. Paths are configurable: `mikraal.seed.path`, `mikraal.dev-bootstrap.model-path`.

## Test

```bash
./gradlew test
```

Integration tests start PostgreSQL 17 (with `dev/postgres/init.sql`) and OpenFGA `v1.8.16` with Testcontainers,
run the dev bootstrap and call the API through MockMvc with real tokens. A movable clock drives recurrence and
reminders; a recording adapter replaces notifications.

## Dev token

HS256, claims as in `docs/dev-conventions.md`. With `openssl`:

```bash
mint() {
  local secret="${MIKRAAL_DEV_JWT_SECRET:-mikraal-dev-only-secret-change-me-0123456789abcdef}"
  b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
  local h=$(printf '{"alg":"HS256","typ":"JWT"}' | b64)
  local p=$(printf '{"iss":"mikraal-dev","aud":"mikraal-activities","sub":"%s","name":"Dev","exp":%d}' \
    "$1" $(( $(date +%s) + 3600 )) | b64)
  printf '%s.%s.%s\n' "$h" "$p" "$(printf '%s.%s' "$h" "$p" | openssl dgst -sha256 -hmac "$secret" -binary | b64)"
}
TOKEN=$(mint 0193a000-0000-7000-8000-000000000012)   # Javier Ruiz Sanz, scouter
curl -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8081/groups/0193a000-0000-7000-8000-000000000001/tasks?assignedTo=me'
```

Ana López Díaz (`…0018`) is a family member: every call answers 403.

## Architecture of the module

| Package | Role |
| :-- | :-- |
| `api` | Controllers implementing the generated interfaces, mapping, merge-patch reader, RFC 9457 problems |
| `application` | Commands (`TaskCommands`, `TagCommands`, `OwnerLeave`), queries, recurrence and reminder jobs |
| `domain` | `Task` and its value types, status transitions, history changes, errors |
| `authz` | `Authorizer`, the single decision point; `TaskRelations` and `PositionHolders`. Only this package talks to OpenFGA |
| `persistence` | JDBC repositories; `TenantScope` opens a transaction and sets `app.current_group` (transaction-local) |
| `calendar` | `GroupDirectory` (groups and time zones), `RoundCalendar` (dev: 1 September), `RecurrenceSchedule` |
| `notification` | `NotificationPort`; the dev adapter logs ids only |
| `dev` | Seed reading and the dev bootstrap |

- **Generated code.** `openApiGenerate` writes interfaces and models to `build/generated/openapi` (`spring`,
  `interfaceOnly`, `useTags`, `useSpringBoot3`, `openApiNullable=false`). It compiles with Boot 4 as is. Two
  settings matter: `UpdateTask` is mapped to Jackson's `ObjectNode` (`schemaMappings`), because a merge patch must
  tell a missing field from a `null` one; and `configPackage` puts the generated enum converters where component
  scanning finds them. Generator 7.27 also offers `useSpringBoot4`/`useJackson3`, a possible later switch.
- **Tenancy.** Every table has `group_id` and a forced RLS policy on `app.current_group`. The app role has only the
  grants it needs; `task_history` is insert-only (no grants plus a trigger). Deleting keeps a tombstone.
- **Authorization.** Group checks: `scouter`, `can_create_tasks`, `can_manage_tags`. Task checks: `can_change_status`,
  `can_edit`, `can_delete` (one batch check per page for `allowedActions`). Task tuples (group, creator, owners,
  `position:<id>#holder`) are written in the same unit of work as the database change.
- **Jobs.** Every 15 minutes (`mikraal.tasks.jobs-cron`), per group: create due recurrence instances, then send
  reminders.

## Deviations and open questions

- Creating with the `id` of a deleted (or foreign) task answers 409 `id-in-use`; the contract has no 409 there.
- OpenFGA errors answer 503 `authorization-unavailable` (not in the contract).
- Extra validation codes: `pause_reason_not_allowed`, `reminder_requires_due_date`, `unknown_tag`,
  `uuid_v7_required`, `not_allowed` (e.g. `status` in a PATCH), `out_of_range`, `too_many`, `invalid`.
- `end_recurrence` uses `can_edit`: the model has no own relation, and the spec gives it to the same people.
- A PATCH that changes nothing keeps the version. Removing a tag bumps the version of the tasks that carried it.
- Recurrence: the series keeps its own calendar; editing an instance's deadline does not move it. The next instance
  copies the newest live instance; its creator is the series creator. New instances send no assignment notice.
- Only `group_id` on rows; ARQ-01 point 1 also asks for the node. To decide in workshop 4.
- Database and OpenFGA writes are not atomic: OpenFGA is written last inside the transaction. An outbox would fix
  the rare commit failure after it.
- OpenFGA v1.8 rejects duplicate writes (`on_duplicate` came later), so the bootstrap reads the stored tuples first.
- `GroupDirectory` reads the seed in every profile until a groups service exists; unknown groups use
  `mikraal.tasks.default-time-zone` and are skipped by the jobs.
- `OwnerLeave.removeFromOpenTasks` has no endpoint: the people service will trigger it with an event.
