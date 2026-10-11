# Worker — Operations Guide

## Manually Re-running a Failed Job

When a job reaches `FAILED` status (e.g., after exhausting retries), you can force a re-run
by resetting it to `PENDING` directly in the database.

### Step 1: Connect to the database

```bash
kubectl exec -it foundry-postgresql-0 -- psql -U foundry_app foundry
# or via local psql:
psql $DATABASE_URL
```

### Step 2: Locate the failed job

```sql
SELECT id, job_type, status, retry_count, max_retries, message, created_at
FROM async_jobs
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;
```

### Step 3: Reset to PENDING

Replace `<job-uuid>` with the actual ID:

```sql
UPDATE async_jobs
SET status     = 'PENDING',
    retry_count = 0,
    message     = 'Manually reset for re-run',
    updated_at  = CURRENT_TIMESTAMP
WHERE id = '<job-uuid>';
```

**Why reset `retry_count`?** The worker increments `retry_count` on each retry.
If it was already at `max_retries`, leaving it unchanged would cause the next attempt
to fail immediately without trying again.

### Step 4: Notify the worker

The worker listens on a PostgreSQL `NOTIFY` channel to pick up new jobs without polling delay:

```sql
NOTIFY new_job;
```

### How it works

| Step | Component | Action |
|------|-----------|--------|
| Claim | `AsyncJobRepository.claimNextJob()` | `SELECT ... WHERE status = 'PENDING' ... FOR UPDATE SKIP LOCKED` |
| Lock | `JobClaimService.processJob()` | Sets `status = RUNNING`, `started_at = now()` |
| Execute | `JobExecutionService.executeJob()` | Dispatches to processor; handles retries and heartbeats |
| Retry | `onRetryableFailure()` | Increments `retry_count`, sets status back to `PENDING` |
| Fail | `onPermanentFailure()` | Sets `status = FAILED` |

The `FOR UPDATE SKIP LOCKED` clause ensures multiple worker instances never claim the same row.

### Resetting other statuses

| Current status | What to reset | Notes |
|---|---|---|
| `FAILED` | `retry_count = 0` | See above |
| `COMPLETED` | Clear `result`, `completed_at` | Re-running repeats any side effects of the processor; make processors idempotent |
| `RUNNING` (stale) | Nothing — automatic | `StaleJobRecoveryTask` resets stale RUNNING jobs to PENDING on a schedule |

## Deleting jobs

`async_jobs` has no child tables in the base project, so rows can be deleted directly (e.g. dev cleanup or retention):

```sql
DELETE FROM async_jobs WHERE status IN ('COMPLETED', 'FAILED') AND completed_at < NOW() - INTERVAL '30 days';
```

If your feature adds tables that reference `async_jobs`, delete those child rows first or declare the FK with `ON DELETE CASCADE`.

## Adding a new job type

1. Add the enum value to `common/.../model/enums/JobType.java`.
2. Implement `JobProcessor` in `worker/.../job/` as a Spring `@Component`:
   - `supportedType()` returns the new `JobType`.
   - `process(AsyncJob job)` reads `job.getParameters()` and returns the result map (stored in `async_jobs.result`).
   - Throw `RetryableException` for transient errors (timeouts, 5xx, rate limits) and `NonRetryableException` / `PermanentJobException` for bad input.
   - Long work should check `Thread.currentThread().isInterrupted()` so graceful shutdown can requeue it.
3. Add an API endpoint that validates input and calls `AsyncJobService.createJob(type, name, parameters, ownerKey, estimatedSec, priority, maxRetries)` and returns `202 {jobId}`. Clients reuse `/jobs/{id}/status` and `/jobs/{id}/stream`.
4. Add tests: a processor unit test (see `EchoJobProcessorTest`) and an integration test that runs the job end to end.
5. If the processor uses an LLM prompt, add `<name>_prompt.system.txt` / `.user.txt` to `src/main/resources/prompts/` and `helm/foundry-worker/config/`, and list them in `config.files.paths`.
