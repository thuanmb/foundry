import { screen } from "@testing-library/react";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import i18n from "../i18n/i18n";
import JobResultPage from "./JobResultPage";
import { renderWithProviders } from "../test/testUtils";
import { getJobStatus } from "../services/api";
import vi_ from "../i18n/locales/vi.json";

vi.mock("@microsoft/fetch-event-source", () => ({ fetchEventSource: vi.fn(() => new Promise(() => { })) }));

vi.mock("../services/api", async (importOriginal) => ({
    ...(await importOriginal()),
    getJobStatus: vi.fn(),
}));

const renderPage = (jobId) =>
    renderWithProviders(<JobResultPage />, { route: `/jobs/${jobId}`, path: "/jobs/:jobId" });

describe("JobResultPage", () => {
    beforeEach(async () => {
        vi.clearAllMocks();
        await i18n.changeLanguage("vi");
    });

    it("renders a completed result without opening the stream", async () => {
        const result = { echo: "xin chào", length: 8, processedAt: "2026-01-01T00:00:00Z", llmReply: "Hello!" };
        getJobStatus.mockResolvedValue({ data: { jobId: "job-1", status: "COMPLETED", result } });

        renderPage("job-1");

        expect(await screen.findByText(vi_.job.result)).toBeInTheDocument();
        expect(screen.getByText("xin chào")).toBeInTheDocument();
        expect(screen.getByText("Hello!")).toBeInTheDocument();
        expect(screen.getByTestId("job-result-json")).toHaveTextContent('"length": 8');
        expect(screen.getByRole("link", { name: vi_.job.runAnother })).toHaveAttribute("href", "/");
        expect(getJobStatus).toHaveBeenCalledWith("job-1");
        expect(fetchEventSource).not.toHaveBeenCalled();
    });

    it("renders the error for a failed job", async () => {
        getJobStatus.mockResolvedValue({
            data: { jobId: "job-2", status: "FAILED", error: { code: "JOB_FAILED", message: "Boom" } },
        });

        renderPage("job-2");

        expect(await screen.findByText(vi_.job.failedTitle)).toBeInTheDocument();
        expect(screen.getByRole("alert")).toHaveTextContent("Boom");
    });

    it("opens the SSE stream for a pending job", async () => {
        getJobStatus.mockResolvedValue({ data: { jobId: "job-3", status: "PENDING" } });

        renderPage("job-3");

        expect(await screen.findByText(vi_.job.timeline, { selector: "h2" })).toBeInTheDocument();
        await vi.waitFor(() => expect(fetchEventSource).toHaveBeenCalled());
        const [url, options] = fetchEventSource.mock.calls[0];
        expect(url).toMatch(/\/jobs\/job-3\/stream$/);
        expect(options.headers["X-Platform"]).toBe("WEB");
    });
});
