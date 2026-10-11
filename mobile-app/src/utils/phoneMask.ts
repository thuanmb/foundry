/**
 * Mask a phone number for display.
 * Examples:
 *   +84912345678 → +84912****78
 *   +14155551234 → +1415***234
 *   0912345678   → 0912***678
 */
export function maskPhoneNumber(phone: string): string {
    if (phone.length <= 6) return phone;
    const prefix = phone.slice(0, Math.min(6, phone.length - 3));
    const suffix = phone.slice(-2);
    const maskedLength = phone.length - prefix.length - suffix.length;
    return prefix + '*'.repeat(Math.max(1, maskedLength)) + suffix;
}
