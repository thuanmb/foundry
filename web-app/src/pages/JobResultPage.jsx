import { useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import SEO from "../components/SEO";
import Card from "../components/Card";
import LoadingSpinner from "../components/LoadingSpinner";
import PrimaryButton from "../components/PrimaryButton";
import useJobStream from "../hooks/useJobStream";
import { selectJob, TERMINAL_STATUSES } from "../redux/jobsSlice";

const STEPS = ["PENDING", "RUNNING", "COMPLETED"];

const stepState = (step, status) => {
    if (status === "FAILED") {
        return step === "COMPLETED" ? "failed" : "done";
    }
    const current = STEPS.indexOf(status);
    const index = STEPS.indexOf(step);
    if (current < 0) return "upcoming";
    if (index < current || status === "COMPLETED") return "done";
    return index === current ? "current" : "upcoming";
};

const dotClasses = {
    done: "bg-success border-success",
    current: "bg-primary border-primary animate-pulse",
    failed: "bg-error border-error",
    upcoming: "bg-transparent border-border",
};

const JobResultPage = () => {
    const { jobId } = useParams();
    const { t } = useTranslation();
    useJobStream(jobId);
    const job = useSelector(selectJob(jobId));
    const fetchError = useSelector((state) => state.jobs.fetchError);

    const status = job?.status;
    const isDone = TERMINAL_STATUSES.includes(status);

    return (
        <div className="mx-auto max-w-3xl px-4 py-12 md:px-8">
            <SEO title={t("job.title")} canonicalPath={`/jobs/${jobId}`} noIndex />
            <h1 className="mb-2 text-3xl font-bold tracking-tight">{t("job.title")}</h1>
            <p className="mb-8 break-all font-mono text-sm text-muted">
                {t("job.id")}: {jobId}
            </p>

            {!job && !fetchError && <LoadingSpinner label={t("job.loading")} className="py-12" />}

            {!job && fetchError && (
                <Card>
                    <p role="alert" className="text-error">
                        {t("job.loadError")}: {fetchError}
                    </p>
                </Card>
            )}

            {job && (
                <div className="space-y-6">
                    <Card>
                        <h2 className="mb-4 text-lg font-semibold">{t("job.timeline")}</h2>
                        <ol className="flex flex-col gap-4 sm:flex-row sm:gap-8" aria-label={t("job.timeline")}>
                            {STEPS.map((step) => {
                                const state = stepState(step, status);
                                const label = step === "COMPLETED" && status === "FAILED" ? "FAILED" : step;
                                return (
                                    <li key={step} className="flex items-center gap-2" aria-current={state === "current" ? "step" : undefined}>
                                        <span className={`h-3 w-3 rounded-full border-2 ${dotClasses[state]}`} aria-hidden="true" />
                                        <span className={state === "upcoming" ? "text-muted" : "font-medium"}>
                                            {t(`job.status.${label}`)}
                                        </span>
                                    </li>
                                );
                            })}
                        </ol>
                        {!isDone && job.estimatedWaitSec != null && (
                            <p className="mt-4 text-sm text-muted">
                                {t("job.estimatedWait", { count: job.estimatedWaitSec })}
                            </p>
                        )}
                    </Card>

                    {status === "COMPLETED" && job.result && (
                        <Card>
                            <h2 className="mb-4 text-lg font-semibold">{t("job.result")}</h2>
                            <dl className="mb-4 grid grid-cols-[auto,1fr] gap-x-4 gap-y-2 text-sm">
                                <dt className="text-muted">{t("job.fields.echo")}</dt>
                                <dd className="break-words">{job.result.echo}</dd>
                                <dt className="text-muted">{t("job.fields.length")}</dt>
                                <dd>{job.result.length}</dd>
                                <dt className="text-muted">{t("job.fields.processedAt")}</dt>
                                <dd>{job.result.processedAt}</dd>
                                {job.result.llmReply && (
                                    <>
                                        <dt className="text-muted">{t("job.fields.llmReply")}</dt>
                                        <dd className="whitespace-pre-wrap break-words">{job.result.llmReply}</dd>
                                    </>
                                )}
                            </dl>
                            <pre
                                data-testid="job-result-json"
                                className="overflow-x-auto rounded-lg bg-background p-4 font-mono text-xs"
                            >
                                {JSON.stringify(job.result, null, 2)}
                            </pre>
                        </Card>
                    )}

                    {status === "FAILED" && (
                        <Card>
                            <h2 className="mb-2 text-lg font-semibold text-error">{t("job.failedTitle")}</h2>
                            <p role="alert" className="text-sm">
                                {job.error?.message || t("job.failedFallback")}
                                {job.error?.code && <span className="ml-2 font-mono text-muted">({job.error.code})</span>}
                            </p>
                        </Card>
                    )}
                </div>
            )}

            <div className="mt-8">
                <PrimaryButton to="/">{t("job.runAnother")}</PrimaryButton>
            </div>
        </div>
    );
};

export default JobResultPage;
