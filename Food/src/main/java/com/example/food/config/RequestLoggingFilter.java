package com.example.food.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Log all admin chat requests
        if (path != null && path.contains("/admin/chat")) {
            log.info("=== Admin Chat Request ===");
            log.info("Method: {}, Path: {}", method, path);
            log.info("Query String: {}", request.getQueryString());
            log.info("Remote Address: {}", request.getRemoteAddr());
            log.info("User Principal: {}", request.getUserPrincipal());
            log.info("Session ID: {}", request.getSession(false) != null ? request.getSession().getId() : "No session");

            if ("POST".equals(method)) {
                log.info("POST Request Body Length: {}", request.getContentLength());
                log.info("Content Type: {}", request.getContentType());
            }
        }

        filterChain.doFilter(request, response);

        // Log response
        if (path != null && path.contains("/admin/chat")) {
            log.info("Response Status: {} for {} {}", response.getStatus(), method, path);
        }
    }
}

