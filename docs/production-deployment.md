# Production Deployment Guide

How to deploy the API server, worker, web bundle, and monitoring to production, plus a
checklist of production-readiness items that still need attention before hard launch.

Two deployment paths are covered:

- **Kubernetes** (§2–§3) — full CI/CD via Bitbucket Pipelines + Helm.
- **Single free VM + static hosting** (§5) — Oracle Cloud Always Free for the backend and
  Cloudflare Pages for the web. Recommended to start with to keep costs at $0.

## 1. Prerequisites

Complete these before the first production deploy:

- Bitbucket repository variables set — see [cicd-setup-guide.md](cicd-setup-guide.md#4-configure-bitbucket-repository-variables).
- Deployment environments `staging` and `production` created, each with its own
  `KUBECONFIG` and `NAMESPACE` deployment variables — see
  [cicd-setup-guide.md](cicd-setup-guide.md#5-create-deployment-environments).
- A Docker registry the pipeline can push to.
- A Kubernetes cluster with a reachable context.
- The Prometheus Operator (kube-prometheus-stack) installed, with its
  `serviceMonitorSelector` matching the `release: prometheus` label (required for the monitoring step).
- The production TLS secret `foundry-tls` present in the cluster — on the
  Kubernetes path see deferred item 6; on the free-VM path TLS is handled via
  Cloudflare Tunnel/certbot (§5.1).

## 2. Deploy via Bitbucket Pipelines (recommended)

1. Merge to `main`. The pipeline runs tests, then builds & pushes the API and Worker images
   and builds the web production bundle in parallel.
2. **Deploy to Staging** runs automatically.
3. Click **Run** on the **Deploy to Production** step (manual gate).
4. Click **Run** on the **Apply Monitoring Manifests** step (manual gate) to install
   ServiceMonitors, Prometheus rules, and the Alertmanager config secret.
5. Download the web bundle artifact (`web-app/build/**`) and upload it to your static host
   (see §5.2 — Cloudflare Pages on the free path).

Each deploy step sets `--namespace "$NAMESPACE"`, so staging and production deploy into
separate namespaces. `KUBECONFIG` and `NAMESPACE` are scoped per deployment environment.

## 3. Manual Helm deploy (fallback)

```bash
helm upgrade --install foundry api-server/helm/foundry \
  -f api-server/helm/foundry/values-prod.yaml \
  --set image.repository="$REGISTRY/foundry-api" \
  --set image.tag="<commit-sha>" \
  --set auth.jweSecretKey="$AUTH_JWE_SECRET_KEY" \
  --set auth.jwtSecretKey="$AUTH_JWT_SECRET_KEY" \
  --set turnstile.secretKey="$TURNSTILE_SECRET_KEY" \
  --set firebase.webApiKey="$FIREBASE_WEB_API_KEY" \
  --set postgresql.auth.username="$DB_USERNAME" \
  --set postgresql.auth.password="$DB_PASSWORD" \
  --set postgresql.auth.database="$DB_NAME" \
  --namespace foundry-production \
  --wait --timeout 5m

helm upgrade --install foundry-worker worker/helm/foundry-worker \
  -f worker/helm/foundry-worker/values-prod.yaml \
  --set image.repository="$REGISTRY/foundry-worker" \
  --set image.tag="<commit-sha>" \
  --set openai.apiKey="$OPENAI_API_KEY" \
  --set postgresql.auth.username="$DB_USERNAME" \
  --set postgresql.auth.password="$DB_PASSWORD" \
  --set postgresql.auth.database="$DB_NAME" \
  --set postgresql.auth.host="foundry-postgresql" \
  --namespace foundry-production \
  --wait --timeout 5m
```

> The PostgreSQL credentials are supplied at deploy time via `--set postgresql.auth.*`
> (from `.env` locally, or from Bitbucket variables in CI). They are no longer hardcoded
> in the chart.

## 4. Post-deploy verification

1. `GET /actuator/health` returns `UP` for both API and worker pods.
2. A test request returns an `X-Request-Id` header (8-char hex).
3. `GET /api/v1/features` returns the expected feature flags.
4. End-to-end: submit an echo job (`POST /api/v1/jobs/echo`), confirm it moves PENDING → COMPLETED via SSE/polling.

## 5. Deploy on Oracle Cloud Free Tier + Cloudflare Pages (recommended to start)

This is the $0-cost path for phase 1 (20K registered users, ~1K concurrent sessions).

### 5.1 Backend: Oracle Cloud Always Free ARM VM

Oracle's Always Free tier gives 4× ARM OCPU + 24 GB RAM + ~200 GB storage, free forever.

1. Create an account at <https://www.oracle.com/cloud/free/>.
2. Create an **Ampere A1** instance (`VM.Standard.A1.Flex`): 4 OCPU / 24 GB RAM, Ubuntu 24.04,
   200 GB boot volume. If "out of capacity", retry later or use another availability domain.
3. In the VCN security list, open **22** (SSH), **80** (HTTP), **443** (HTTPS).
4. SSH in and install Docker:

   ```bash
   sudo apt-get update && sudo apt-get install -y ca-certificates curl
   sudo install -m 0755 -d /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
   sudo chmod a+r /etc/apt/keyrings/docker.asc
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   sudo apt-get update && sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   sudo usermod -aG docker $USER   # log out/in afterwards
   ```

#### Option A — docker-compose (simplest)

1. Copy secrets into `.env` (see [`.env.example`](../.env.example)) and fill `AUTH_JWE_SECRET_KEY`,
   `AUTH_JWT_SECRET_KEY`, `TURNSTILE_SECRET_KEY`, `FIREBASE_WEB_API_KEY` (and `OPENAI_API_KEY` if the worker uses an LLM).
2. [`docker-compose.yml`](../docker-compose.yml) runs PostgreSQL, the API and the worker under the `app` profile.
   To override the worker prompt templates, mount a directory at `/etc/opt/foundry/config`
   (see [`worker/helm/foundry-worker/config/`](../worker/helm/foundry-worker/config/)).
3. Start everything: `docker compose --profile app up -d --build` (or `make app-up`).
4. TLS: put Cloudflare Tunnel (`cloudflared`) in front of port 8080 for free HTTPS, or use
   nginx + certbot. Skip the K8s Ingress — it is not used on this path.

#### Option B — k3s (keep the Helm workflow)

```bash
curl -sfL https://get.k3s.io | sh -
mkdir -p ~/.kube && sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && sudo chown $USER ~/.kube/config
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

Then run the two `helm upgrade --install` commands from §3. Push the images to a registry the
VM can pull from (e.g. Docker Hub), set `image.repository` / `image.tag` accordingly, and either
install cert-manager for the TLS secret or provide `foundry-tls` manually.

### 5.2 Web: Cloudflare Pages

The web app is a static Vite build ([`web-app/`](../web-app/)).

- Build command: `npm run build` (runs `vite build`)
- Output directory: `build`
- Environment variables: `VITE_API_URL`, `VITE_TURNSTILE_SITE_KEY`

Cloudflare Pages' Git integration does not support Bitbucket, so use **Direct Upload**:

```bash
cd web-app
npm ci
VITE_API_URL=https://api.<your-domain>/api/v1 VITE_TURNSTILE_SITE_KEY=<site-key> npm run build
npx wrangler pages deploy build --project-name=foundry
```

(Or download the `web-app/build/**` artifact from the pipeline and upload it in the dashboard.)

Then attach a custom domain — Cloudflare issues free SSL automatically. Because the API origin
changes, also update `security.cors.allowed-origins` to your web origin (see deferred item 3).

### 5.3 Phone OTP: Firebase Auth

Phone OTP keeps using Firebase Authentication (client SDK: `@react-native-firebase/auth`).

- In the Firebase console, enable the **Phone** sign-in provider.
- Set `FIREBASE_WEB_API_KEY` (used server-side to verify Firebase ID tokens) in `.env` /
  Bitbucket variables.
- No changes to the mobile app are required for this path.

The backend's `firebase.web-api-key` must match the Firebase project your app is configured
against ([`mobile-app/google-services.json`](../mobile-app/google-services.json),
[`mobile-app/GoogleService-Info.plist`](../mobile-app/GoogleService-Info.plist)).

## 6. Deferred items — change these before hard launch

These are known gaps that are not blocking an initial deploy but must be resolved before
public launch.

| # | Item | Where | What to do |
|---|------|-------|------------|
| 1 | iOS App Attest placeholder + wrong environment | [values.yaml](api-server/helm/foundry/values.yaml) | `attestation.iosAppId` is `APP_ID_HERE` and `attestation.iosEnvironment`/`apple.environment` are `development`. Set the real `<TeamID>.<bundleId>` and `production` in [values-prod.yaml](api-server/helm/foundry/values-prod.yaml). |
| 2 | Domain mismatch (mobile app vs ingress vs CORS vs SEO) | [app.config.ts](mobile-app/app.config.ts), [values-prod.yaml](api-server/helm/foundry/values-prod.yaml), [application-prod.properties](api-server/src/main/resources/application-prod.properties) | Mobile uses `api.example.com`, ingress uses `api.foundry.com`, CORS uses `example.com`, web SEO uses `foundry.com`. Pick one domain and reconcile all four. |
| 3 | CORS is `*` in production | [values.yaml](api-server/helm/foundry/values.yaml) | `security.cors.allowedOrigins: "*"` is rendered into the override ConfigMap and wins over the prod profile. Set explicit origins in [values-prod.yaml](api-server/helm/foundry/values-prod.yaml). |
| 4 | Android Play Integrity keys empty | [values.yaml](api-server/helm/foundry/values.yaml) | `attestation.androidDecryptionKey` / `androidVerificationKey` are `""`. Provide real keys (wired via [secret.yaml](api-server/helm/foundry/templates/secret.yaml)). |
| 5 | Flyway `clean` enabled | [application.properties](api-server/src/main/resources/application.properties) | `spring.flyway.cleanDisabled=false`. Set to `true` in production to prevent accidental schema drops. |
| 6 | TLS secret prerequisite | [values-prod.yaml](api-server/helm/foundry/values-prod.yaml) | Ingress references `foundry-tls`, which must exist in the cluster (cert-manager or manual) before HTTPS works. |
| 7 | Worker DEBUG logging | [application.properties](worker/src/main/resources/application.properties) | `debug=true` enables DEBUG across the whole worker. Set to `false` for production. |
| 8 | Empty secret defaults | [values.yaml](api-server/helm/foundry/values.yaml) | `auth.jweSecretKey`, `auth.jwtSecretKey`, `turnstile.secretKey`, `firebase.webApiKey`, `openai.apiKey` default to `""`. Confirm every Bitbucket variable listed in [bitbucket-pipelines.yml](bitbucket-pipelines.yml) is actually set, or deployments will run with empty secrets. |
