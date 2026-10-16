package com.pramod.oauth2.service;

import com.pramod.oauth2.dto.UserDtos.View;
import com.pramod.oauth2.entity.LoginSession;
import com.pramod.oauth2.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DirectLoginService {
  public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, View user) {}

  public record Issued(TokenResponse response, String refreshToken, Instant refreshExpiresAt) {}

  private final AuthenticationManager authentication;
  private final AppUserJpaRepository accounts;
  private final UserRepository users;
  private final LoginSessionRepository sessions;
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final String issuer, audience;
  private final SecureRandom random = new SecureRandom();

  public DirectLoginService(
      AuthenticationManager authentication,
      AppUserJpaRepository accounts,
      UserRepository users,
      LoginSessionRepository sessions,
      JwtEncoder encoder,
      JwtDecoder decoder,
      @Value("${app.issuer}") String issuer,
      @Value("${app.audience}") String audience) {
    this.authentication = authentication;
    this.accounts = accounts;
    this.users = users;
    this.sessions = sessions;
    this.encoder = encoder;
    this.decoder = decoder;
    this.issuer = issuer;
    this.audience = audience;
  }

  // Keep account changes and session creation ordered; commit lockout counters on invalid
  // credentials.
  @Transactional(noRollbackFor = ResponseStatusException.class)
  public Issued login(String email, String password) {
    String normalized = UserService.normalize(email);
    accounts.findLockedByEmail(normalized);
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      users.failedLogin(normalized);
      throw invalid();
    }
    try {
      authentication.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
    } catch (AuthenticationException ex) {
      users.failedLogin(normalized);
      throw invalid();
    }
    users.successfulLogin(normalized);
    var user = users.byEmail(normalized).orElseThrow(DirectLoginService::invalid).view();
    LoginSession session = new LoginSession();
    session.setId(UUID.randomUUID());
    session.setUserId(user.id());
    session.setExpiresAt(Instant.now().plus(Duration.ofHours(8)));
    String refresh = secret(session.getId(), session.getExpiresAt());
    session.setRefreshHash(hash(refresh));
    sessions.saveAndFlush(session);
    return issue(session, refresh, user);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public Issued refresh(String raw) {
    LoginSession session =
        sessions.findLocked(sessionId(raw)).orElseThrow(DirectLoginService::invalid);
    if (session.isRevoked() || !session.getExpiresAt().isAfter(Instant.now())) throw invalid();
    if (!matches(raw, session.getRefreshHash())) {
      session.setRevoked(true);
      throw invalid();
    }
    var user = users.byId(session.getUserId()).orElseThrow(DirectLoginService::invalid).view();
    if (!user.enabled()) {
      session.setRevoked(true);
      throw invalid();
    }
    String next = secret(session.getId(), session.getExpiresAt());
    session.setRefreshHash(hash(next));
    return issue(session, next, user);
  }

  @Transactional
  public void logout(String raw) {
    if (raw == null) return;
    UUID id;
    try {
      id = sessionId(raw);
    } catch (ResponseStatusException ex) {
      return;
    }
    // A valid signed token identifies its family even if a concurrent refresh rotated it.
    sessions.findLocked(id).ifPresent(s -> s.setRevoked(true));
  }

  private Issued issue(LoginSession session, String raw, View user) {
    Instant now = Instant.now(), expiry = now.plusSeconds(300);
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(user.id().toString())
            .audience(List.of(audience))
            .issuedAt(now)
            .expiresAt(expiry)
            .id(UUID.randomUUID().toString())
            .claim("sid", session.getId().toString())
            .claim("scope", List.of("users.read", "users.write"))
            .claim("roles", List.of(user.role().name()))
            .claim("user_version", Long.toString(user.version()))
            .build();
    String access = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return new Issued(
        new TokenResponse(access, "Bearer", expiry, user), raw, session.getExpiresAt());
  }

  private String secret(UUID id, Instant expiry) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    var claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(id.toString())
            .audience(List.of("identity-refresh"))
            .issuedAt(Instant.now())
            .expiresAt(expiry)
            .id(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
            .claim("token_use", "refresh")
            .build();
    return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  private UUID sessionId(String raw) {
    if (raw == null || raw.length() > 4096) throw invalid();
    try {
      var jwt = decoder.decode(raw);
      if (!jwt.getAudience().contains("identity-refresh")
          || !"refresh".equals(jwt.getClaimAsString("token_use"))) throw invalid();
      return UUID.fromString(jwt.getSubject());
    } catch (JwtException | IllegalArgumentException ex) {
      throw invalid();
    }
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static boolean matches(String raw, String expected) {
    return MessageDigest.isEqual(
        hash(raw).getBytes(StandardCharsets.US_ASCII),
        expected.getBytes(StandardCharsets.US_ASCII));
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Invalid credentials or expired session");
  }

  @Scheduled(initialDelayString = "3600000", fixedDelayString = "3600000")
  @Transactional
  public void cleanup() {
    sessions.deleteExpired(Instant.now());
  }
}
