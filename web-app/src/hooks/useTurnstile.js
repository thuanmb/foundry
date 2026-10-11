import { useRef, useCallback, useEffect } from "react";

const TEST_SITE_KEY = "1x00000000000000000000AA";

/**
 * Renders a Cloudflare Turnstile widget into `containerSelector` and resolves with the token.
 * Requires the Turnstile script (render=explicit) loaded in index.html.
 */
export function useTurnstile(containerSelector = "#turnstile-container") {
    const widgetId = useRef(null);

    const remove = useCallback(() => {
        if (widgetId.current !== null && window.turnstile) {
            window.turnstile.remove(widgetId.current);
        }
        widgetId.current = null;
    }, []);

    const execute = useCallback(() => {
        return new Promise((resolve, reject) => {
            if (!window.turnstile) {
                reject(new Error("TURNSTILE_NOT_LOADED"));
                return;
            }
            remove();
            widgetId.current = window.turnstile.render(containerSelector, {
                sitekey: import.meta.env.VITE_TURNSTILE_SITE_KEY || TEST_SITE_KEY,
                callback: (token) => resolve(token),
                "error-callback": () => reject(new Error("TURNSTILE_FAILED")),
                "expired-callback": () => reject(new Error("TURNSTILE_EXPIRED")),
                appearance: "interaction-only",
            });
        });
    }, [containerSelector, remove]);

    const reset = useCallback(() => {
        if (widgetId.current !== null && window.turnstile) {
            window.turnstile.reset(widgetId.current);
        }
    }, []);

    useEffect(() => remove, [remove]);

    return { execute, reset };
}
