import forms from "@tailwindcss/forms";

const token = (name) => `rgb(var(--color-${name}) / <alpha-value>)`;

/** @type {import('tailwindcss').Config} */
export default {
    darkMode: "class",
    content: ["./index.html", "./src/**/*.{js,jsx}"],
    theme: {
        extend: {
            colors: {
                primary: {
                    DEFAULT: token("primary"),
                    hover: token("primary-hover"),
                },
                "on-primary": token("on-primary"),
                background: token("background"),
                surface: token("surface"),
                "on-surface": token("on-surface"),
                muted: token("muted"),
                border: token("border"),
                error: token("error"),
                success: token("success"),
            },
            fontFamily: {
                sans: [
                    "system-ui",
                    "-apple-system",
                    "Segoe UI",
                    "Roboto",
                    "Helvetica Neue",
                    "Arial",
                    "Noto Sans",
                    "sans-serif",
                ],
                mono: ["ui-monospace", "SFMono-Regular", "Menlo", "Consolas", "monospace"],
            },
            maxWidth: {
                container: "1200px",
            },
        },
    },
    plugins: [forms],
};
