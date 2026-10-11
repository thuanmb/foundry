import { useTranslation } from "react-i18next";
import SEO from "../components/SEO";
import EchoJobDemo from "../components/EchoJobDemo";

const FEATURE_KEYS = ["api", "worker", "web", "mobile", "jobs", "auth", "i18n"];

const HomePage = () => {
    const { t } = useTranslation();

    return (
        <>
            <SEO canonicalPath="/" />
            <section className="mx-auto max-w-container px-4 pb-12 pt-16 text-center md:px-8 md:pt-24">
                <h1 className="text-4xl font-bold tracking-tight md:text-5xl">{t("home.hero.title")}</h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-muted">{t("home.hero.subtitle")}</p>
            </section>

            <div className="mx-auto grid max-w-container gap-8 px-4 pb-16 md:grid-cols-2 md:px-8">
                <section aria-labelledby="included-title">
                    <h2 id="included-title" className="mb-4 text-xl font-semibold">
                        {t("home.included.title")}
                    </h2>
                    <ul className="space-y-3">
                        {FEATURE_KEYS.map((key) => (
                            <li key={key} className="flex gap-3">
                                <span aria-hidden="true" className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
                                <span>
                                    <strong className="font-medium">{t(`home.included.items.${key}.title`)}</strong>
                                    <span className="text-muted"> — {t(`home.included.items.${key}.description`)}</span>
                                </span>
                            </li>
                        ))}
                    </ul>
                </section>

                <EchoJobDemo />
            </div>
        </>
    );
};

export default HomePage;
