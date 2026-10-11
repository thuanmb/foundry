/**
 * Checks if the JWE token exists and has not expired.
 */
export function isTokenValid(jweToken, expiresAt) {
    if (!jweToken || !expiresAt) return false;
    return new Date(expiresAt) > new Date();
}
