import { configureStore } from "@reduxjs/toolkit";
import { AxiosError } from "axios";
import api, { injectStore, getErrorMessage } from "./api";
import tokenReducer, { setAnonymousToken } from "../redux/tokenSlice";

const okAdapter = (captured) => async (config) => {
    captured.config = config;
    return { data: {}, status: 200, statusText: "OK", headers: {}, config };
};

const statusAdapter = (status) => async (config) => {
    const response = { data: { title: "Unauthorized", status }, status, statusText: "", headers: {}, config };
    throw new AxiosError("Request failed", AxiosError.ERR_BAD_REQUEST, config, null, response);
};

describe("api client", () => {
    let store;

    beforeEach(() => {
        sessionStorage.clear();
        store = configureStore({ reducer: { token: tokenReducer } });
        injectStore(store);
    });

    it("adds X-Platform and no Authorization when there is no token", async () => {
        const captured = {};
        await api.get("/features", { adapter: okAdapter(captured) });
        expect(captured.config.headers["X-Platform"]).toBe("WEB");
        expect(captured.config.headers.Authorization).toBeUndefined();
    });

    it("adds Bearer token when a valid token is stored", async () => {
        store.dispatch(setAnonymousToken({ jweToken: "jwe-123", expiresAt: "2099-01-01T00:00:00Z" }));
        const captured = {};
        await api.get("/features", { adapter: okAdapter(captured) });
        expect(captured.config.headers.Authorization).toBe("Bearer jwe-123");
        expect(captured.config.headers["X-Platform"]).toBe("WEB");
    });

    it("skips an expired token", async () => {
        store.dispatch(setAnonymousToken({ jweToken: "old", expiresAt: "2000-01-01T00:00:00Z" }));
        const captured = {};
        await api.get("/features", { adapter: okAdapter(captured) });
        expect(captured.config.headers.Authorization).toBeUndefined();
    });

    it("clears the token on 401", async () => {
        store.dispatch(setAnonymousToken({ jweToken: "jwe-123", expiresAt: "2099-01-01T00:00:00Z" }));
        await expect(api.get("/jobs/x/status", { adapter: statusAdapter(401) })).rejects.toBeInstanceOf(AxiosError);
        expect(store.getState().token.jweToken).toBeNull();
        expect(sessionStorage.getItem("jwe_token")).toBeNull();
    });

    it("keeps the token on other errors", async () => {
        store.dispatch(setAnonymousToken({ jweToken: "jwe-123", expiresAt: "2099-01-01T00:00:00Z" }));
        await expect(api.get("/jobs/x/status", { adapter: statusAdapter(403) })).rejects.toBeInstanceOf(AxiosError);
        expect(store.getState().token.jweToken).toBe("jwe-123");
    });

    it("getErrorMessage prefers RFC 7807 detail, then title", () => {
        expect(getErrorMessage({ message: "x", response: { data: { title: "T", detail: "D" } } })).toBe("D");
        expect(getErrorMessage({ message: "x", response: { data: { title: "T" } } })).toBe("T");
        expect(getErrorMessage({ message: "Network Error" })).toBe("Network Error");
    });
});
