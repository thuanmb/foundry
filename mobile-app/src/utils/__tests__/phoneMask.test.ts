import { maskPhoneNumber } from '../phoneMask';

describe('maskPhoneNumber', () => {
    it('masks a standard Vietnamese mobile number', () => {
        expect(maskPhoneNumber('+84912345678')).toBe('+84912****78');
    });

    it('masks a domestic Vietnamese number', () => {
        expect(maskPhoneNumber('0912345678')).toBe('091234**78');
    });

    it('masks a US number', () => {
        expect(maskPhoneNumber('+14155551234')).toBe('+14155****34');
    });

    it('returns short numbers unchanged if too short to mask', () => {
        expect(maskPhoneNumber('123456')).toBe('123456');
    });

    it('handles empty string', () => {
        expect(maskPhoneNumber('')).toBe('');
    });
});
