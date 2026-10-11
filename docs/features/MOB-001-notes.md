# MOB-001: Notes tab

| Field | Value |
|-------|-------|
| Platform | Mobile |
| Owner agent | `mobile-feature-developer` |
| Status | Done |
| Depends on | BE-001, auth (phone login) |
| Feature flag | `notes` (from `GET /api/v1/config/mobile`) |

## Objective

Signed-in users manage their notes from a dedicated tab. Signed-out users see a prompt to log in. This is the reference for an auth-gated, flag-gated feature.

## API contract

See [BE-001](BE-001-notes-api.md). The JWT comes from `authSlice` and is refreshed automatically by `src/services/api.ts` on a 401.

## UI

| Route | Purpose |
|-------|---------|
| `app/(tabs)/notes/index.tsx` | `FlatList` of notes, pull-to-refresh, load more (page size 20), create button; login prompt when anonymous |
| `app/(tabs)/notes/edit.tsx` | Create (`no id`) or edit (`?id=`) with save and delete (confirm dialog) |

- The tab is hidden when `features.notes` is false (`app/(tabs)/_layout.tsx`).
- Data: `src/services/notesApi.ts`, `src/redux/slices/notesSlice.ts`, `src/types/note.ts`.
- States: loading, empty, error with retry, saving, validation (title required, ≤ 200 chars).
- i18n: `notes.*`, `tabs.notes` in `vi.json` / `en.json`.

## Tests

`notesSlice.test.ts` (reducers + thunks with a mocked API). Detox smoke test checks the tab labels.

## Acceptance criteria

- [x] List, create, edit, delete work against the API
- [x] Anonymous users see the login prompt, not an error
- [x] Tab hidden when the flag is off
- [x] Works on iOS and Android (safe areas, keyboard, back button)
