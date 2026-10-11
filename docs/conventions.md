# Conventions

These rules apply to every module. The Notes feature is the reference implementation, so copy its shape when you add something new.

## General

- **No hard-coded business values.** Put limits, TTLs, URLs and flags in properties with defaults (see [configuration-guide.md](configuration-guide.md)).
- **Language**: user-facing text is Vietnamese by default with an English fallback, always through i18n keys. Logs and code are in English.
- **IDs exposed to clients** are UUIDv7 strings, never sequential integers.
- **Security**: validate at every boundary, never trust client input, check ownership on every resource, and keep secrets out of git.
- **Tests** come with every change: unit tests for logic, integration tests for persistence and HTTP.

## Java (api-server, worker, common)

| Topic | Rule |
|-------|------|
| Package | `com.thuanbui.foundry` (API), `com.thuanbui.foundry.worker`, `com.thuanbui.foundry.common` |
| DTOs | Java `record`s in `model/dto`, never JPA entities in responses |
| Validation | Jakarta Bean Validation on request records (`@NotBlank`, `@Size`, `@Min`, `@Max`) plus `@Valid` in the controller |
| Errors | Throw a specific exception and map it in `GlobalExceptionHandler` to RFC 7807 with an i18n message key (`error.<area>.<reason>`) |
| Config | One `@ConfigurationProperties` record per prefix, registered in `FoundryApplication` |
| Transactions | `@Transactional` on service methods, never on controllers |
| Injection | Constructor injection only, no field `@Autowired` |
| Naming | `PascalCase` classes, `camelCase` members, `UPPER_SNAKE` constants |
| Logging | SLF4J, parameterized messages, no PII or tokens; the `rid` comes from MDC automatically |
| Tests | JUnit 5 + Mockito (`*Test`), `@SpringBootTest` + Testcontainers PostgreSQL (`*IntegrationTest`) |

## Database

| Object | Pattern | Example |
|--------|---------|---------|
| Table | `snake_case`, plural | `notes`, `async_jobs` |
| Column | `snake_case` | `created_at`, `owner_key` |
| Primary key | `id UUID` (UUIDv7 set by the app) | |
| Index | `idx_{table}_{columns}` | `idx_notes_user_id_created_at` |
| Foreign key | `fk_{table}_{ref_table}` | `fk_notes_users` |
| Unique | `uq_{table}_{columns}` | `uq_users_phone_number` |
| Migration | `V{N}__verb_object.sql`, append-only | `V7__create_invoices_table.sql` |

Every table gets `created_at` / `updated_at TIMESTAMPTZ` and an `update_updated_at_column()` trigger.

## REST API

- Base path `/api/v1`, plural resource nouns (`/notes`, `/jobs/{id}/status`).
- Headers: `Authorization: Bearer <token>` and `X-Platform: WEB | MOBILE`.
- Status codes: `201` + `Location` for create, `202` for queued jobs, `204` for delete, `400` validation, `401` no or invalid token, `404` not found or not yours, `409` conflict, `429` rate limited, `503` feature disabled.
- Pagination: `?page=0&size=20`, response `PageResponse { content, page, size, totalElements, totalPages }`, maximum size 100.
- Errors: `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`, `timestamp`).

## Web (web-app)

- Function components and hooks only. Pages go in `src/pages`, reusable UI in `src/components`, data hooks in `src/hooks`.
- State: one Redux Toolkit slice per domain (`createSlice` + `createAsyncThunk`). API calls only in `src/services/api.js`.
- Styling: Tailwind with the semantic tokens from `tailwind.config.js` (`bg-surface`, `text-on-surface`, `bg-primary`). Don't use raw hex colours in components.
- All text goes through `t('area.key')`, with keys added to both `locales/vi.json` and `locales/en.json`.
- Tests: `*.test.jsx` next to the code (Vitest + Testing Library).

## Mobile (mobile-app)

- Screens live in `app/` (Expo Router). Keep them thin and move logic into `src/hooks`, `src/redux/slices` and `src/services`.
- TypeScript strict mode, with types in `src/types`. Import through the `@/` alias.
- Styling: `StyleSheet.create` with tokens from `@/theme` (`colors`, `spacing`, `typography`).
- Tokens live in secure storage (`src/services/secureStorage.ts`). Never put them in plain AsyncStorage.
- Gate optional features with `selectFeatures` from the remote config.
- Read the versioned Expo docs before adding native modules (see `mobile-app/AGENTS.md`).

## Adding a feature end to end (Notes as the example)

1. **Spec**: copy [features/FEATURE-TEMPLATE.md](features/FEATURE-TEMPLATE.md) to `features/BE-0xx-<name>.md` (and the WEB/MOB/WK equivalents).
2. **Schema**: add `V{N}__create_<table>.sql` (table, indexes, FK, `updated_at` trigger).
3. **common**: add the entity (`@PrePersist` UUIDv7) and repository (owner-scoped finders such as `findByIdAndUserId`).
4. **api-server**:
   - request and response records in `model/dto`
   - `<Name>Service` (`@Transactional`, throws `<Name>NotFoundException`)
   - `<Name>Controller` (`@FeatureFlag("<key>")`, owner from `RequestIdentity`)
   - an exception mapping in `GlobalExceptionHandler`
   - i18n keys in `messages_vi/en.properties`
   - the feature flag default in `application.properties` and Helm `values.yaml` (`config.featureFlags`)
5. **Tests**: a `<Name>ServiceTest` (Mockito) and a `<Name>IntegrationTest` (CRUD, 401, other-owner 404, validation 400).
6. **Background work?** Add a `JobType` value, a `JobProcessor` bean in the worker with a unit test, and an API endpoint that calls `AsyncJobService.createJob(...)`.
7. **Clients**: add a service (`notesApi.ts`), a slice (`notesSlice.ts`), types, screens or pages, i18n keys and slice tests.
8. **Config and docs**: document new properties in [configuration-guide.md](configuration-guide.md) and add new Helm values.
