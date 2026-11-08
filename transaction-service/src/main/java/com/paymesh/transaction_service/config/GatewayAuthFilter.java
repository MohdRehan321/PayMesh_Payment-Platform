package com.paymesh.transaction_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${paymesh.gateway.secret}")
    private String gatewaySecret;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Allow H2 console only if you intentionally enable it.
        if (request.getRequestURI().startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret =
                request.getHeader("X-Gateway-Secret");

        String userId =
                request.getHeader("X-User-Id");

        if (providedSecret == null ||
                !MessageDigest.isEqual(
                        gatewaySecret.getBytes(StandardCharsets.UTF_8),
                        providedSecret.getBytes(StandardCharsets.UTF_8))) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid gateway credentials"
            );
            return;
        }

        if (userId == null || userId.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing authenticated user ID"
            );
            return;
        }

        try {
            Long authenticatedUserId = Long.valueOf(userId);

            if (authenticatedUserId <= 0) {
                throw new NumberFormatException("Invalid user ID");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUserId,
                            null,
                            Collections.emptyList()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (NumberFormatException ex) {
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid user ID"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
