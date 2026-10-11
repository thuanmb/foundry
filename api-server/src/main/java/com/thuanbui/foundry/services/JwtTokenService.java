package com.thuanbui.foundry.services;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.thuanbui.foundry.config.AuthProperties;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey signingKey;
    private final AuthProperties.AccessToken accessTokenConfig;

    public JwtTokenService(AuthProperties authProperties,
            @Value("${auth.access-token.secret-key:#{systemEnvironment['AUTH_JWT_SECRET_KEY']}}") String secret) {
        this.accessTokenConfig = authProperties.accessToken();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("AUTH_JWT_SECRET_KEY environment variable must be set");
        }
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenConfig.ttlMinutes(), ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(accessTokenConfig.issuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .build();

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT jwt = new SignedJWT(header, claims);

        try {
            jwt.sign(new MACSigner(signingKey));
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }

        return jwt.serialize();
    }

    public UUID validateAccessToken(String jwtString) {
        try {
            SignedJWT jwt = SignedJWT.parse(jwtString);
            if (!jwt.verify(new MACVerifier(signingKey))) {
                throw new TokenInvalidException("JWT signature verification failed");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration != null && expiration.toInstant().isBefore(Instant.now())) {
                throw new TokenExpiredException("JWT has expired");
            }

            return UUID.fromString(claims.getSubject());
        } catch (ParseException e) {
            throw new TokenInvalidException("Invalid JWT format", e);
        } catch (JOSEException e) {
            throw new TokenInvalidException("JWT verification failed", e);
        }
    }

    public Instant getAccessExpiresAt() {
        return Instant.now().plus(accessTokenConfig.ttlMinutes(), ChronoUnit.MINUTES);
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) {
            super(message);
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
