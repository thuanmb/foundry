# Expo HAS CHANGED

Read the exact versioned docs at https://docs.expo.dev/versions/v57.0.0/ before writing any code.

## Project structure

- `app/` — Expo Router routes: `(tabs)/{home,notes,account}` (each a Stack), `(auth)/{login,otp-verify}` (modal).
- `src/redux/` — store, slices (`auth`, `anonymous`, `config`, `notes`, `jobs`), typed hooks, selectors.
- `src/services/` — axios client (`api.ts`, token refresh), per-domain API modules, SSE (`jobsApi.ts`), Firebase.
- `src/i18n/locales/{vi,en}.json` — every user-facing string; `vi` is the default.
- Verify with `npm run typecheck && npm run lint && npm test`. See `README.md` for details.
