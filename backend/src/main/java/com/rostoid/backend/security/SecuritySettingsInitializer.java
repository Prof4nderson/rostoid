package com.rostoid.backend.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SecuritySettingsInitializer implements ApplicationRunner {
    private final SecurityService securityService;

    public SecuritySettingsInitializer(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        securityService.ensureInitialized();
    }
}
