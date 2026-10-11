import { colors } from './colors';
import { typography } from './typography';

/** Shared header styling for the per-tab Stack navigators. */
export const stackScreenOptions = {
    headerStyle: { backgroundColor: colors.background },
    headerTintColor: colors.text,
    headerTitleStyle: { fontSize: typography.titleSmall.fontSize, fontWeight: typography.titleSmall.fontWeight },
    headerShadowVisible: false,
    contentStyle: { backgroundColor: colors.background },
} as const;
