package com.pramod.oauth2.controller;

import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api")
public class UserController {
  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
  }

  @PostMapping("/auth/register")
  public ResponseEntity<View> register(@Valid @RequestBody Registration req) {
    View user = users.register(req);
    return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
  }

  @GetMapping("/users/me")
  public View me(Authentication auth) {
    return users.get(id(auth));
  }

  @PutMapping("/users/me")
  public View updateMe(Authentication auth, @Valid @RequestBody Update req) {
    return users.update(id(auth), req, auth.getName());
  }

  @PutMapping("/users/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(Authentication auth, @Valid @RequestBody PasswordChange req) {
    users.changePassword(id(auth), req);
  }

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public Page list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return users.list(page, size);
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public View get(@PathVariable UUID id) {
    return users.get(id);
  }

  @PutMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public View update(@PathVariable UUID id, @Valid @RequestBody Update req, Authentication auth) {
    return users.update(id, req, auth.getName());
  }

  @PutMapping("/users/{id}/permissions")
  @PreAuthorize("hasRole('ADMIN')")
  public View permissions(
      @PathVariable UUID id, @Valid @RequestBody Permissions req, Authentication auth) {
    return users.permissions(id, req, id(auth));
  }

  @DeleteMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication auth) {
    users.delete(id, id(auth));
  }

  private static UUID id(Authentication auth) {
    return UUID.fromString(auth.getName());
  }
}
