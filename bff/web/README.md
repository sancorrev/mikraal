# bff/web

Backend-for-frontend of the mikraal web. It serves `contracts/openapi/bff-web.yaml` on port 8080 and calls the activities service (`contracts/openapi/tasks.yaml`) on port 8081. Ports, dev login, JWT and seed follow `docs/dev-conventions.md`.

Java 21, Spring Boot 4.1 (Jackson 3), Gradle 8.14 (Kotlin DSL), standalone build.

## Run and test

```sh
./gradlew bootRun --args='--spring.profiles.active=dev'   # http://localhost:8080
./gradlew test
```

The `dev` profile is required for now: it provides the directory, the dev login and the token issuer.

| Setting | Env variable | Default |
| :-- | :-- | :-- |
| Activities service URL | `MIKRAAL_ACTIVITIES_URL` | `http://localhost:8081` |
| Seed file (dev) | `MIKRAAL_DEV_SEED_PATH` | `../../dev/seed/seed.json` (relative to this folder) |
| JWT secret (dev) | `MIKRAAL_DEV_JWT_SECRET` | the value in `dev-conventions.md` |

## Generated code

The openapi-generator Gradle plugin (7.27.0) writes into `build/generated` before each compile. Never edit it.

- `bff-api`: Spring server interfaces and models from `bff-web.yaml` (`interfaceOnly`, `useTags`, `useSpringBoot3`, `openApiNullable=false`). Controllers implement them.
- `activities-client`: Spring HTTP interfaces (`library=spring-http-interface`) and models from `tasks.yaml`. Spring Boot's HTTP service groups (`@ImportHttpServices`, group `activities`) build the clients on `RestClient`.

Configuration needed with Boot 4:

- `documentationProvider=none`, `annotationLibrary=none`: no Swagger annotations on the classpath.
- `useBeanValidation=false`: the BFF does not validate; the service does, and its validation problems pass through unchanged.
- `skipDefaultInterface=true`: every endpoint must be implemented; no silent 501.
- `generateJsonIncludeAnnotations=true`: absent optional fields are not sent to the service as `null`.
- `schemaMappings UpdateTask → tools.jackson.databind.JsonNode`: without JsonNullable a model cannot tell an absent field from an explicit `null`, which a merge patch needs. The patch travels as a raw JSON tree.
- Client names get a prefix and suffix (`ActivitiesTask`, `TasksClient`) so they do not clash with server types.
- Supporting files of the client are skipped; the server's `EnumConverterConfiguration` lands in `app.mikraal.bff.web.api.configuration` so component scanning picks it up.
- The client's proxy factory gets a converter so enum query parameters carry the contract value (`in_progress`), not the Java name.
- `useSpringBoot4` exists in 7.27; for these contracts it generates the same code, so `useSpringBoot3` stays as decided.

## Sessions and identity

- `Identity` (interface): who is calling. `HttpSessionIdentity` keeps the person id in the servlet session: cookie `MIKRAAL_SESSION`, httpOnly, SameSite=Lax. Login invalidates any earlier session.
- `ServiceTokens` (interface): `DevJwtTokens` issues HS256 tokens with exactly `iss`, `aud`, `sub`, `name`, `exp` (1 hour). `BearerTokenInterceptor` adds one to every service call.
- `Directory` (interface): `SeedDirectory` reads the seed. Groups of a session are the groups where the person is a scouter. Directory people are scouters only.
- `/api/dev/**` and the dev login exist only in the `dev` profile (404 elsewhere).
- `CsrfGuard`: any method other than GET, HEAD or OPTIONS needs `X-Requested-With: mikraal-web`, or 403. `SessionGuard`: 401 without a session.

All BFF errors are problem details (`application/problem+json`, `type` `about:blank`).

## Enrichment

Each task response from the service is mapped to the web model with a per-request `Names`:

- owners, links → `name` (person full name, position, council or event name);
- `tagIds` → `tags: [{id, name}]`, from the service tag catalogue, fetched once per request and only when the payload carries tag ids;
- `createdBy`, history `actor` and `onBehalfOfPosition` → `{id, name}`;
- history `changes`: values of `owners` and `links` gain a `name`; ids in `tagIds` become `{id, name}`. Keys stay as the service sends them; other fields and unknown shapes pass through.

Unknown ids get the label `names.unknown` from `messages.properties` («Desconocido», unknown). Status codes and `ETag` are copied from the service. `If-Match` is forwarded as received. Service errors pass through with their status, content type and body unchanged. If the service cannot be reached the BFF answers 503 problem+json.

Logs carry ids only, never names or tokens.

## Deviations and open questions

- `Problem.type` is required by the contract; Spring 7 omits it unless set, so the BFF sets `about:blank`.
- The directory check (403 for non-scouters) is decided here from the seed, not through OpenFGA. Task and tag permissions stay with the service.
- History `changes.tagIds` keeps its key while its values become `{id, name}`. Should the key become `tags`?
- A tag deleted from the catalogue shows as «Desconocido» in history. The service could keep deleted tag names, or history could store them.
- The BFF sends `status` as repeated query parameters (`status=a&status=b`); `tasks.yaml` says `explode: false` (`status=a,b`). Spring accepts both.
