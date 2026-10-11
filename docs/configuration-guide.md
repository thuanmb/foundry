# Configuration Guide

Every runtime value is a Spring property with a safe default in `application.properties`. You can override it without code changes in three ways:

| Where | How | Use for |
|-------|-----|---------|
| Local dev | `.env` (loaded by the Makefile / docker-compose) or env vars | secrets, DB URL |
| Kubernetes | Helm `values*.yaml` → `config.*` → ConfigMap `application-override.properties` | non-secret tuning per environment |
| Kubernetes secrets | `helm --set auth.jweSecretKey=...` (from CI variables) → Secret → env var | keys, passwords, API tokens |

Spring's relaxed binding maps env vars too, e.g. `SECURITY_CORS_ALLOWED_ORIGINS` sets `security.cors.allowed-origins`.

## API server (`api-server/src/main/resources/application.properties`)

### Secrets and infrastructure (env vars)

| Env var | Property | Notes |
|---------|----------|-------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `spring.datasource.*` | default `jdbc:postgresql://localhost:5432/foundry`, user `foundry_app` |
| `AUTH_JWE_SECRET_KEY` | `auth.jwe.secret-key` | **required**, 32 bytes base64 (`openssl rand -base64 32`) |
| `AUTH_JWT_SECRET_KEY` | `auth.access-token.secret-key` | **required**, ≥ 64 bytes (`openssl rand -base64 64`) |
| `TURNSTILE_SECRET_KEY` | `turnstile.secret-key` | dev test key `1x0000000000000000000000000000000AA` always passes |
| `FIREBASE_WEB_API_KEY` | `firebase.web-api-key` | verifies Firebase phone-auth ID tokens |
| `SMS_PROVIDER` | `sms.provider` | `twilio` or `noop` (default, logs the OTP) |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | `sms.twilio.*` | needed when `sms.provider=twilio` |
| `IOS_APP_ID`, `IOS_ATTEST_ENVIRONMENT` | `attestation.ios-*` | `TEAMID.bundle.id`, `development`/`production` |
| `ANDROID_PACKAGE_NAME`, `PLAY_INTEGRITY_DECRYPTION_KEY`, `PLAY_INTEGRITY_VERIFICATION_KEY` | `attestation.android-*` | Play Integrity keys from the Play Console |
| `ATTESTATION_BYPASS_ENABLED` | `attestation.bypass-enabled` | `true` only for simulators/dev |

### Auth (`auth.*`)

| Property | Default | Description |
|----------|---------|-------------|
| `auth.access-token.ttl-minutes` | 60 | JWT access token lifetime |
| `auth.access-token.issuer` | foundry | JWT `iss` claim |
| `auth.refresh-token.ttl-days` | 30 | refresh token lifetime |
| `auth.refresh-token.max-per-user` | 5 | oldest tokens are revoked beyond this |
| `auth.otp.length` | 6 | OTP digits |
| `auth.otp.expiry-minutes` | 5 | OTP validity |
| `auth.otp.max-attempts` | 5 | wrong attempts before the OTP is invalidated |
| `auth.otp.rate-limit-per-hour` | 3 | OTP sends per phone per hour |
| `auth.otp.cooldown-sec` | 60 | minimum seconds between sends |
| `auth.jwe.ttl-hours` | 24 | anonymous token lifetime |
| `auth.jwe.algorithm` | A256GCM | JWE content encryption |

### Turnstile, attestation

| Property | Default |
|----------|---------|
| `turnstile.verify-url` | `https://challenges.cloudflare.com/turnstile/v0/siteverify` |
| `turnstile.timeout-sec` | 5 |
| `attestation.bypass-enabled` | `false` |

### Background jobs and real-time (`worker.*`, `sse.*`, `polling.*`)

The API and worker **must** use the same channel names.

| Property | Default | Description |
|----------|---------|-------------|
| `worker.listen.channel` | `new_job` | NOTIFY channel the API sends new job ids on |
| `worker.listen.complete-channel` | `job_completed` | channel the worker sends finished job ids on |
| `worker.listen.fallback-poll-sec` | 30 | polling interval if LISTEN is unavailable |
| `worker.job.max-retries` | 3 | default `async_jobs.max_retries` |
| `worker.job.stale-timeout-minutes` | 5 | RUNNING jobs older than this are reset |
| `sse.heartbeat-interval-sec` | 15 | SSE keep-alive comment |
| `sse.connection-timeout-sec` | 300 | SSE emitter timeout |
| `polling.initial-delay-sec` / `max-delay-sec` / `backoff-multiplier` | 2 / 15 / 2.0 | client polling fallback (served via `/config/mobile`) |

### Feature flags (`feature.flags.<key>.enabled`)

| Key | Default | Guards |
|-----|---------|--------|
| `notes` | true | `/api/v1/notes/**`, notes tab |
| `echo` | true | `POST /api/v1/jobs/echo`, echo demo UI |
| `anonymousWeb` | true | anonymous web token issuing |

To add a flag, add the property (and `config.featureFlags.<key>` in Helm), annotate the endpoint with `@FeatureFlag("<key>")`, and hide the UI with `features.<key>`. Clients receive the flags from `GET /api/v1/features` and `GET /api/v1/config/mobile`, which are cached for `cache.caffeine.features-ttl-sec` / `mobile-config-ttl-sec`.

### Security, cache, account, app

| Property | Default | Description |
|----------|---------|-------------|
| `security.rate-limit.public-endpoints-per-sec` | 10 | per IP |
| `security.rate-limit.auth-endpoints-per-min` | 20 | per IP on `/auth/**` |
| `security.request.max-body-size-mb` | 10 | |
| `security.cors.allowed-origins` | `*` (prod profile: explicit list) | comma-separated |
| `security.cors.max-age-sec` | 3600 | |
| `cache.caffeine.max-entries` | 1000 | |
| `account.deletion.grace-period-days` | 30 | before a deleted account is purged |
| `account.deletion.anonymize-on-delete` | true | |
| `account.retention.inactive-days` | 365 | |
| `app.mobile.min-version` / `latest-version` | 1.0.0 | force/soft update prompts in the mobile app |

The `prod` Spring profile (`application-prod.properties`) disables SQL logging and locks CORS. Helm sets it with `springProfilesActive: "prod"`.

## Worker (`worker/src/main/resources/application.properties`)

| Env var / property | Default | Description |
|--------------------|---------|-------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local `foundry` DB | same database as the API |
| `worker.listen.*`, `worker.job.*` | see the API table | must match the API |
| `worker.job.heartbeat-interval-sec` | 30 | |
| `worker.job.graceful-shutdown-threshold-sec` | 10 | jobs with more remaining time are requeued on shutdown |
| `LLM_MODEL` → `worker.llm.model` | `gpt-4.1-mini` | |
| `worker.llm.timeout-sec` / `max-tokens` / `temperature` | 120 / 1000 / 0.7 | |
| `OPENAI_API_KEY` → `openai.api-key` | empty | the LLM is disabled when empty |
| `openai.base-url` | `https://api.openai.com/v1` | any OpenAI-compatible endpoint |
| `ECHO_LLM_ENABLED` → `worker.echo.llm-enabled` | false | adds `llmReply` to echo results |
| `worker.echo.max-delay-sec` | 30 | |
| `CONFIG_FILES_PATHS` → `config.files.paths` | `/etc/opt/foundry/config/echo_prompt.{system,user}.txt` | prompt templates; a missing file falls back to `classpath:prompts/` |

## Web (`web-app/.env.local`)

| Var | Default | Description |
|-----|---------|-------------|
| `VITE_API_URL` | `http://localhost:8080/api/v1` | API base URL |
| `VITE_TURNSTILE_SITE_KEY` | `1x00000000000000000000AA` | Turnstile site key (the test key always passes) |
| `VITE_SITE_URL` | `http://localhost:3000` | canonical URL for SEO tags |

## Mobile (`mobile-app`, read by `app.config.ts`)

| Var | Description |
|-----|-------------|
| `APP_ENV` | `development` (default) / `staging` / `production`: selects the app name, bundle ID suffix and API URL |
| `EXPO_PUBLIC_API_URL` | overrides the API URL for any variant |
| `EXPO_PUBLIC_AUTH_PROVIDER` | `firebase` or `backend` (server OTP); defaults to `firebase` when the Firebase config files exist |
| `EXPO_PUBLIC_PLAY_INTEGRITY_PROJECT_NUMBER` | Google Cloud project number for Play Integrity |

## Helm

- API chart: [api-server/helm/foundry/values.yaml](../api-server/helm/foundry/values.yaml), with overrides in `values-{dev,staging,prod}.yaml`. Secrets: `auth.*`, `turnstile.secretKey`, `firebase.webApiKey`, `twilio.*`, `attestation.android*Key`, `postgresql.auth.*`.
- Worker chart: [worker/helm/foundry-worker/values.yaml](../worker/helm/foundry-worker/values.yaml). Prompt files live in `worker/helm/foundry-worker/config/` and are mounted at `/etc/opt/foundry/config`.
