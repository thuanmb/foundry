# WK-001: Echo background job (reference async job)

| Field | Value |
|-------|-------|
| Platform | Backend + Worker |
| Owner agent | `feature-developer` |
| Status | Done |
| Depends on | async_jobs (V4) |
| Feature flag | `feature.flags.echo.enabled` |

## Objective

Demonstrate the full background-job pipeline: the API enqueues a job, the worker processes it, and the client receives the result through SSE or polling. Copy this when adding real job types.

## API contract

Auth: anonymous **JWE** or **JWT**. The caller becomes the job owner (`ownerKey` = `user:<uuid>` | `anon:<tokenHash>`).

| Method | Path | Request | Success | Errors |
|--------|------|---------|---------|--------|
| `POST` | `/api/v1/jobs/echo` | `{ "message": "hi", "delaySec": 3 }` | `202 { "jobId", "status": "PENDING" }` | `400`, `401`, `503` |
| `GET` | `/api/v1/jobs/{id}/status` | — | `200 { jobId, status, estimatedWaitSec?, result?, error? }` | `403` not owner, `404` |
| `GET` | `/api/v1/jobs/{id}/stream` | `Accept: text/event-stream` | SSE events `status`, `completed`, `failed` | `403`, `404` |

- `message`: 1–500 chars, `delaySec`: 0–30 (default 0).
- `completed` data: `{ "jobId", "result": { "echo": "hi", "length": 2, "processedAt": "…", "llmReply"?: "…" } }`
- `failed` data: `{ "jobId", "error": { "code": "JOB_FAILED", "message": "…" } }`

## Flow

1. `EchoJobController` → `AsyncJobService.createJob(ECHO, "echo", {message, delaySec}, ownerKey, …)`. This deduplicates on `parameters_hash` and sends `NOTIFY new_job`.
2. Worker `PostgresJobListener` wakes up. `JobClaimService` claims the job (`FOR UPDATE SKIP LOCKED`) and `JobDispatcher` routes it to `EchoJobProcessor`.
3. `EchoJobProcessor` waits for `delaySec` (interruptible) and builds the result. When `worker.echo.llm-enabled=true` and an OpenAI key is configured, it also calls `LlmClient` with `echo_prompt.{system,user}.txt`.
4. `JobExecutionService` stores `async_jobs.result`, sets status COMPLETED and sends `NOTIFY job_completed`.
5. The API's `JobCompletionListener` pushes `completed` to the SSE subscribers.

## Configuration

`worker.echo.llm-enabled` (false), `worker.echo.max-delay-sec` (30), `worker.llm.*`, `openai.*`, `config.files.paths`.

## Tests

API: `EchoJobIntegrationTest`, `AsyncJobIntegrationTest`, `JobStreamIntegrationTest`. Worker: `EchoJobProcessorTest`, `JobExecutionIntegrationTest`, `JobClaimServiceIntegrationTest`.

## Acceptance criteria

- [x] 202 with jobId; identical in-flight request returns the same job
- [x] Only the owner can read status/stream (403 otherwise)
- [x] Result delivered via SSE; polling returns the same payload
- [x] Worker restart mid-job → job recovered by stale-job task
