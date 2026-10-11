package com.thuanbui.foundry.security;

import com.thuanbui.foundry.services.JweTokenService;
import com.thuanbui.foundry.services.JwtTokenService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class TokenAuthenticationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final JweTokenService jweTokenService;

    public TokenAuthenticationFilter(JwtTokenService jwtTokenService, JweTokenService jweTokenService) {
        this.jwtTokenService = jwtTokenService;
        this.jweTokenService = jweTokenService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = extractToken(httpRequest);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        if (isJwe(token)) {
            try {
                AnonymousTokenClaims anonClaims = jweTokenService.validateToken(token);
                TokenClaims claims = new TokenClaims(anonClaims.anonymousId(), TokenClaims.TokenType.JWE);
                httpRequest.setAttribute("tokenClaims", claims);
                String tokenHash = jweTokenService.hashToken(token);
                httpRequest.setAttribute("anonymousTokenHash", tokenHash);
            } catch (JweTokenService.TokenExpiredException | JweTokenService.TokenInvalidException e) {
                log.warn("JWE token validation failed: {}", e.getMessage());
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/problem+json");
                httpResponse.getWriter().write(
                        "{\"type\":\"TOKEN_INVALID\",\"title\":\"Token không hợp lệ\",\"status\":401}");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = jwtTokenService.validateAccessToken(token);
            TokenClaims claims = new TokenClaims(userId, TokenClaims.TokenType.JWT);
            httpRequest.setAttribute("tokenClaims", claims);
        } catch (JwtTokenService.TokenExpiredException | JwtTokenService.TokenInvalidException e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/problem+json");
            httpResponse.getWriter().write(
                    "{\"type\":\"TOKEN_INVALID\",\"title\":\"Token không hợp lệ\",\"status\":401}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isJwe(String token) {
        return token.chars().filter(ch -> ch == '.').count() == 4;
    }
}
