package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.security.AnonymousTokenClaims;
import com.thuanbui.foundry.security.TokenAuthenticationFilter;
import com.thuanbui.foundry.security.TokenClaims;
import com.thuanbui.foundry.services.JweTokenService;
import com.thuanbui.foundry.services.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JweTokenService jweTokenService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private PrintWriter writer;

    private TokenAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TokenAuthenticationFilter(jwtTokenService, jweTokenService);
    }

    @Test
    void shouldPassThrough_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassThrough_whenNotBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldValidateJwtAndSetClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenService.validateAccessToken(jwt)).thenReturn(userId);

        ArgumentCaptor<TokenClaims> claimsCaptor = ArgumentCaptor.forClass(TokenClaims.class);
        filter.doFilter(request, response, chain);

        verify(request).setAttribute(anyString(), claimsCaptor.capture());
        TokenClaims claims = claimsCaptor.getValue();
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.tokenType()).isEqualTo(TokenClaims.TokenType.JWT);
    }

    @Test
    void shouldValidateJweAndSetClaims() throws Exception {
        UUID anonymousId = UUID.randomUUID();
        String jwe = "eyJhbGci.eyJlbmMi.iv.ciphertext.tag";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwe);
        when(jweTokenService.validateToken(jwe))
                .thenReturn(new AnonymousTokenClaims(anonymousId, "WEB", Instant.now().plusSeconds(86400)));
        when(jweTokenService.hashToken(jwe)).thenReturn("abc123hash");

        ArgumentCaptor<TokenClaims> claimsCaptor = ArgumentCaptor.forClass(TokenClaims.class);
        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("tokenClaims"), claimsCaptor.capture());
        verify(request).setAttribute(eq("anonymousTokenHash"), eq("abc123hash"));
        TokenClaims claims = claimsCaptor.getValue();
        assertThat(claims.userId()).isEqualTo(anonymousId);
        assertThat(claims.tokenType()).isEqualTo(TokenClaims.TokenType.JWE);
    }

    @Test
    void shouldReturn401_whenJweIsInvalid() throws Exception {
        String jwe = "eyJhbGci.eyJlbmMi.iv.ciphertext.tag";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwe);
        when(jweTokenService.validateToken(jwe))
                .thenThrow(new JweTokenService.TokenInvalidException("Invalid"));
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
        verify(response).setContentType("application/problem+json");
    }

    @Test
    void shouldReturn401_whenJweIsExpired() throws Exception {
        String jwe = "eyJhbGci.eyJlbmMi.iv.ciphertext.tag";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwe);
        when(jweTokenService.validateToken(jwe))
                .thenThrow(new JweTokenService.TokenExpiredException());
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
    }

    @Test
    void shouldReturn401_whenJwtIsInvalid() throws Exception {
        String jwt = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenService.validateAccessToken(jwt))
                .thenThrow(new JwtTokenService.TokenInvalidException("Invalid"));
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
    }

    @Test
    void shouldReturn401_whenJwtIsExpired() throws Exception {
        String jwt = "expired.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtTokenService.validateAccessToken(jwt))
                .thenThrow(new JwtTokenService.TokenExpiredException("Expired"));
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(401);
    }
}
