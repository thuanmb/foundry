import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import { fetchJobStatus, jobUpdated, TERMINAL_STATUSES } from "../redux/jobsSlice";
import { getJobStreamHeaders, getJobStreamUrl } from "../services/api";

const POLL_INITIAL_MS = 2000;
const POLL_MAX_MS = 15000;

class StopStream extends Error { }

/**
 * Keeps the job in the jobs slice up to date: fetches a snapshot, then subscribes via SSE.
 * Falls back to polling (2s, x2 backoff, max 15s) if the stream fails or closes early.
 */
export default function useJobStream(jobId) {
    const dispatch = useDispatch();

    useEffect(() => {
        if (!jobId) return undefined;

        let cancelled = false;
        let finished = false;
        let pollTimer = null;
        const controller = new AbortController();

        const isTerminal = (status) => TERMINAL_STATUSES.includes(status);

        const markIfTerminal = (status) => {
            if (isTerminal(status)) {
                finished = true;
                controller.abort();
            }
        };

        const poll = (delay) => {
            if (cancelled || finished) return;
            pollTimer = setTimeout(async () => {
                const action = await dispatch(fetchJobStatus(jobId));
                if (cancelled) return;
                if (fetchJobStatus.fulfilled.match(action)) {
                    markIfTerminal(action.payload?.status);
                }
                poll(Math.min(delay * 2, POLL_MAX_MS));
            }, delay);
        };

        const handleEvent = (msg) => {
            if (!msg.data) return;
            let data;
            try {
                data = JSON.parse(msg.data);
            } catch {
                return;
            }
            if (msg.event === "completed") {
                dispatch(jobUpdated({ jobId, status: "COMPLETED", result: data.result }));
                markIfTerminal("COMPLETED");
            } else if (msg.event === "failed") {
                dispatch(jobUpdated({ jobId, status: "FAILED", error: data.error }));
                markIfTerminal("FAILED");
            } else {
                dispatch(jobUpdated({ ...data, jobId }));
                markIfTerminal(data.status);
            }
        };

        const openStream = () => {
            fetchEventSource(getJobStreamUrl(jobId), {
                headers: getJobStreamHeaders(),
                signal: controller.signal,
                openWhenHidden: true,
                onopen: async (response) => {
                    const type = response.headers.get("content-type") || "";
                    if (!response.ok || !type.includes("text/event-stream")) {
                        throw new StopStream(`SSE open failed: ${response.status}`);
                    }
                },
                onmessage: handleEvent,
                onclose: () => {
                    // Server closed before a terminal event; stop SSE retries and poll instead.
                    throw new StopStream("SSE closed");
                },
                onerror: (err) => {
                    throw err;
                },
            }).catch(() => {
                if (!cancelled && !finished) poll(POLL_INITIAL_MS);
            });
        };

        (async () => {
            const action = await dispatch(fetchJobStatus(jobId));
            if (cancelled || !fetchJobStatus.fulfilled.match(action)) return;
            if (isTerminal(action.payload?.status)) {
                finished = true;
                return;
            }
            openStream();
        })();

        return () => {
            cancelled = true;
            controller.abort();
            clearTimeout(pollTimer);
        };
    }, [dispatch, jobId]);
}
