import type { TextStyle } from 'react-native';

// System fonts only (San Francisco on iOS, Roboto on Android).
const fontWeight = {
    regular: '400',
    medium: '500',
    semibold: '600',
    bold: '700',
} as const satisfies Record<string, TextStyle['fontWeight']>;

export const typography = {
    fontWeight,
    titleLarge: { fontSize: 28, fontWeight: fontWeight.bold, lineHeight: 34 },
    titleMedium: { fontSize: 22, fontWeight: fontWeight.semibold, lineHeight: 28 },
    titleSmall: { fontSize: 18, fontWeight: fontWeight.semibold, lineHeight: 24 },
    bodyLarge: { fontSize: 16, fontWeight: fontWeight.regular, lineHeight: 24 },
    bodyMedium: { fontSize: 14, fontWeight: fontWeight.regular, lineHeight: 20 },
    bodySmall: { fontSize: 12, fontWeight: fontWeight.regular, lineHeight: 16 },
    label: { fontSize: 14, fontWeight: fontWeight.medium, lineHeight: 20 },
    caption: { fontSize: 12, fontWeight: fontWeight.medium, lineHeight: 16, letterSpacing: 0.4 },
    button: { fontSize: 16, fontWeight: fontWeight.semibold, lineHeight: 20 },
} as const satisfies Record<string, TextStyle | typeof fontWeight>;
