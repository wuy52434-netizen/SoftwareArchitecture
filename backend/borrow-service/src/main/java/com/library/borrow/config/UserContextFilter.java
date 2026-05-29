package com.library.borrow.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-User-Id");
        String username = httpRequest.getHeader("X-User-Name");
        String role = httpRequest.getHeader("X-User-Role");

        if (StringUtils.hasText(userId)) {
            httpRequest.setAttribute("userId", Long.parseLong(userId));
        }
        if (StringUtils.hasText(username)) {
            httpRequest.setAttribute("username", username);
        }
        if (StringUtils.hasText(role)) {
            httpRequest.setAttribute("role", role);
        }

        chain.doFilter(request, response);
    }
}
