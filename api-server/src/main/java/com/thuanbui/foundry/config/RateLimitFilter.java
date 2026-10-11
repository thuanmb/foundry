package com.thuanbui.foundry.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxRequestsPerSec;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(SecurityProperties securityProperties) {
        this.maxRequestsPerSec = securityProperties.rateLimit().publicEndpointsPerSec();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (path.startsWith("/api/v1/turnstile") || path.startsWith("/api/v1/device")) {
            String clientIp = extractClientIp(httpRequest);
            if (!allowRequest(clientIp)) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/problem+json");
                httpResponse.getWriter().write(
                        "{\"type\":\"RATE_LIMITED\",\"title\":\"Quá nhiều yêu cầu\",\"status\":429}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean allowRequest(String clientIp) {
        Instant now = Instant.now();
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> new Bucket(now));

        synchronized (bucket) {
            long elapsedMs = now.toEpochMilli() - bucket.windowStart.toEpochMilli();
            if (elapsedMs > 1000) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            bucket.count++;
            return bucket.count <= maxRequestsPerSec;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Bucket {
        Instant windowStart;
        int count;

        Bucket(Instant start) {
            this.windowStart = start;
            this.count = 0;
        }
    }
}
