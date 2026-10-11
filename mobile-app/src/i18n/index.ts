import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { getLocales } from 'expo-localization';
import AsyncStorage from '@react-native-async-storage/async-storage';
import vi from './locales/vi.json';
import en from './locales/en.json';

const LANG_STORAGE_KEY = 'userLanguage';
const resources = { vi: { translation: vi }, en: { translation: en } };

const deviceLocale = getLocales()[0]?.languageCode ?? 'vi';
const fallbackLocale = deviceLocale === 'en' ? 'en' : 'vi';

i18n.use(initReactI18next).init({
    resources,
    lng: fallbackLocale,
    fallbackLng: 'vi',
    interpolation: { escapeValue: false },
    returnNull: false,
});

export async function loadSavedLanguage(): Promise<void> {
    try {
        const saved = await AsyncStorage.getItem(LANG_STORAGE_KEY);
        if (saved && (saved === 'vi' || saved === 'en')) {
            await i18n.changeLanguage(saved);
        }
    } catch {
        // Silently fall back to detected locale
    }
}

export async function switchLanguage(lang: 'vi' | 'en'): Promise<void> {
    await i18n.changeLanguage(lang);
    await AsyncStorage.setItem(LANG_STORAGE_KEY, lang);
}

export default i18n;
