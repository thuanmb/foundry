# Foundry Mobile

Generic Expo base app: phone OTP login, anonymous device sessions, remote feature flags, an async job demo (SSE + polling fallback), and Notes CRUD. Fork it and replace the demo screens with your features.

## Stack

- Expo SDK 57, React Native 0.86, React 19, TypeScript
- Expo Router (file-based routes, tabs + stacks)
- Redux Toolkit + redux-persist (auth/anonymous in SecureStore, config in AsyncStorage)
- react-i18next (Vietnamese default, English fallback)
- axios (bearer token + refresh interceptor), react-native-sse
- Firebase phone auth (optional), device attestation (App Attest / Play Integrity)
- Jest (`jest-expo`) + Testing Library, Detox for E2E

## Scripts

| Command | Description |
|---------|-------------|
| `npm start` | Start Metro (use a dev client build) |
| `npm run ios` / `npm run android` | Build and run the native app locally |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run lint` | `expo lint` |
| `npm test` | Unit tests (`npm run test:watch`, `npm run test:coverage`) |
| `npm run generate-splash` | Regenerate `assets/images/splash.png` |

Verify every change with `npm run typecheck && npm run lint && npm test`.

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `APP_ENV` | `development` | `development` / `staging` / `production` — picks app name, bundle id suffix and default API URL |
| `EXPO_PUBLIC_API_URL` | per `APP_ENV` (see `app.config.ts`) | Backend base URL including `/api/v1` |
| `EXPO_PUBLIC_AUTH_PROVIDER` | `firebase` if Firebase config files exist, else `backend` | `firebase` = Firebase phone auth; `backend` = server SMS OTP (`/auth/send-otp`, `/auth/verify-otp`) |
| `EXPO_PUBLIC_PLAY_INTEGRITY_PROJECT_NUMBER` | empty | Google Cloud project number for Play Integrity |

Put local values in `.env.local` (git-ignored).

## Firebase setup

Real Firebase config files are git-ignored. To enable Firebase phone auth:

1. Create iOS/Android apps in the Firebase console with bundle id `com.thuanbui.foundry` (plus `.dev` / `.staging` variants as needed).
2. Download the config files and place them in this folder:
   - `google-services.json` (see `google-services.example.json`)
   - `GoogleService-Info.plist` (see `GoogleService-Info.example.plist`)
3. Rebuild the native app. `app.config.ts` only adds the Firebase plugins and `googleServicesFile` entries when the files exist.

Without these files the app uses the backend SMS OTP endpoints.

## EAS build profiles

| Profile | `APP_ENV` | App name | Bundle id | Notes |
|---------|-----------|----------|-----------|-------|
| `development` | development | Foundry (Dev) | `com.thuanbui.foundry.dev` | Dev client, internal distribution |
| `staging` | staging | Foundry (Staging) | `com.thuanbui.foundry.staging` | Internal, Android APK |
| `production` | production | Foundry | `com.thuanbui.foundry` | Store build, auto-increment |

Example: `eas build --profile staging --platform android`. Set `extra.eas.projectId` in `app.config.ts` after `eas init`.

## Folder structure

```
app/
  _layout.tsx            Root stack: Redux provider, config fetch, device attestation
  index.tsx              Redirects to /home
  (auth)/                Modal stack: login, otp-verify
  (tabs)/
    _layout.tsx          Tab bar (home, notes, account); notes hidden when flag "notes" is off
    home/                Welcome + Echo job demo (index), job progress (job)
    notes/               Notes list (index), create/edit (edit?id=)
    account/             Profile, edit-profile, privacy, delete-account
src/
  components/            Shared UI (Card, PrimaryButton, OutlineButton, account/*)
  hooks/                 useDeviceAttestation, useJobProgress
  i18n/                  i18next setup + locales/{vi,en}.json
  redux/                 store, hooks, selectors, slices/
  services/              api (axios), authApi, notesApi, jobsApi (SSE), configApi, firebase, attestation
  theme/                 colors, typography, spacing, navigation header options
  types/                 API types
  utils/                 apiError, phoneMask
e2e/                     Detox tests
scripts/                 generate-splash.js
```

## How to…

### Add a tab

1. Create `app/(tabs)/<name>/_layout.tsx` (a `Stack` using `stackScreenOptions` from `@/theme/navigation`) and `app/(tabs)/<name>/index.tsx`.
2. Register it in `app/(tabs)/_layout.tsx` with `<Tabs.Screen name="<name>" options={{ tabBarLabel: t('tabs.<name>'), tabBarIcon: ... }} />` (Ionicons from `@expo/vector-icons`).
3. To gate it behind a feature flag, add the flag to `FeatureFlags` in `configSlice.ts` and set `href: features.<flag> ? undefined : null`.

### Add a screen

Create a file in the relevant stack folder (e.g. `app/(tabs)/home/details.tsx`), register a title in that folder's `_layout.tsx`, and navigate with `router.push({ pathname: '/(tabs)/home/details', params: { id } })`. Read params with `useLocalSearchParams`.

### Add a slice

1. Add API types in `src/types/` and calls in `src/services/<domain>Api.ts` (use the shared `api` axios instance).
2. Create `src/redux/slices/<domain>Slice.ts` with `createAsyncThunk` + `getErrorMessage` for errors.
3. Register the reducer in `src/redux/store.ts` (add to the persist whitelist only if it should survive restarts), export from `slices/index.ts`, add selectors in `selectors.ts`.
4. Add tests in `src/redux/slices/__tests__/` (mock the service module with `jest.mock`).

### Add a translation

Add the key to **both** `src/i18n/locales/vi.json` and `en.json`, then use `t('section.key')`. Never hardcode user-facing strings.
