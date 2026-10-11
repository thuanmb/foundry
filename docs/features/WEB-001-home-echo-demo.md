# WEB-001: Home page and echo job demo

| Field | Value |
|-------|-------|
| Platform | Web |
| Owner agent | `Expert React Frontend Engineer` |
| Status | Done |
| Depends on | WK-001 |
| Feature flag | `echo` (from `GET /api/v1/features`) |

## Objective

A landing page that introduces the stack and lets an anonymous visitor run the echo job and watch its progress live.

## API contract

- `POST /turnstile/verify` `{ token }` → `{ jweToken, expiresAt }`
- `GET /features` → `{ notes, echo, anonymousWeb }`
- `POST /jobs/echo`, `GET /jobs/{id}/status`, `GET /jobs/{id}/stream`: see [WK-001](WK-001-echo-job.md)

## UI

| Route | Page | Notes |
|-------|------|-------|
| `/` | `HomePage` | Hero, "what's included" list, `EchoJobDemo` card (Turnstile → message + delay → submit) |
| `/jobs/:jobId` | `JobResultPage` | Status timeline, result JSON or error, "Run another" |
| `*` | `NotFoundPage` | |

State: `tokenSlice` (JWE + expiry in `sessionStorage`), `jobsSlice` (`submitEchoJob`, `fetchJobStatus`). Live updates: `useJobStream` (fetch-event-source with an Authorization header, polling fallback 2 s → ×2 → 15 s).

States: Turnstile pending/failed, submitting, feature disabled notice, job PENDING/RUNNING/COMPLETED/FAILED, network error with retry.

i18n: `app.*`, `nav.*`, `footer.*`, `errors.*`, `home.*`, `job.*`, `notFound.*` in `vi.json` and `en.json`.

## Tests

`tokenSlice.test.js`, `jobsSlice.test.js`, `api.test.js`, `HomePage.test.jsx`, `JobResultPage.test.jsx`.

## Acceptance criteria

- [x] Anonymous token obtained via Turnstile and attached to API calls
- [x] Job result shown live; polling fallback works when SSE fails
- [x] Demo hidden/disabled when `echo` flag is false
- [x] All strings localized (vi default, en)
