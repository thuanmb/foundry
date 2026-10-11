# BE-001: Notes API (reference CRUD)

| Field | Value |
|-------|-------|
| Platform | Backend |
| Owner agent | `feature-developer` |
| Status | Done |
| Depends on | — (auth, Flyway V1–V4) |
| Feature flag | `feature.flags.notes.enabled` |

## Objective

Signed-in users can create, list, read, update and delete their own text notes. This is the reference implementation for owner-scoped CRUD.

## API contract

All endpoints require a **JWT** (anonymous JWE or no token → `401`). Another user's note → `404`. Flag off → `503 FEATURE_DISABLED`.

| Method | Path | Request | Success |
|--------|------|---------|---------|
| `GET` | `/api/v1/notes?page=0&size=20` | — | `200 PageResponse<NoteResponse>` (newest first, `size` ≤ 100) |
| `POST` | `/api/v1/notes` | `NoteRequest` | `201 NoteResponse` + `Location` |
| `GET` | `/api/v1/notes/{id}` | — | `200 NoteResponse` |
| `PUT` | `/api/v1/notes/{id}` | `NoteRequest` | `200 NoteResponse` |
| `DELETE` | `/api/v1/notes/{id}` | — | `204` |

```json
// NoteRequest
{ "title": "Groceries", "content": "Milk, eggs" }
// NoteResponse
{ "id": "0190f3c2-…", "title": "Groceries", "content": "Milk, eggs",
  "createdAt": "2026-01-01T10:00:00Z", "updatedAt": "2026-01-01T10:00:00Z" }
// PageResponse
{ "content": [ … ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
```

Validation: `title` required, 1–200 chars; `content` ≤ 10,000 chars → `400` problem+json.

## Data model

`V5__create_notes_table.sql`: `notes(id UUID PK, user_id UUID FK→users ON DELETE CASCADE, title VARCHAR(200), content TEXT, created_at, updated_at)`, `idx_notes_user_id_created_at`, `updated_at` trigger in V6.

## Implementation

- `common`: `Note` entity (UUIDv7 `@PrePersist`), `NoteRepository` (`findByUserId(Pageable)`, `findByIdAndUserId`, `deleteByUserId`).
- `api-server`: `NoteRequest`/`NoteResponse`/`PageResponse` records, `NoteService` (`@Transactional`, `NoteNotFoundException`), `NoteController` (`@FeatureFlag("notes")`, user from `RequestIdentity`), `error.note.not_found` i18n key, `AccountCleanupScheduler` deletes notes of purged accounts.

## Tests

`NoteServiceTest` (Mockito), `NoteIntegrationTest` (Testcontainers: CRUD, 401, other-user 404, validation 400).

## Acceptance criteria

- [x] CRUD works for the owner; other users get 404
- [x] Anonymous / missing token → 401
- [x] Pagination capped at 100, newest first
- [x] Flag off → 503
