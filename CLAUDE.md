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
Pending workshops 2 (services, 16 Nov), 7 (API and mobile, 18 Jan) and 8 (repo layout and CI/CD, 25 Jan).
The proposed layout is in `../mikraal-memory/repo-map.md`. Do not create that structure before those workshops close.

## Watch out for
- This product handles minors' and health data: never commit real personal data, fixtures included. Test data is always synthetic.
- No secrets in the repo.
