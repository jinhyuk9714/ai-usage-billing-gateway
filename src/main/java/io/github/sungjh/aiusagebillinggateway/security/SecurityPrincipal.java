package io.github.sungjh.aiusagebillinggateway.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityPrincipal {

    private SecurityPrincipal() {
    }

    public static AuthenticatedUser currentUser() {
        Object principal = currentPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT authentication required");
    }

    public static AuthenticatedApiKey currentApiKey() {
        Object principal = currentPrincipal();
        if (principal instanceof AuthenticatedApiKey apiKey) {
            return apiKey;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key authentication required");
    }

    public static UUID currentUserId() {
        return currentUser().userId();
    }

    private static Object currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getPrincipal();
    }
}
