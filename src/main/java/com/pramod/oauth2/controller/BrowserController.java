package com.pramod.oauth2.controller;

import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff")
@Validated
public class BrowserController {
  private final BrowserApiService api;
  private final UserService users;

  public BrowserController(BrowserApiService api, UserService users) {
    this.api = api;
    this.users = users;
  }

  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public View register(@Valid @RequestBody Registration body) {
    return users.register(body);
  }

  @GetMapping("/session")
  public Object session(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    return api.session(auth, req, res);
  }

  @GetMapping("/users/me")
  public Object me(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    return api.call(HttpMethod.GET, "/api/users/me", null, auth, req, res);
  }

  @PutMapping("/users/me")
  public Object updateMe(
      @Valid @RequestBody Update body,
      Authentication auth,
      HttpServletRequest req,
      HttpServletResponse res) {
    return api.call(HttpMethod.PUT, "/api/users/me", body, auth, req, res);
  }

  @PutMapping("/users/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(
      @Valid @RequestBody PasswordChange body,
      Authentication auth,
      HttpServletRequest req,
      HttpServletResponse res) {
    api.call(HttpMethod.PUT, "/api/users/me/password", body, auth, req, res);
  }

  @GetMapping("/users")
  public Object list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      Authentication auth,
      HttpServletRequest req,
      HttpServletResponse res) {
    return api.call(
        HttpMethod.GET, "/api/users?page=" + page + "&size=" + size, null, auth, req, res);
  }

  @GetMapping("/users/{id}")
  public Object view(
      @PathVariable UUID id, Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    return api.call(HttpMethod.GET, "/api/users/" + id, null, auth, req, res);
  }

  @PutMapping("/users/{id}")
  public Object update(
      @PathVariable UUID id,
      @Valid @RequestBody Update body,
      Authentication auth,
      HttpServletRequest req,
      HttpServletResponse res) {
    return api.call(HttpMethod.PUT, "/api/users/" + id, body, auth, req, res);
  }

  @PutMapping("/users/{id}/permissions")
  public Object permissions(
      @PathVariable UUID id,
      @Valid @RequestBody Permissions body,
      Authentication auth,
      HttpServletRequest req,
      HttpServletResponse res) {
    return api.call(HttpMethod.PUT, "/api/users/" + id + "/permissions", body, auth, req, res);
  }

  @DeleteMapping("/users/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable UUID id, Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    api.call(HttpMethod.DELETE, "/api/users/" + id, null, auth, req, res);
  }

  @PostMapping("/token/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refresh(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    api.refresh(auth, req, res);
  }

  @GetMapping("/token/info")
  public Object info(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    return api.inspect(auth, req, res);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    api.logout(auth, req, res);
  }
}
