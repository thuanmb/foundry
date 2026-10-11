import { colors, darkColors, lightColors } from '../colors';
import { typography } from '../typography';
import { spacing, radius } from '../spacing';

describe('colors', () => {
    it('uses indigo as the primary color in both schemes', () => {
        expect(darkColors.primary).toBe('#4F46E5');
        expect(lightColors.primary).toBe('#4F46E5');
    });

    it('defines the same tokens for light and dark schemes', () => {
        expect(Object.keys(lightColors).sort()).toEqual(Object.keys(darkColors).sort());
    });

    it('exposes a default scheme', () => {
        expect(colors.background).toBeDefined();
        expect(colors.text).toBeDefined();
        expect(colors.error).toBeDefined();
    });
});

describe('typography', () => {
    it('uses system fonts (no custom fontFamily)', () => {
        Object.values(typography).forEach((style) => {
            expect(style).not.toHaveProperty('fontFamily');
        });
    });

    it('has a button style', () => {
        expect(typography.button.fontWeight).toBe('600');
    });
});

describe('spacing', () => {
    it('should have defined scale', () => {
        expect(spacing.xs).toBe(4);
        expect(spacing.lg).toBe(16);
        expect(spacing.xxxl).toBe(48);
    });
});

describe('radius', () => {
    it('should have full radius for pills', () => {
        expect(radius.full).toBe(9999);
        expect(radius.lg).toBe(12);
    });
});
