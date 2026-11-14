package com.rostoid.backend.security;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController @RequestMapping("/api/security")
public class SecurityController {
    private final SecurityService service; private final String setupKey;
    public SecurityController(SecurityService service, @Value("${rostoid.setup-key:}") String setupKey) { this.service = service; this.setupKey = setupKey; }
    @GetMapping("/state") public SecurityService.SecurityState state() { return service.state(); }
    @PostMapping("/register") public SecurityService.AuthResult register(@Valid @RequestBody RegisterRequest request) {
        if (!setupKey.isBlank() && (request.bootstrapKey() == null || !MessageDigest.isEqual(setupKey.getBytes(StandardCharsets.UTF_8), request.bootstrapKey().getBytes(StandardCharsets.UTF_8)))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chave de provisionamento inválida");
        try { return service.register(request.username(), request.password()); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
    }
    @PostMapping("/login") public SecurityService.AuthResult login(@Valid @RequestBody LoginRequest request) { try { return service.login(request.username(), request.password()); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage()); } }
    public record RegisterRequest(@NotBlank @Size(min=3,max=80) String username, @NotBlank @Size(min=12,max=200) String password, @Size(max=1024) String bootstrapKey) {}
    public record LoginRequest(@NotBlank @Size(min=3,max=80) String username, @NotBlank @Size(min=12,max=200) String password) {}
}
