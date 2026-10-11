# ==============================================================================
# Foundry — Development Makefile
# ==============================================================================

# Load .env file and export all variables to subprocesses
-include .env
export

.PHONY: help up down down-clean app-up dev-api dev-worker dev-web dev-mobile test test-common test-api test-api-integration test-worker test-web test-mobile test-all docker-test-api clean install setup db-reset \
	minikube-start minikube-stop minikube-build minikube-build-api minikube-build-workers minikube-install minikube-forward-api minikube-forward-db \
	minikube-upgrade minikube-upgrade-api minikube-upgrade-workers minikube-rebuild minikube-rebuild-api minikube-rebuild-workers minikube-uninstall minikube-dev

# Minikube configuration (override via env or make vars)
API_IMAGE_NAME ?= foundry-api
WORKER_IMAGE_NAME ?= foundry-worker
API_IMAGE_TAG ?= 0.1.0
WORKER_IMAGE_TAG ?= 0.1.0
API_RELEASE ?= foundry
WORKER_RELEASE ?= foundry-worker
API_CHART ?= ./api-server/helm/foundry
WORKER_CHART ?= ./worker/helm/foundry-worker
WORKER_DEPLOYMENT ?= $(WORKER_RELEASE)-foundry-worker
MINIKUBE_CPUS ?= 4
MINIKUBE_MEMORY ?= 7837
MINIKUBE_DISK ?= 20

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-24s\033[0m %s\n", $$1, $$2}'

# ==============================================================================
# Infrastructure
# ==============================================================================

up: ## Start PostgreSQL (and wait for it to be healthy)
	@docker compose up -d postgres
	@echo "Waiting for PostgreSQL to be ready..."
	@docker compose exec postgres sh -c 'until pg_isready -U foundry_app -d foundry; do sleep 1; done'
	@echo "✓ PostgreSQL is ready on localhost:5432"

down: ## Stop all containers
	@docker compose down

down-clean: ## Stop all containers and delete data volumes
	@docker compose down -v

app-up: ## Build & run Postgres + API + worker in Docker (no local Java needed)
	@docker compose --profile app up -d --build
	@echo "✓ API on http://localhost:8080, worker on http://localhost:8081"

# ==============================================================================
# Local Development (JVM on host, Postgres in Docker)
# ==============================================================================

dev-api: up ## Run the API server locally (http://localhost:8080)
	@./mvnw -q -pl common -am install -DskipTests
	@./mvnw -pl api-server spring-boot:run

dev-worker: up ## Run the worker locally (http://localhost:8081)
	@./mvnw -q -pl common -am install -DskipTests
	@./mvnw -pl worker spring-boot:run

# ==============================================================================
# Minikube Development (API Server + Worker on Kubernetes)
# ==============================================================================

minikube-start: ## Start Minikube cluster
	@minikube start --driver=docker --cpus $(MINIKUBE_CPUS) --memory $(MINIKUBE_MEMORY)
	@minikube ssh "sudo mkdir -p /mnt/foundry-shared-volume && sudo chmod -R 777 /mnt/foundry-shared-volume" 2>/dev/null || true
	@echo "✓ Minikube cluster is ready"

minikube-stop: ## Stop Minikube cluster
	@minikube stop
	@echo "✓ Minikube stopped"

minikube-build: minikube-build-api minikube-build-workers ## Build API server & worker Docker images inside Minikube
	@echo "✓ All images built"

minikube-build-api: ## Build API server Docker images inside Minikube
	@eval $$(minikube -p minikube docker-env) && \
		export DOCKER_BUILDKIT=1 && \
		echo "Building $(API_IMAGE_NAME):$(API_IMAGE_TAG)..." && \
		docker build --platform=linux/amd64 -f ./Dockerfile.api -t $(API_IMAGE_NAME):$(API_IMAGE_TAG) ./ && \
		echo "✓ Images built"

minikube-build-workers: ## Build worker Docker images inside Minikube
	@eval $$(minikube -p minikube docker-env) && \
		export DOCKER_BUILDKIT=1 && \
		echo "Building $(WORKER_IMAGE_NAME):$(WORKER_IMAGE_TAG)..." && \
		docker build --platform=linux/amd64 -f ./Dockerfile.worker -t $(WORKER_IMAGE_NAME):$(WORKER_IMAGE_TAG) ./ && \
		echo "✓ Images built"

minikube-install: ## Install/upgrade Helm charts on Minikube
	@: $(if $(AUTH_JWE_SECRET_KEY),,$$(error AUTH_JWE_SECRET_KEY is not set. Check your .env file))
	@: $(if $(AUTH_JWT_SECRET_KEY),,$$(error AUTH_JWT_SECRET_KEY is not set. Check your .env file))
	@: $(if $(TURNSTILE_SECRET_KEY),,$$(error TURNSTILE_SECRET_KEY is not set. Check your .env file))
	@: $(if $(FIREBASE_WEB_API_KEY),,$$(error FIREBASE_WEB_API_KEY is not set. Check your .env file))
	@: $(if $(SPRING_DATASOURCE_USERNAME),,$$(error SPRING_DATASOURCE_USERNAME is not set. Check your .env file))
	@: $(if $(SPRING_DATASOURCE_PASSWORD),,$$(error SPRING_DATASOURCE_PASSWORD is not set. Check your .env file))
	@: $(if $(DB_NAME),,$$(error DB_NAME is not set. Check your .env file))
	@: $(if $(DB_HOST),,$$(error DB_HOST is not set. Check your .env file))
	@helm upgrade --install $(API_RELEASE) $(API_CHART) -f $(API_CHART)/values.yaml \
		--set image.repository=$(API_IMAGE_NAME) \
		--set image.tag=$(API_IMAGE_TAG) \
		--set auth.jweSecretKey="$(AUTH_JWE_SECRET_KEY)" \
		--set auth.jwtSecretKey="$(AUTH_JWT_SECRET_KEY)" \
		--set turnstile.secretKey="$(TURNSTILE_SECRET_KEY)" \
		--set firebase.webApiKey="$(FIREBASE_WEB_API_KEY)" \
		--set config.attestation.bypassEnabled=$(ATTESTATION_BYPASS_ENABLED) \
		--set postgresql.auth.username="$(SPRING_DATASOURCE_USERNAME)" \
		--set postgresql.auth.password="$(SPRING_DATASOURCE_PASSWORD)" \
		--set postgresql.auth.database="$(DB_NAME)" \
		--set postgresql.auth.host="$(DB_HOST)" \
		--set postgresql.fullnameOverride="$(DB_HOST)"
	@helm upgrade --install $(WORKER_RELEASE) $(WORKER_CHART) -f $(WORKER_CHART)/values.yaml \
		--set image.repository=$(WORKER_IMAGE_NAME) \
		--set image.tag=$(WORKER_IMAGE_TAG) \
		--set openai.apiKey="$(OPENAI_API_KEY)" \
		--set openai.baseUrl="$(OPENAI_API_URL)" \
		--set postgresql.auth.username="$(SPRING_DATASOURCE_USERNAME)" \
		--set postgresql.auth.password="$(SPRING_DATASOURCE_PASSWORD)" \
		--set postgresql.auth.database="$(DB_NAME)" \
		--set postgresql.auth.host="$(DB_HOST)"
	@echo "Waiting for deployments..."
	@kubectl rollout status deployment/$(API_RELEASE)-server --timeout=180s 2>/dev/null || true
	@kubectl rollout status deployment/$(WORKER_DEPLOYMENT) --timeout=180s 2>/dev/null || true
	@echo "✓ Helm charts installed"
	@echo ""
	@echo "API server service:  $(API_RELEASE)-server"
	@echo "Worker service:      $(WORKER_DEPLOYMENT)"
	@echo ""
	@echo "Run 'make minikube-forward-api' to expose the API server on localhost"

minikube-forward-api: ## Port-forward API server to localhost:8080
	@echo "Port-forwarding API server to http://localhost:8080 ..."
	@kubectl port-forward service/foundry-server 8080:8080

minikube-forward-db: ## Port-forward database to localhost:5432
	@echo "Port-forwarding database to http://localhost:5432 ..."
	@kubectl port-forward service/foundry-postgresql 5432:5432

minikube-upgrade: minikube-upgrade-api minikube-upgrade-workers ## Re-apply Helm charts (template/config changes, no image rebuild)

minikube-upgrade-api: ## Re-apply Helm charts (template/config changes, no image rebuild)
	@: $(if $(AUTH_JWT_SECRET_KEY),,$$(error AUTH_JWT_SECRET_KEY is not set. Check your .env file))
	@: $(if $(AUTH_JWE_SECRET_KEY),,$$(error AUTH_JWE_SECRET_KEY is not set. Check your .env file))
	@: $(if $(TURNSTILE_SECRET_KEY),,$$(error TURNSTILE_SECRET_KEY is not set. Check your .env file))
	@: $(if $(FIREBASE_WEB_API_KEY),,$$(error FIREBASE_WEB_API_KEY is not set. Check your .env file))
	@: $(if $(SPRING_DATASOURCE_USERNAME),,$$(error SPRING_DATASOURCE_USERNAME is not set. Check your .env file))
	@: $(if $(SPRING_DATASOURCE_PASSWORD),,$$(error SPRING_DATASOURCE_PASSWORD is not set. Check your .env file))
	@: $(if $(DB_NAME),,$$(error DB_NAME is not set. Check your .env file))
	@: $(if $(DB_HOST),,$$(error DB_HOST is not set. Check your .env file))
	@helm upgrade --install $(API_RELEASE) $(API_CHART) -f $(API_CHART)/values.yaml \
		--set image.repository=$(API_IMAGE_NAME) \
		--set image.tag=$(API_IMAGE_TAG) \
		--set auth.jweSecretKey="$(AUTH_JWE_SECRET_KEY)" \
		--set auth.jwtSecretKey="$(AUTH_JWT_SECRET_KEY)" \
		--set turnstile.secretKey="$(TURNSTILE_SECRET_KEY)" \
		--set firebase.webApiKey="$(FIREBASE_WEB_API_KEY)" \
		--set config.attestation.bypassEnabled=$(ATTESTATION_BYPASS_ENABLED) \
		--set postgresql.auth.username="$(SPRING_DATASOURCE_USERNAME)" \
		--set postgresql.auth.password="$(SPRING_DATASOURCE_PASSWORD)" \
		--set postgresql.auth.database="$(DB_NAME)" \
		--set postgresql.auth.host="$(DB_HOST)" \
		--set postgresql.fullnameOverride="$(DB_HOST)"
	@kubectl rollout restart deployment/$(API_RELEASE)-server
	@kubectl rollout status deployment/$(API_RELEASE)-server --timeout=180s
	@echo "✓ Helm charts (API) upgraded and deployments restarted"

minikube-upgrade-workers: ## Re-apply Helm charts (template/config changes, no image rebuild)
	@: $(if $(SPRING_DATASOURCE_USERNAME),,$$(error SPRING_DATASOURCE_USERNAME is not set. Check your .env file))
	@: $(if $(SPRING_DATASOURCE_PASSWORD),,$$(error SPRING_DATASOURCE_PASSWORD is not set. Check your .env file))
	@: $(if $(DB_NAME),,$$(error DB_NAME is not set. Check your .env file))
	@: $(if $(DB_HOST),,$$(error DB_HOST is not set. Check your .env file))
	@helm upgrade --install $(WORKER_RELEASE) $(WORKER_CHART) -f $(WORKER_CHART)/values.yaml \
		--set image.repository=$(WORKER_IMAGE_NAME) \
		--set image.tag=$(WORKER_IMAGE_TAG) \
		--set openai.apiKey="$(OPENAI_API_KEY)" \
		--set openai.baseUrl="$(OPENAI_API_URL)" \
		--set postgresql.auth.username="$(SPRING_DATASOURCE_USERNAME)" \
		--set postgresql.auth.password="$(SPRING_DATASOURCE_PASSWORD)" \
		--set postgresql.auth.database="$(DB_NAME)" \
		--set postgresql.auth.host="$(DB_HOST)"
	@kubectl rollout restart deployment/$(WORKER_DEPLOYMENT)
	@kubectl rollout status deployment/$(WORKER_DEPLOYMENT) --timeout=180s
	@echo "✓ Helm charts (workers) upgraded and deployments restarted"

minikube-rebuild: minikube-build ## Rebuild images and restart deployments
	@kubectl rollout restart deployment/$(API_RELEASE)-server
	@kubectl rollout restart deployment/$(WORKER_DEPLOYMENT)
	@kubectl rollout status deployment/$(API_RELEASE)-server --timeout=180s
	@kubectl rollout status deployment/$(WORKER_DEPLOYMENT) --timeout=180s
	@echo "✓ Deployments restarted with updated images"

minikube-rebuild-api: minikube-build-api ## Rebuild API images and restart deployments
	@kubectl rollout restart deployment/$(API_RELEASE)-server
	@kubectl rollout restart deployment/$(WORKER_DEPLOYMENT)
	@echo "✓ Deployments restarted with updated images"

minikube-rebuild-workers: minikube-build-workers ## Rebuild images and restart deployments
	@kubectl rollout restart deployment/$(WORKER_DEPLOYMENT)
	@kubectl rollout status deployment/$(WORKER_DEPLOYMENT) --timeout=180s
	@echo "✓ Deployments restarted with updated images"

minikube-uninstall: ## Uninstall Helm charts from Minikube
	@helm uninstall $(API_RELEASE) 2>/dev/null || true
	@helm uninstall $(WORKER_RELEASE) 2>/dev/null || true
	@echo "✓ Helm charts uninstalled"

minikube-dev: minikube-start minikube-build minikube-install ## Full dev setup (start cluster, build images, install charts)
	@echo ""
	@echo "✓ Minikube development environment ready!"
	@echo "  Run 'make minikube-forward-api' to expose API server on localhost:8080"
	@echo "  Run 'make minikube-rebuild' after code changes"
	@echo "  Run 'make minikube-uninstall' to tear down"

# ==============================================================================
# Testing
# ==============================================================================

test-common: ## Run common module unit tests
	@./mvnw -pl common test

test-api: ## Run API server unit tests
	@./mvnw -pl common,api-server test

test-api-integration: up ## Run API server integration tests (starts DB)
	@./mvnw -pl common,api-server verify

docker-test-api: ## Run API server tests in a Maven container (no local Java needed)
	@docker compose --profile test run --rm api-test

test-worker: up ## Run worker tests
	@./mvnw -pl common,worker test

test: test-common test-api test-web test-mobile ## Run all unit tests

test-all: test-api-integration test-worker test-web test-mobile ## Run all tests including integration

# ==============================================================================
# Web App Development
# ==============================================================================

dev-web: ## Start web app dev server
	@cd web-app && npm run dev

test-web: ## Run web app unit tests
	@cd web-app && npm test

# ==============================================================================
# Mobile App Development
# ==============================================================================

dev-mobile: ## Start Expo dev server (use a dev client build for native modules)
	@cd mobile-app && npx expo start

test-mobile: ## Run mobile typecheck, lint and unit tests
	@cd mobile-app && npm run typecheck && npm run lint && npm test

# ==============================================================================
# Utilities
# ==============================================================================

clean: ## Clean build artifacts
	@./mvnw clean
	@cd web-app && rm -rf node_modules/.cache build

install: ## Install all dependencies
	@cd web-app && npm install
	@cd mobile-app && npm install

setup: ## First-time setup (make mvnw executable, copy .env, install deps)
	@chmod +x mvnw
	@test -f .env || cp .env.example .env
	@test -f web-app/.env.local || cp web-app/.env.example web-app/.env.local
	@cd web-app && npm install
	@cd mobile-app && npm install
	@echo "✓ Setup complete. Edit .env with your secrets, then run: make dev-api"

db-reset: up ## Reset database (drop & recreate via Flyway)
	@./mvnw -pl api-server flyway:clean flyway:migrate -Dflyway.cleanDisabled=false
