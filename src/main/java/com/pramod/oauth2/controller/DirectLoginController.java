package com.pramod.oauth2.controller;

import com.pramod.oauth2.service.DirectLoginService;
import com.pramod.oauth2.service.DirectLoginService.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class DirectLoginController {
  public static final String COOKIE = "IDENTITY_REFRESH";

  public record LoginRequest(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 256) String password) {}

  private final DirectLoginService auth;
  private final boolean secure;

  public DirectLoginController(
      DirectLoginService auth,
      @Value("${server.servlet.session.cookie.secure:true}") boolean secure) {
    this.auth = auth;
    this.secure = secure;
  }

  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken csrf, HttpServletResponse response) {
    noCache(response);
    return csrf;
  }

  @PostMapping("/login")
  public TokenResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servlet,
      HttpServletResponse response) {
    var issued = auth.login(request.email(), request.password());
    if (servlet.getSession(false) != null) servlet.getSession(false).invalidate();
    return send(issued, response);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(
      @CookieValue(name = COOKIE, required = false) String cookie, HttpServletResponse response) {
    noCache(response);
    try {
      return send(auth.refresh(cookie), response);
    } catch (ResponseStatusException ex) {
      cookie(response, "", Duration.ZERO);
      throw ex;
    }
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @CookieValue(name = COOKIE, required = false) String token,
      HttpServletRequest request,
      HttpServletResponse response) {
    auth.logout(token);
    if (request.getSession(false) != null) request.getSession(false).invalidate();
    cookie(response, "", Duration.ZERO);
    noCache(response);
  }

  @GetMapping("/token-info")
  public Object info(JwtAuthenticationToken authentication, HttpServletResponse response) {
    noCache(response);
    var jwt = authentication.getToken();
    return Map.of(
        "active",
        true,
        "sub",
        jwt.getSubject(),
        "scope",
        jwt.getClaim("scope"),
        "expiresAt",
        jwt.getExpiresAt());
  }

  private TokenResponse send(DirectLoginService.Issued issued, HttpServletResponse response) {
    cookie(
        response,
        issued.refreshToken(),
        Duration.between(Instant.now(), issued.refreshExpiresAt()));
    noCache(response);
    return issued.response();
  }

  private void cookie(HttpServletResponse response, String value, Duration ttl) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(COOKIE, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/api/auth")
            .maxAge(ttl)
            .build()
            .toString());
  }

  private void noCache(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
  }
}
