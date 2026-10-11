import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";

const navLinks = [{ path: "/", labelKey: "nav.home" }];
const LANGUAGES = ["vi", "en"];
const THEME_KEY = "theme";

const useTheme = () => {
    const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) || "light");

    useEffect(() => {
        document.documentElement.classList.toggle("dark", theme === "dark");
        localStorage.setItem(THEME_KEY, theme);
    }, [theme]);

    return [theme, () => setTheme((prev) => (prev === "dark" ? "light" : "dark"))];
};

const Layout = ({ children }) => {
    const { t, i18n } = useTranslation();
    const [theme, toggleTheme] = useTheme();
    const currentLang = (i18n.resolvedLanguage || i18n.language || "vi").slice(0, 2);

    return (
        <div className="flex min-h-screen flex-col bg-background">
            <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4">
                {t("nav.skipToContent")}
            </a>
            <header className="sticky top-0 z-40 border-b border-border bg-surface/80 backdrop-blur">
                <nav
                    className="mx-auto flex h-16 max-w-container items-center justify-between gap-4 px-4 md:px-8"
                    aria-label={t("nav.main")}
                >
                    <Link to="/" className="text-lg font-bold tracking-tight text-primary">
                        {t("app.name")}
                    </Link>

                    <div className="flex items-center gap-4">
                        {navLinks.map((link) => (
                            <NavLink
                                key={link.path}
                                to={link.path}
                                end
                                className={({ isActive }) =>
                                    `text-sm font-medium transition-colors ${isActive ? "text-primary" : "text-muted hover:text-on-surface"}`
                                }
                            >
                                {t(link.labelKey)}
                            </NavLink>
                        ))}

                        <div className="flex overflow-hidden rounded-md border border-border" role="group" aria-label={t("nav.language")}>
                            {LANGUAGES.map((lng) => (
                                <button
                                    key={lng}
                                    type="button"
                                    onClick={() => i18n.changeLanguage(lng)}
                                    aria-pressed={currentLang === lng}
                                    className={`px-2 py-1 text-xs font-semibold uppercase ${currentLang === lng ? "bg-primary text-on-primary" : "text-muted hover:text-on-surface"}`}
                                >
                                    {lng}
                                </button>
                            ))}
                        </div>

                        <button
                            type="button"
                            onClick={toggleTheme}
                            className="rounded-md border border-border px-2 py-1 text-xs text-muted hover:text-on-surface"
                            aria-label={t("nav.toggleTheme")}
                        >
                            {theme === "dark" ? t("nav.themeLight") : t("nav.themeDark")}
                        </button>
                    </div>
                </nav>
            </header>

            <main className="flex-1" id="main-content">
                {children}
            </main>

            <footer className="border-t border-border bg-surface">
                <div className="mx-auto flex max-w-container flex-col items-center justify-between gap-2 px-4 py-6 text-sm text-muted md:flex-row md:px-8">
                    <p>{t("footer.copyright", { year: new Date().getFullYear(), name: t("app.name") })}</p>
                    <p>{t("footer.builtWith")}</p>
                </div>
            </footer>
        </div>
    );
};

export default Layout;
