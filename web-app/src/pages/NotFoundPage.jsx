import { useTranslation } from "react-i18next";
import SEO from "../components/SEO";
import PrimaryButton from "../components/PrimaryButton";

const NotFoundPage = () => {
    const { t } = useTranslation();

    return (
        <div className="mx-auto flex max-w-xl flex-col items-center px-4 py-24 text-center">
            <SEO title={t("notFound.title")} noIndex />
            <p className="text-6xl font-bold text-primary">404</p>
            <h1 className="mt-4 text-2xl font-semibold">{t("notFound.title")}</h1>
            <p className="mt-2 text-muted">{t("notFound.message")}</p>
            <PrimaryButton to="/" className="mt-8">
                {t("notFound.backHome")}
            </PrimaryButton>
        </div>
    );
};

export default NotFoundPage;
