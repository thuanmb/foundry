# Foundry — Project Instructions

Foundry is a base project: a Spring Boot API and worker, a React web app and an Expo mobile app, with shared patterns for auth, background jobs, feature flags and i18n. Product features are added on top of it. The demo features are **Notes** (CRUD) and the **echo** background job.

## Read first

- `docs/architecture.md`: modules, auth model (JWE anonymous / JWT authenticated), background-job pattern, configuration model
- `docs/conventions.md`: coding, DB and API conventions, plus "Adding a feature end to end"
- `docs/configuration-guide.md`: every property, env var and Helm value
- `docs/features/`: feature specs (`BE-*`, `WK-*`, `WEB-*`, `MOB-*`, `INF-*`), written from `FEATURE-TEMPLATE.md`

## Modules

| Path | Stack |
|------|-------|
| `api-server/` | Java 21, Spring Boot 3.3.5, JPA/Hibernate, Flyway (owns the schema), Caffeine, nimbus-jose-jwt |
| `worker/` | Java 21, Spring Boot, PostgreSQL LISTEN/NOTIFY, WebClient OpenAI client |
| `common/` | Shared JPA entities, enums, repositories |
| `web-app/` | React 18, Vite, **Tailwind CSS**, Redux Toolkit, react-i18next, Vitest |
| `mobile-app/` | Expo SDK 57, React Native 0.86, Expo Router, Redux Toolkit + Persist, Jest, Detox |
| `*/helm/`, `monitoring/`, `bitbucket-pipelines.yml` | Helm 3, Prometheus Operator, Bitbucket Pipelines |

## Agent routing

| Feature prefix | Owns | Agent |
|----------------|------|-------|
| `BE-*`, `WK-*`, `INF-*` | `api-server/`, `worker/`, `common/`, Helm, Makefile | `feature-developer` |
| `WEB-*` | `web-app/` | `Expert React Frontend Engineer` |
| `MOB-*` | `mobile-app/` | `mobile-feature-developer` |
| — | planning docs / new feature specs | `implementation-planner` |

## Non-negotiables

- Java package `com.thuanbui.foundry`. DTOs are records. Use Jakarta validation, constructor injection, and `@Transactional` in services.
- Errors are RFC 7807 `application/problem+json` from `GlobalExceptionHandler`, with i18n messages.
- Every PK is a UUIDv7 generated in Java. Clients never see sequential IDs.
- Schema changes only through a new Flyway `V{N}__*.sql` in `api-server`. Never edit an applied migration.
- Don't hard-code business values: use `@ConfigurationProperties` with a default and mirror the setting in the Helm `values.yaml`.
- Feature flags: `feature.flags.<key>.enabled` + `@FeatureFlag("<key>")`. Clients read `/features` or `/config/mobile`.
- Slow work goes through `async_jobs` (`AsyncJobService.createJob` → `JobProcessor` in the worker). Don't block request threads.
- Check ownership on every resource (`RequestIdentity`). Other users' resources return 404.
- User-facing strings go through i18n: Vietnamese by default, English fallback. Logs are in English with no PII or tokens.
- Tests ship with every change: JUnit 5 + Mockito, Testcontainers, Vitest, Jest.

## API conventions

- Base path `/api/v1`. Send `Authorization: Bearer <JWE|JWT>` and `X-Platform: WEB | MOBILE`.
- Pagination `?page=0&size=20` returns `PageResponse {content, page, size, totalElements, totalPages}`.
- Use 202 + `{jobId}` for background work. Progress comes from `GET /jobs/{id}/stream` (SSE) with `GET /jobs/{id}/status` as the polling fallback.

## Database naming

Tables are plural `snake_case`, columns `snake_case`. Name indexes `idx_{table}_{columns}`, foreign keys `fk_{table}_{ref}` and unique constraints `uq_{table}_{columns}`.
