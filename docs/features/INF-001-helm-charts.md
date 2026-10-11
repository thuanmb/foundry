# INF-001: Helm charts and environment configuration

| Field | Value |
|-------|-------|
| Platform | Infra |
| Owner agent | `feature-developer` |
| Status | Done |
| Depends on | — |

## Objective

Deploy the API and the worker to Kubernetes with one chart each. Non-secret config comes from values files, and secrets are injected at deploy time.

## Charts

| Chart | Path | Renders |
|-------|------|---------|
| API | `api-server/helm/foundry` | Deployment `<release>-server`, Service, ConfigMap `<release>-app-config`, Secret `<release>-secrets`, optional Ingress, HPA, PDB, Bitnami PostgreSQL subchart |
| Worker | `worker/helm/foundry-worker` | Deployment `<release>-foundry-worker`, Service, app ConfigMap, prompt-files ConfigMap (`config/*` → `/etc/opt/foundry/config`), Secret, optional HPA |

## Configuration flow

`values.yaml` → `config.*` → `templates/configmap.yaml` → `/config/application-override.properties` (loaded with `SPRING_CONFIG_ADDITIONAL_LOCATION`).
Secrets (`auth.*`, `turnstile.secretKey`, `firebase.webApiKey`, `twilio.*`, `openai.apiKey`, `postgresql.auth.*`) are passed with `--set` from `.env` (Makefile) or CI variables. They are never committed.

Environment overrides: `values-dev.yaml` (bypass attestation, small resources), `values-staging.yaml` (2 replicas, ingress `api-staging.example.com`), `values-prod.yaml` (`prod` profile, HPA, PDB, ingress `api.example.com`).

## Adding a property

1. Add the default in `application.properties` and the `@ConfigurationProperties` record.
2. Add the value under `config.` in `values.yaml` and render it in `templates/configmap.yaml`.
3. For a secret: add it to `values.yaml` (empty), `templates/secret.yaml` and the `deployment.yaml` env, plus `--set` in the `Makefile` and `bitbucket-pipelines.yml`.
4. Document it in `docs/configuration-guide.md`.

## Commands

```bash
make minikube-dev                 # start cluster, build images, install both charts
helm lint api-server/helm/foundry && helm template api-server/helm/foundry -f api-server/helm/foundry/values-dev.yaml
```

## Acceptance criteria

- [x] `helm lint` / `helm template` pass for dev, staging and prod values
- [x] No secrets in values files
- [x] Feature flags configurable via `config.featureFlags.<key>.enabled`
