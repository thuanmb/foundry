import { act, screen } from "@testing-library/react";
import i18n from "../i18n/i18n";
import HomePage from "./HomePage";
import { renderWithProviders } from "../test/testUtils";
import { getFeatures } from "../services/api";
import vi_ from "../i18n/locales/vi.json";
import en from "../i18n/locales/en.json";

vi.mock("../services/api", async (importOriginal) => ({
    ...(await importOriginal()),
    getFeatures: vi.fn(),
}));

// Flushes the async /features request so its state update happens inside act().
const renderHome = async (options) => {
    const result = renderWithProviders(<HomePage />, options);
    await act(() => Promise.resolve());
    return result;
};

describe("HomePage", () => {
    beforeEach(async () => {
        sessionStorage.clear();
        getFeatures.mockResolvedValue({ data: { notes: true, echo: true } });
        await i18n.changeLanguage("vi");
    });

    it("renders hero, included list and verification step in Vietnamese", async () => {
        await renderHome();

        expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(vi_.home.hero.title);
        expect(screen.getByText(vi_.home.included.title)).toBeInTheDocument();
        expect(screen.getByText(vi_.home.included.items.api.title)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: vi_.home.demo.verify })).toBeInTheDocument();
        expect(getFeatures).toHaveBeenCalled();
    });

    it("renders English copy after switching language", async () => {
        await i18n.changeLanguage("en");
        await renderHome();

        expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(en.home.hero.title);
        expect(screen.getByText(en.home.included.title)).toBeInTheDocument();
    });

    it("shows the echo form when a valid token exists", async () => {
        await renderHome({
            preloadedState: { token: { jweToken: "jwe", expiresAt: "2099-01-01T00:00:00Z" } },
        });

        expect(screen.getByLabelText(vi_.home.demo.messageLabel)).toBeInTheDocument();
        expect(screen.getByLabelText(vi_.home.demo.delayLabel)).toBeInTheDocument();
        expect(screen.getAllByRole("option").map((o) => o.value)).toEqual(["0", "3", "5", "10"]);
        expect(screen.getByRole("button", { name: vi_.home.demo.submit })).toBeInTheDocument();
    });

    it("shows a disabled notice when the echo feature is off", async () => {
        getFeatures.mockResolvedValue({ data: { notes: true, echo: false } });
        await renderHome();

        expect(await screen.findByText(vi_.home.demo.disabled)).toBeInTheDocument();
    });
});
