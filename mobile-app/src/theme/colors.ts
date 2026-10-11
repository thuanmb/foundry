const palette = {
    indigo50: '#EEF2FF',
    indigo400: '#818CF8',
    indigo500: '#6366F1',
    indigo600: '#4F46E5',
    gray50: '#F9FAFB',
    gray100: '#F3F4F6',
    gray200: '#E5E7EB',
    gray300: '#D1D5DB',
    gray400: '#9CA3AF',
    gray500: '#6B7280',
    gray600: '#4B5563',
    gray700: '#374151',
    gray800: '#1F2937',
    gray900: '#111827',
    gray950: '#030712',
    white: '#FFFFFF',
    red400: '#F87171',
    red600: '#DC2626',
    green400: '#4ADE80',
    green600: '#16A34A',
    amber400: '#FBBF24',
} as const;

export interface ColorScheme {
    background: string;
    surface: string;
    surfaceMuted: string;
    border: string;
    primary: string;
    primaryMuted: string;
    onPrimary: string;
    text: string;
    textMuted: string;
    textSubtle: string;
    error: string;
    success: string;
    warning: string;
    tabBarBackground: string;
    tabBarBorder: string;
    overlay: string;
}

export const darkColors: ColorScheme = {
    background: palette.gray900,
    surface: palette.gray800,
    surfaceMuted: palette.gray950,
    border: palette.gray700,
    primary: palette.indigo600,
    primaryMuted: 'rgba(79, 70, 229, 0.16)',
    onPrimary: palette.white,
    text: palette.gray50,
    textMuted: palette.gray400,
    textSubtle: palette.gray500,
    error: palette.red400,
    success: palette.green400,
    warning: palette.amber400,
    tabBarBackground: palette.gray900,
    tabBarBorder: palette.gray800,
    overlay: 'rgba(0, 0, 0, 0.6)',
};

export const lightColors: ColorScheme = {
    background: palette.gray50,
    surface: palette.white,
    surfaceMuted: palette.gray100,
    border: palette.gray200,
    primary: palette.indigo600,
    primaryMuted: palette.indigo50,
    onPrimary: palette.white,
    text: palette.gray900,
    textMuted: palette.gray600,
    textSubtle: palette.gray400,
    error: palette.red600,
    success: palette.green600,
    warning: palette.amber400,
    tabBarBackground: palette.white,
    tabBarBorder: palette.gray200,
    overlay: 'rgba(17, 24, 39, 0.5)',
};

// The app ships with a single dark scheme; swap to `lightColors` (or wire useColorScheme) to change it.
export const colors: ColorScheme = darkColors;

export { palette };
