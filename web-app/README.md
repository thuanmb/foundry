# Foundry Web

React web client for the Foundry starter.

## Stack

React 18 · Vite 6 · Tailwind CSS 3 (+ `@tailwindcss/forms`) · Redux Toolkit · react-router-dom 7 · react-i18next · axios · `@microsoft/fetch-event-source` (SSE) · Vitest + Testing Library.

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Dev server on http://localhost:3000 |
| `npm run build` | Production build into `build/` |
| `npm run preview` | Preview the production build |
| `npm test` | Run tests once |
| `npm run test:watch` | Run tests in watch mode |

## Environment variables

Copy `.env.example` to `.env.local`.

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Backend API base URL |
| `VITE_TURNSTILE_SITE_KEY` | `1x00000000000000000000AA` (test key) | Cloudflare Turnstile site key |
| `VITE_SITE_URL` | `http://localhost:3000` | Public URL for canonical / Open Graph tags |

## Folder structure

```
src/
  App.jsx              Routes (lazy-loaded pages)
  index.jsx            Entry: ErrorBoundary, Helmet, Redux providers
  index.css            Tailwind + semantic color tokens (light/dark)
  components/          Layout, SEO, Card, PrimaryButton, OutlineButton, LoadingSpinner, ErrorBoundary, EchoJobDemo
  hooks/               useApi, useTurnstile, useJobStream (SSE + polling fallback)
  i18n/                i18next setup + locales/{vi,en}.json
  pages/               HomePage, JobResultPage, NotFoundPage
  redux/               store, tokenSlice (anonymous JWE), jobsSlice
  services/api.js      axios instance + API functions
  utils/               tokenUtils
  test/                Test render helpers
```

## Theming

Colors are semantic tokens (`primary`, `background`, `surface`, `on-surface`, `muted`, `border`, `error`, `success`) defined as CSS variables in `src/index.css` and mapped in `tailwind.config.js`. Light mode is the default; adding the `dark` class to `<html>` switches tokens (toggle in the header). `dark:` variants also work.

## How to…

**Add a page**
1. Create `src/pages/MyPage.jsx` (use `<SEO title={t("my.title")} />`).
2. Register a lazy route in `src/App.jsx`.
3. Optionally add a nav entry in `navLinks` in `src/components/Layout.jsx`.

**Add a Redux slice**
1. Create `src/redux/mySlice.js` with `createSlice` / `createAsyncThunk` (call API functions from `services/api.js`, map errors with `getErrorMessage`).
2. Register the reducer in `src/redux/store.js` (and in `src/test/testUtils.jsx` for tests).

**Add a translation**
1. Add keys to both `src/i18n/locales/vi.json` (default) and `en.json`.
2. Use `const { t } = useTranslation(); t("my.key")`. Never hardcode user-facing strings.

## API notes

- Requests carry `Authorization: Bearer <jwe>` (when a valid anonymous token exists) and `X-Platform: WEB`.
- A `401` clears the stored token; errors follow RFC 7807 (`title`, `detail`, `status`).
- Anonymous sessions come from `POST /turnstile/verify` → `{ jweToken, expiresAt }`, stored in `sessionStorage`.
