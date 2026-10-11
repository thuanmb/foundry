import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import Card from "./Card";
import PrimaryButton from "./PrimaryButton";
import LoadingSpinner from "./LoadingSpinner";
import useApi from "../hooks/useApi";
import { useTurnstile } from "../hooks/useTurnstile";
import { getFeatures, verifyTurnstile, getErrorMessage } from "../services/api";
import { setAnonymousToken } from "../redux/tokenSlice";
import { submitEchoJob } from "../redux/jobsSlice";
import { isTokenValid } from "../utils/tokenUtils";

export const MESSAGE_MAX = 500;
export const DELAY_OPTIONS = [0, 3, 5, 10];

const inputClasses =
    "block w-full rounded-lg border-border bg-background text-on-surface shadow-sm focus:border-primary focus:ring-primary";

const EchoJobDemo = () => {
    const { t } = useTranslation();
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { jweToken, expiresAt } = useSelector((state) => state.token);
    const { submitting, submitError } = useSelector((state) => state.jobs);
    const hasToken = isTokenValid(jweToken, expiresAt);

    const { execute: runTurnstile } = useTurnstile("#turnstile-container");
    const [verifying, setVerifying] = useState(false);
    const [verifyError, setVerifyError] = useState(null);

    const [message, setMessage] = useState("");
    const [delaySec, setDelaySec] = useState(DELAY_OPTIONS[1]);
    const [validationError, setValidationError] = useState(null);

    const { data: features, execute: loadFeatures } = useApi(getFeatures);
    useEffect(() => {
        loadFeatures().catch(() => { });
    }, [loadFeatures]);
    const echoDisabled = features?.echo === false;

    const handleVerify = async () => {
        setVerifying(true);
        setVerifyError(null);
        try {
            const turnstileToken = await runTurnstile();
            const response = await verifyTurnstile(turnstileToken);
            dispatch(setAnonymousToken(response.data));
        } catch (err) {
            setVerifyError(
                err.message?.startsWith("TURNSTILE_") ? t(`errors.${err.message}`) : getErrorMessage(err)
            );
        } finally {
            setVerifying(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const trimmed = message.trim();
        if (!trimmed) {
            setValidationError(t("home.demo.errors.messageRequired"));
            return;
        }
        if (trimmed.length > MESSAGE_MAX) {
            setValidationError(t("home.demo.errors.messageTooLong", { max: MESSAGE_MAX }));
            return;
        }
        setValidationError(null);
        try {
            const job = await dispatch(submitEchoJob({ message: trimmed, delaySec })).unwrap();
            navigate(`/jobs/${job.jobId}`);
        } catch {
            // submitError in the slice is rendered below
        }
    };

    return (
        <Card as="section" aria-labelledby="echo-demo-title">
            <h2 id="echo-demo-title" className="mb-2 text-xl font-semibold">
                {t("home.demo.title")}
            </h2>
            <p className="mb-6 text-sm text-muted">{t("home.demo.description")}</p>

            {echoDisabled ? (
                <p className="text-sm text-muted">{t("home.demo.disabled")}</p>
            ) : !hasToken ? (
                <div className="space-y-4">
                    <p className="text-sm">{t("home.demo.verifyPrompt")}</p>
                    <div id="turnstile-container" />
                    <PrimaryButton onClick={handleVerify} disabled={verifying}>
                        {verifying && <LoadingSpinner size="sm" />}
                        {verifying ? t("home.demo.verifying") : t("home.demo.verify")}
                    </PrimaryButton>
                    {verifyError && (
                        <p role="alert" className="text-sm text-error">
                            {verifyError}
                        </p>
                    )}
                </div>
            ) : (
                <form onSubmit={handleSubmit} className="space-y-4" noValidate>
                    <div>
                        <label htmlFor="echo-message" className="mb-1 block text-sm font-medium">
                            {t("home.demo.messageLabel")}
                        </label>
                        <textarea
                            id="echo-message"
                            rows={4}
                            maxLength={MESSAGE_MAX}
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                            placeholder={t("home.demo.messagePlaceholder")}
                            className={inputClasses}
                        />
                        <p className="mt-1 text-right text-xs text-muted">
                            {message.length}/{MESSAGE_MAX}
                        </p>
                    </div>
                    <div>
                        <label htmlFor="echo-delay" className="mb-1 block text-sm font-medium">
                            {t("home.demo.delayLabel")}
                        </label>
                        <select
                            id="echo-delay"
                            value={delaySec}
                            onChange={(e) => setDelaySec(Number(e.target.value))}
                            className={inputClasses}
                        >
                            {DELAY_OPTIONS.map((sec) => (
                                <option key={sec} value={sec}>
                                    {t("home.demo.delayOption", { count: sec })}
                                </option>
                            ))}
                        </select>
                    </div>
                    {(validationError || submitError) && (
                        <p role="alert" className="text-sm text-error">
                            {validationError || submitError}
                        </p>
                    )}
                    <PrimaryButton type="submit" disabled={submitting}>
                        {submitting && <LoadingSpinner size="sm" />}
                        {submitting ? t("home.demo.submitting") : t("home.demo.submit")}
                    </PrimaryButton>
                </form>
            )}
        </Card>
    );
};

export default EchoJobDemo;
