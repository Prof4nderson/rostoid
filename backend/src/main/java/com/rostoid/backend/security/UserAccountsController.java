package com.rostoid.backend.security;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/users")
public class UserAccountsController {
    private final SecurityService service;
    public UserAccountsController(SecurityService service) { this.service = service; }
    @GetMapping public List<UserAccountView> list() { return service.listUsers(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UserAccountView create(@Valid @RequestBody UserRequest request) { try { return service.createUser(request.username(), request.password()); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); } }
    @PutMapping("/{id}") public UserAccountView update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) { try { return service.updateUser(id, request.username(), request.password()); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); } }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { try { service.deleteUser(id); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); } }
    public record UserRequest(@NotBlank @Size(min=3,max=80) String username, @NotBlank @Size(min=12,max=200) String password) {}
    public record UpdateUserRequest(@Size(min=3,max=80) String username, @Size(min=12,max=200) String password) {}
}
