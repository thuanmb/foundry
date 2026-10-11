package com.thuanbui.foundry.services;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.github.f4b6a3.uuid.UuidCreator;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.security.AnonymousTokenClaims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JweTokenService {

    private static final Logger log = LoggerFactory.getLogger(JweTokenService.class);

    private final SecretKey encryptionKey;
    private final AuthProperties.Jwe jweProperties;

    public JweTokenService(AuthProperties authProperties) {
        this.jweProperties = authProperties.jwe();
        String secret = this.jweProperties.secretKey();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("AUTH_JWE_SECRET_KEY must be set");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("AUTH_JWE_SECRET_KEY must be at least 32 bytes when decoded");
        }
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String generateAnonymousToken(String platform) {
        return generateAnonymousToken(platform, UuidCreator.getTimeOrderedEpoch());
    }

    public String generateAnonymousToken(String platform, UUID anonymousId) {
        Instant now = Instant.now();
        Instant exp = now.plus(jweProperties.ttlHours(), ChronoUnit.HOURS);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(anonymousId.toString())
                .claim("platform", platform)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .build();

        JWEHeader header = new JWEHeader(JWEAlgorithm.A256KW, EncryptionMethod.A256GCM);
        JWEObject jwe = new JWEObject(header, claims.toPayload());

        try {
            jwe.encrypt(new AESEncrypter(encryptionKey));
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new RuntimeException("Failed to encrypt JWE", e);
        }

        return jwe.serialize();
    }

    public AnonymousTokenClaims validateToken(String jweToken) {
        try {
            JWEObject jwe = JWEObject.parse(jweToken);
            jwe.decrypt(new AESDecrypter(encryptionKey));

            JWTClaimsSet claims = JWTClaimsSet.parse(jwe.getPayload().toJSONObject());

            Date expiration = claims.getExpirationTime();
            if (expiration != null && expiration.toInstant().isBefore(Instant.now())) {
                throw new TokenExpiredException();
            }

            return new AnonymousTokenClaims(
                    UUID.fromString(claims.getSubject()),
                    claims.getStringClaim("platform"),
                    expiration != null ? expiration.toInstant() : null);
        } catch (ParseException e) {
            throw new TokenInvalidException("Invalid JWE format", e);
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new TokenInvalidException("JWE decryption failed", e);
        }
    }

    public String hashToken(String jweToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jweToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public Instant getAnonymousTokenExpiresAt() {
        return Instant.now().plus(jweProperties.ttlHours(), ChronoUnit.HOURS);
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException() {
            super("JWE token has expired");
        }
    }

    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message) {
            super(message);
        }

        public TokenInvalidException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
