# {ID}: {Feature title}

| Field | Value |
|-------|-------|
| Platform | Backend / Worker / Web / Mobile / Infra |
| Owner agent | `feature-developer` / `Expert React Frontend Engineer` / `mobile-feature-developer` |
| Status | Draft / Ready / In progress / Done |
| Depends on | e.g. BE-001 |
| Feature flag | `feature.flags.<key>.enabled` (or "none") |

## Objective

One paragraph: the user problem and what "done" means.

## Scope

**In scope**
- …

**Out of scope**
- …

## API contract

> Backend specs define this; client specs copy it verbatim.

| Method | Path | Auth | Request | Success | Errors |
|--------|------|------|---------|---------|--------|
| `POST` | `/api/v1/...` | JWT / JWE / public | `{ ... }` | `201 { ... }` | `400`, `401`, `404`, `503 FEATURE_DISABLED` |

```json
// Request example
```

```json
// Response example
```

## Data model

```sql
-- V{N}__create_<table>.sql
```

| Entity / field | Type | Notes |
|----------------|------|-------|

## Configuration

| Property | Default | Helm value | Description |
|----------|---------|------------|-------------|

## Implementation steps

1. …
2. …

## UI (client specs)

- Screens/pages and navigation
- States: loading, empty, error (retry), offline, auth required, feature disabled
- i18n keys (`area.key`) in `vi` and `en`

## Tests

- Unit: …
- Integration / component: …
- E2E / manual: …

## Acceptance criteria

- [ ] …
- [ ] Validation errors return RFC 7807 with localized messages
- [ ] Ownership enforced (another user's resource → 404)
- [ ] New properties documented in `docs/configuration-guide.md` and Helm values
