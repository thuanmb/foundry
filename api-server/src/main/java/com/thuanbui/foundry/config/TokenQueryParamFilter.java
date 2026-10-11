package com.thuanbui.foundry.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class TokenQueryParamFilter implements Filter {

    private static final String TOKEN_PARAM = "token";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (httpRequest.getParameter(TOKEN_PARAM) != null) {
            request = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getQueryString() {
                    String qs = super.getQueryString();
                    if (qs != null) {
                        return qs.replaceAll("(?<=[&?])token=[^&]*", "token=REDACTED")
                                .replaceAll("^token=[^&]*", "token=REDACTED");
                    }
                    return null;
                }

                @Override
                public String getParameter(String name) {
                    if (TOKEN_PARAM.equals(name)) {
                        return "REDACTED";
                    }
                    return super.getParameter(name);
                }
            };
        }

        chain.doFilter(request, response);
    }
}
