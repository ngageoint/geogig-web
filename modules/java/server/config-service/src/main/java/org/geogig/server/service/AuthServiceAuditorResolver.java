package org.geogig.server.service;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthServiceAuditorResolver implements AuditorAware<String> {

    public AuthServiceAuditorResolver() {
        log.info("AuthServiceAuditorResolver instantiated and to be used as AuditorAware");
    }

    public @Override Optional<String> getCurrentAuditor() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = context.getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        String name = null;
        if (principal instanceof String) {
            name = (String) principal;
        } else if (principal instanceof UserDetails) {
            name = ((UserDetails) principal).getUsername();
        }
        return Optional.ofNullable(name);
    }

}