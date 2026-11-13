package com.rostoid.backend.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/security")
public class SecurityController {
    private final SecurityService securityService;
    private final String expectedBootstrapKey;

    public SecurityController(SecurityService securityService,
                              @Value("${rostoid.setup-key:}") String expectedBootstrapKey) {
        this.securityService = securityService;
        this.expectedBootstrapKey = expectedBootstrapKey;
    }

    @GetMapping("/state")
    public SecurityService.SecurityState state() {
        return securityService.state();
    }

    @PostMapping("/setup")
    public OkResponse setup(@Valid @RequestBody SetupRequest request) {
        if (expectedBootstrapKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Provisionamento inicial desativado; configure ROSTOID_SETUP_KEY");
        }
        byte[] expected = expectedBootstrapKey.getBytes(StandardCharsets.UTF_8);
        byte[] supplied = request.bootstrapKey().getBytes(StandardCharsets.UTF_8);
        if (supplied.length < 24 || !MessageDigest.isEqual(expected, supplied)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de provisionamento inválida");
        }
        if (!securityService.setup(request.token())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Segurança já configurada");
        }
        return new OkResponse(true);
    }

    @PostMapping("/login")
    public OkResponse login(@Valid @RequestBody TokenRequest request) {
        if (!securityService.authenticate(request.token())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        return new OkResponse(true);
    }

    public record SetupRequest(@NotBlank @Size(max = 4096) String token,
                               @NotBlank @Size(min = 24, max = 1024) String bootstrapKey) {}
    public record TokenRequest(@NotBlank @Size(max = 4096) String token) {}
    public record OkResponse(boolean ok) {}
}
