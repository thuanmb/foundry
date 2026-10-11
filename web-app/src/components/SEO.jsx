import { Helmet } from "react-helmet-async";
import { useTranslation } from "react-i18next";

const BASE_URL = (import.meta.env.VITE_SITE_URL || "http://localhost:3000").replace(/\/$/, "");
const OG_LOCALES = { vi: "vi_VN", en: "en_US" };

const SEO = ({ title, description, canonicalPath = "/", ogImage, ogType = "website", noIndex = false, children }) => {
    const { t, i18n } = useTranslation();
    const siteName = t("app.name");
    const fullTitle = title ? `${title} | ${siteName}` : `${siteName} — ${t("app.tagline")}`;
    const metaDescription = description || t("app.description");
    const canonical = `${BASE_URL}${canonicalPath}`;
    const lang = (i18n.resolvedLanguage || i18n.language || "vi").slice(0, 2);

    return (
        <Helmet>
            <html lang={lang} />
            <title>{fullTitle}</title>
            <meta name="description" content={metaDescription} />
            <link rel="canonical" href={canonical} />
            {noIndex && <meta name="robots" content="noindex" />}

            <meta property="og:type" content={ogType} />
            <meta property="og:url" content={canonical} />
            <meta property="og:title" content={fullTitle} />
            <meta property="og:description" content={metaDescription} />
            <meta property="og:locale" content={OG_LOCALES[lang] || OG_LOCALES.vi} />
            <meta property="og:site_name" content={siteName} />
            {ogImage && <meta property="og:image" content={ogImage} />}

            <meta name="twitter:card" content={ogImage ? "summary_large_image" : "summary"} />
            <meta name="twitter:title" content={fullTitle} />
            <meta name="twitter:description" content={metaDescription} />
            {ogImage && <meta name="twitter:image" content={ogImage} />}

            {children}
        </Helmet>
    );
};

export default SEO;
