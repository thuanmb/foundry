import reducer, { setAnonymousToken, clearToken, hydrateToken } from "./tokenSlice";

const initial = { jweToken: null, expiresAt: null };

describe("tokenSlice", () => {
    beforeEach(() => sessionStorage.clear());

    it("has an empty initial state", () => {
        expect(reducer(undefined, { type: "@@INIT" })).toEqual(initial);
    });

    it("setAnonymousToken stores token in state and sessionStorage", () => {
        const payload = { jweToken: "jwe-abc", expiresAt: "2099-01-01T00:00:00Z" };
        const state = reducer(initial, setAnonymousToken(payload));
        expect(state).toEqual(payload);
        expect(sessionStorage.getItem("jwe_token")).toBe("jwe-abc");
        expect(sessionStorage.getItem("jwe_expires_at")).toBe("2099-01-01T00:00:00Z");
    });

    it("clearToken resets state and sessionStorage", () => {
        sessionStorage.setItem("jwe_token", "x");
        sessionStorage.setItem("jwe_expires_at", "y");
        const state = reducer({ jweToken: "x", expiresAt: "y" }, clearToken());
        expect(state).toEqual(initial);
        expect(sessionStorage.getItem("jwe_token")).toBeNull();
        expect(sessionStorage.getItem("jwe_expires_at")).toBeNull();
    });

    it("hydrateToken loads a stored token", () => {
        sessionStorage.setItem("jwe_token", "stored");
        sessionStorage.setItem("jwe_expires_at", "2099-01-01T00:00:00Z");
        expect(reducer(initial, hydrateToken())).toEqual({
            jweToken: "stored",
            expiresAt: "2099-01-01T00:00:00Z",
        });
    });

    it("hydrateToken ignores incomplete storage", () => {
        sessionStorage.setItem("jwe_token", "stored");
        expect(reducer(initial, hydrateToken())).toEqual(initial);
    });
});
