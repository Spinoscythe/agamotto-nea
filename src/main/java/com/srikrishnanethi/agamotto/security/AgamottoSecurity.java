package com.srikrishnanethi.agamotto.security;

import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AgamottoSecurity {
    private AgamottoSecurity() {
    }

    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null | auth.getPrincipal() == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Authentication required");
        }
        var principal = auth.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new ForbiddenException("Authentication required");
        }
        return userId;
    }
}
