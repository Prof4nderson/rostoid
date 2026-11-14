package com.rostoid.backend.security;
import org.springframework.boot.ApplicationArguments; import org.springframework.boot.ApplicationRunner; import org.springframework.stereotype.Component;
@Component public class SecuritySettingsInitializer implements ApplicationRunner { private final SecurityService service; public SecuritySettingsInitializer(SecurityService service){this.service=service;} @Override public void run(ApplicationArguments args){service.ensureInitialized();} }
