# CI/CD Setup Guide — Bitbucket Pipelines & Container Registry

This guide walks through everything you need to configure outside the repository so that
[`bitbucket-pipelines.yml`](../bitbucket-pipelines.yml) can build, test, push, and deploy
the Foundry services.

## What the pipeline does

| Stage | Trigger | What happens |
|-------|---------|--------------|
| Test | Every PR | Runs `api-server`, `worker`, `web-app` and `mobile-app` checks in parallel |
| Build & Push | Push to `main` | Builds and pushes the API and Worker Docker images; builds the web production bundle |
| Deploy (staging) | Push to `main` (automatic) | `helm upgrade` of API + Worker into staging |
| Deploy (production) | `main` (manual click) | `helm upgrade` of API + Worker into production |
| Monitoring | `main` (manual click) | Applies `monitoring/` manifests (requires the Prometheus Operator) |

Integration tests use **Testcontainers** (they spin up a PostgreSQL container), so the test steps
need the `docker` service — this is already configured in the pipeline.

---

## 1. Prerequisites

- A Bitbucket **Cloud** repository (the project already contains `bitbucket-pipelines.yml`).
- Admin access to the repository (to add variables and deployment environments).
- A **Docker registry** account (Docker Hub is the simplest — see §2).
- A running **Kubernetes cluster** and a `kubeconfig` for it (see §3).

---

## 2. Set up a container registry

The pipeline pushes images to `$REGISTRY/foundry-api:<commit>` and
`$REGISTRY/foundry-worker:<commit>`. Any registry that accepts
`docker login -u <user> -p <password>` works.

### Option A — Docker Hub (recommended to start)

1. Create an account at <https://hub.docker.com>.
2. Create an **Access Token** (Docker Hub → Account Settings → Security → New Access Token).
   Use this token as the password (it is safer than your real password and can be revoked).
3. Note your Docker Hub **username** and the **token**.

### Option B — Other registries

| Registry | `REGISTRY` value | Auth notes |
|----------|------------------|------------|
| Docker Hub | `<dockerhub-username>` | username + access token |
| AWS ECR | `<account-id>.dkr.ecr.<region>.amazonaws.com` | username = `AWS`, password = `aws ecr get-login-password --region <region>` |
| Google Artifact Registry | `<region>-docker.pkg.dev/<project>/<repo>` | username = `_json_key`, password = service-account JSON key |
| Azure ACR | `<registry>.azurecr.io` | username + password from the registry access keys |
| Harbor / self-hosted | `<harbor-host>/<project>` | username + password |

Whichever you choose, the two images below are the ones the pipeline will create:

- `foundry-api`
- `foundry-worker`

---

## 3. Prepare the Kubernetes cluster + kubeconfig

The deploy steps run `helm upgrade --install`, so the pipeline needs credentials for the target
cluster.

1. Create or obtain a `kubeconfig` for the cluster and namespace you want to deploy into.
2. Base64-encode it (Bitbucket variables are single-line strings):

   ```bash
   base64 < ~/.kube/config | tr -d '\n'
   ```

3. Copy the single-line output — you will paste it into the `KUBECONFIG` deployment variable in §5.

> The pipeline decodes it with `echo "$KUBECONFIG" | base64 -d`, so it **must** be base64-encoded.

---

## 4. Configure Bitbucket repository variables

Go to **Repository settings → Pipelines → Repository variables** and add every variable below.
Mark all of them **Secured** (they are secrets).

| Variable | Description / how to obtain |
|----------|------------------------------|
| `REGISTRY` | Registry URL from §2 (e.g. `mycompany`) |
| `DOCKER_USERNAME` | Registry username |
| `DOCKER_PASSWORD` | Registry password or access token |
| `AUTH_JWE_SECRET_KEY` | `openssl rand -base64 32` |
| `AUTH_JWT_SECRET_KEY` | `openssl rand -base64 64` |
| `OPENAI_API_KEY` | OpenAI API key (worker LLM calls) |
| `TURNSTILE_SECRET_KEY` | Cloudflare Turnstile secret key |
| `FIREBASE_WEB_API_KEY` | Firebase Web API key |
| `DB_USERNAME` | PostgreSQL application username |
| `DB_PASSWORD` | PostgreSQL application password |
| `DB_NAME` | PostgreSQL application database name |
| `WEB_API_URL` | Web app API base URL, e.g. `https://api.foundry.com/api/v1` |
| `TURNSTILE_SITE_KEY` | Cloudflare Turnstile site key (used by the web build) |

> `DB_USERNAME` / `DB_PASSWORD` / `DB_NAME` are injected into the API chart's bundled PostgreSQL
> (the bitnami subchart) and shared by the worker.
>
> `KUBECONFIG` and `NAMESPACE` are **deployment-environment** variables, not repository variables —
> each environment (`staging`, `production`) defines its own values. See §5.

---

## 5. Create deployment environments

The pipeline references two **deployment environments**. They must exist or the deploy steps will
not run.

1. Go to **Repository settings → Deployments**.
2. Add an environment named **`staging`**.
3. Add an environment named **`production`**.
4. Optionally restrict who can trigger `production` (Repository settings → Deployments → production →
   **Restrictions**).

Each environment must define its own **deployment variables** (Deployments → `<env>` → Variables):

| Variable | Description |
|----------|-------------|
| `KUBECONFIG` | Base64-encoded kubeconfig for that environment's cluster (see §3) |
| `NAMESPACE` | Kubernetes namespace, e.g. `foundry-staging` / `foundry-production` |

> These are scoped to the environment, so the staging and production steps deploy into
> separate clusters/namespaces even though they share the same variable names.

The `production` step is already `trigger: manual`, so it only runs when you click **Run** on that
step in the pipeline UI.

---

## 6. Verify the setup

1. **Trigger the test pipeline** — push a branch or open a PR. Confirm the three test steps
   (`Test API Server`, `Test Worker`, `Test Web`) pass.
2. **Merge to `main`** — confirm tests pass, then the build steps push
   `foundry-api:<commit>` and `foundry-worker:<commit>` to your registry
   and produce the web production bundle artifact.
3. **Staging auto-deploy** — the `Deploy to Staging` step should run automatically and
   `helm upgrade` both charts.
4. **Production deploy** — open the `main` pipeline run and click **Run** on the
   `Deploy to Production` step.

---

## 7. Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| Deploy step fails with `base64: invalid input` | `KUBECONFIG` is not base64-encoded, or contains line breaks. Re-encode with `base64 < ~/.kube/config \| tr -d '\n'`. |
| `docker login` fails | Wrong `DOCKER_USERNAME`/`DOCKER_PASSWORD`, or `REGISTRY` doesn't match your provider (see §2). |
| Build step fails on `--mount` / BuildKit | `DOCKER_BUILDKIT` must be enabled; it is already set in the pipeline. If your registry image needs `--platform`, add it to the `docker build` command. |
| `helm upgrade` times out | App startup (Flyway + PostgreSQL) may exceed 5m on first run. Increase `--timeout` or check pod logs. |
| `Deploy to Production` button is missing | The `production` deployment environment (Repository settings → Deployments) has not been created. |
| Test steps fail with "Could not find a valid Docker environment" | Testcontainers needs Docker; the `docker` service is configured, but verify Pipelines has the Docker service available (it is built-in on Bitbucket Cloud). |
