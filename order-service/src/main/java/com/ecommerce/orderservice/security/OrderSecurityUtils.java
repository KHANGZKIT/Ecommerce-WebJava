package com.ecommerce.orderservice.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract the verified OrderPrincipal and authenticated userId
 * from the security context.
 */
public final class OrderSecurityUtils {

    private OrderSecurityUtils() {}

    public static OrderPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OrderPrincipal principal) {
            return principal;
        }
        throw new BadCredentialsException("Authenticated OrderPrincipal not found");
    }

    public static OrderPrincipal findAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OrderPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static long getAuthenticatedUserId() {
        return getAuthenticatedPrincipal().userId();
    }

    public static boolean isCurrentUserAdmin() {
        OrderPrincipal principal = findAuthenticatedPrincipal();
        return principal != null && principal.isAdmin();
    }
}
