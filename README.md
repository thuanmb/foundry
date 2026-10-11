# Foundry

Foundry is a production-ready starter for products that need a **REST API**, a **background worker**, a **web app** and a **mobile app**.
It contains no business features. A small Notes feature and an "echo" background job show each pattern working from end to end.

| Module | Stack | Purpose |
|--------|-------|---------|
| [`api-server/`](api-server/) | Java 21, Spring Boot 3.3, JPA, Flyway, Caffeine | REST API (`/api/v1`), auth, feature flags, SSE job stream |
| [`worker/`](worker/) | Java 21, Spring Boot, WebClient | Processes `async_jobs` using LISTEN/NOTIFY, with an optional OpenAI client |
| [`common/`](common/) | JPA entities, enums, repositories | Code shared by the API and the worker |
| [`web-app/`](web-app/) | React 18, Vite, Tailwind CSS, Redux Toolkit, react-i18next | Web client (anonymous users + echo demo) |
| [`mobile-app/`](mobile-app/) | Expo SDK 57, React Native 0.86, Expo Router, Redux Toolkit + Persist | iOS/Android client (login, notes, echo demo) |
| [`api-server/helm/foundry`](api-server/helm/foundry), [`worker/helm/foundry-worker`](worker/helm/foundry-worker) | Helm 3 | Kubernetes deployment (dev/staging/prod values) |
| [`monitoring/`](monitoring/) | Prometheus Operator, Grafana | ServiceMonitors, alerts, dashboards |
| [`bitbucket-pipelines.yml`](bitbucket-pipelines.yml) | Bitbucket Pipelines | Test, build and push images, deploy with Helm |

## Documentation

- [docs/architecture.md](docs/architecture.md): system design, auth model, background-job pattern, real-time updates, configuration
- [docs/conventions.md](docs/conventions.md): coding conventions and how to add a feature end to end
- [docs/configuration-guide.md](docs/configuration-guide.md): every configurable property and how to override it
- [docs/development-workflow.md](docs/development-workflow.md): feature specs (`docs/features/`) and the Copilot agents
- [docs/cicd-setup-guide.md](docs/cicd-setup-guide.md) and [docs/production-deployment.md](docs/production-deployment.md): CI/CD and deployment

## Quickstart

Prerequisites: JDK 21, Docker, Node 20+, and optionally Minikube and Helm.

```bash
make setup                # copies .env.example -> .env, installs web/mobile deps
make dev-api              # Postgres in Docker + API on http://localhost:8080 (Flyway migrates the schema)
make dev-worker           # in another terminal: worker on http://localhost:8081
make dev-web              # in another terminal: web on http://localhost:3000
make dev-mobile           # Expo dev server (see mobile-app/README.md for dev-client builds)
```

To run everything in Docker without a local JDK, use `make app-up` (Postgres, API and worker).

Try the API:

```bash
# 1. Anonymous token (Turnstile test key always passes in dev)
TOKEN=$(curl -s -X POST localhost:8080/api/v1/turnstile/verify -H 'Content-Type: application/json' \
  -d '{"token":"XXXX.DUMMY.TOKEN.XXXX"}' | jq -r .jweToken)

# 2. Submit a background job and stream its result
JOB=$(curl -s -X POST localhost:8080/api/v1/jobs/echo -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"message":"hello","delaySec":2}' | jq -r .jobId)
curl -N -H "Authorization: Bearer $TOKEN" "localhost:8080/api/v1/jobs/$JOB/stream"
```

## Tests

```bash
make test        # common + API unit tests, web (Vitest), mobile (typecheck, lint, Jest)
make test-all    # also runs Testcontainers integration tests (requires Docker) and worker tests
```

## Start a new product from Foundry

1. Copy this repository into a new repo, then rebrand it:
   ```bash
   scripts/rename-project.sh --slug acme --name "Acme" --package com.acme.app
   ```
   This renames the Java packages and main classes, Maven artifacts, Helm charts, Docker image names, DB name and user, the web/mobile app names, and the bundle IDs.
2. Create secrets: `openssl rand -base64 32` (JWE) and `openssl rand -base64 64` (JWT). Put them in `.env` locally and in CI variables. Never commit them.
3. Mobile: create a Firebase project and save `google-services.json` and `GoogleService-Info.plist` into `mobile-app/` (both are gitignored; see the `*.example` files). Set `eas.projectId` in `mobile-app/app.config.ts`.
4. Replace the brand: the Tailwind tokens in `web-app/tailwind.config.js`, `mobile-app/src/theme/colors.ts`, the icons and splash in `mobile-app/assets/images` (`npm run generate-splash`), and `web-app/public/favicon.ico`.
5. Set real domains: CORS in `api-server/src/main/resources/application-prod.properties`, the Helm `ingress.host` values, the mobile `apiUrl`, and the web `VITE_API_URL` / `VITE_SITE_URL`.
6. Remove the demo once you no longer need it. The Notes feature is spread across: `Note` and `NoteRepository` in `common`, `NoteController`, `NoteService` and the `V5` migration in `api-server`, `notesSlice` and `notesApi` in mobile, and the `notes` tab. The echo demo is `EchoJobController`, `EchoJobProcessor`, `JobType.ECHO`, and the echo UI in web and mobile.
7. Write your first feature spec from [docs/features/FEATURE-TEMPLATE.md](docs/features/FEATURE-TEMPLATE.md).

## License

See [LICENSE](LICENSE).
