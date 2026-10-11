package com.thuanbui.foundry.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    static final String RID_KEY = "rid";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String rid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        response.setHeader("X-Request-Id", rid);
        try {
            MDC.put(RID_KEY, rid);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RID_KEY);
        }
    }
}
