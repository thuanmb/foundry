import { configureStore } from "@reduxjs/toolkit";
import reducer, { submitEchoJob, fetchJobStatus, jobUpdated, selectJob } from "./jobsSlice";
import { submitEcho, getJobStatus } from "../services/api";

vi.mock("../services/api", async (importOriginal) => ({
    ...(await importOriginal()),
    submitEcho: vi.fn(),
    getJobStatus: vi.fn(),
}));

const makeStore = () => configureStore({ reducer: { jobs: reducer } });

describe("jobsSlice", () => {
    beforeEach(() => vi.clearAllMocks());

    it("submitEchoJob stores the accepted job", async () => {
        submitEcho.mockResolvedValue({ data: { jobId: "job-1", status: "PENDING" } });
        const store = makeStore();

        const result = await store.dispatch(submitEchoJob({ message: "hi", delaySec: 3 }));

        expect(submitEcho).toHaveBeenCalledWith({ message: "hi", delaySec: 3 });
        expect(result.payload).toEqual({ jobId: "job-1", status: "PENDING" });
        const state = store.getState();
        expect(state.jobs.submitting).toBe(false);
        expect(selectJob("job-1")(state)).toMatchObject({ jobId: "job-1", status: "PENDING" });
        expect(selectJob("job-1")(state).history).toHaveLength(1);
    });

    it("submitEchoJob records the problem detail on failure", async () => {
        submitEcho.mockRejectedValue({
            message: "Request failed",
            response: { status: 400, data: { title: "Bad Request", detail: "message must not be blank" } },
        });
        const store = makeStore();

        await store.dispatch(submitEchoJob({ message: "", delaySec: 0 }));

        expect(store.getState().jobs.submitError).toBe("message must not be blank");
        expect(store.getState().jobs.submitting).toBe(false);
    });

    it("fetchJobStatus merges status and result", async () => {
        const store = makeStore();
        store.dispatch(jobUpdated({ jobId: "job-2", status: "PENDING" }));
        getJobStatus.mockResolvedValue({
            data: { jobId: "job-2", status: "COMPLETED", result: { echo: "hi", length: 2 } },
        });

        await store.dispatch(fetchJobStatus("job-2"));

        const job = selectJob("job-2")(store.getState());
        expect(getJobStatus).toHaveBeenCalledWith("job-2");
        expect(job.status).toBe("COMPLETED");
        expect(job.result).toEqual({ echo: "hi", length: 2 });
        expect(job.history.map((h) => h.status)).toEqual(["PENDING", "COMPLETED"]);
    });

    it("fetchJobStatus sets fetchError on failure", async () => {
        getJobStatus.mockRejectedValue({ message: "Network Error" });
        const store = makeStore();

        await store.dispatch(fetchJobStatus("missing"));

        expect(store.getState().jobs.fetchError).toBe("Network Error");
    });

    it("jobUpdated does not duplicate history for repeated status", () => {
        let state = reducer(undefined, jobUpdated({ jobId: "j", status: "RUNNING" }));
        state = reducer(state, jobUpdated({ jobId: "j", status: "RUNNING", estimatedWaitSec: 3 }));
        expect(state.byId.j.history).toHaveLength(1);
        expect(state.byId.j.estimatedWaitSec).toBe(3);
    });
});
