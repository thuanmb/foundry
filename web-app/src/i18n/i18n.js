import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import vi from "./locales/vi.json";
import en from "./locales/en.json";

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources: {
            vi: { translation: vi },
            en: { translation: en },
        },
        fallbackLng: "vi",
        supportedLngs: ["vi", "en"],
        load: "languageOnly",
        interpolation: {
            escapeValue: false,
        },
        // Vietnamese is the default; browser language is intentionally not auto-detected.
        detection: {
            order: ["querystring", "localStorage"],
            lookupQuerystring: "lang",
            caches: ["localStorage"],
        },
    });
export default i18n;
