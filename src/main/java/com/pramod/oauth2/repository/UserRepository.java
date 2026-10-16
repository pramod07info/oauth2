package com.pramod.oauth2.repository;

import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserRepository {
  private final AppUserJpaRepository users;
  private final UserAuditJpaRepository audits;
  private final OAuthAuthorizationJpaRepository authorizations;
  private final OAuthConsentJpaRepository consents;
  private final LoginSessionRepository loginSessions;

  public UserRepository(
      AppUserJpaRepository users,
      UserAuditJpaRepository audits,
      OAuthAuthorizationJpaRepository authorizations,
      OAuthConsentJpaRepository consents,
      LoginSessionRepository loginSessions) {
    this.users = users;
    this.audits = audits;
    this.authorizations = authorizations;
    this.consents = consents;
    this.loginSessions = loginSessions;
  }

  public record Account(View view, String passwordHash, Instant lockedUntil) {}

  private View view(AppUser u) {
    return new View(
        u.getId(),
        u.getEmail(),
        u.getDisplayName(),
        u.getRole(),
        u.getEnabled(),
        u.getVersion(),
        u.getCreatedAt(),
        u.getUpdatedAt());
  }

  private Account account(AppUser u) {
    return new Account(view(u), u.getPasswordHash(), u.getLockedUntil());
  }

  @Transactional(readOnly = true)
  public Optional<Account> byEmail(String email) {
    return users.findByEmail(email).map(this::account);
  }

  @Transactional(readOnly = true)
  public Optional<Account> byId(UUID id) {
    return users.findById(id).map(this::account);
  }

  public void create(UUID id, String email, String hash, String name, Role role) {
    AppUser user = new AppUser();
    user.setId(id);
    user.setEmail(email);
    user.setPasswordHash(hash);
    user.setDisplayName(name);
    user.setRole(role);
    user.setEnabled(true);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(user.getCreatedAt());
    users.saveAndFlush(user);
  }

  @Transactional(readOnly = true)
  public List<View> list(int page, int size) {
    return users.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id"))).stream()
        .map(this::view)
        .toList();
  }

  @Transactional(readOnly = true)
  public long count() {
    return users.count();
  }

  public int update(UUID id, Update req) {
    AppUser user = users.findById(id).orElse(null);
    if (user == null || user.getVersion() != req.version()) return 0;
    user.setDisplayName(req.displayName().trim());
    user.setUpdatedAt(Instant.now());
    users.flush();
    return 1;
  }

  public int permissions(UUID id, Permissions req) {
    AppUser user = users.findById(id).orElse(null);
    if (user == null || user.getVersion() != req.version()) return 0;
    user.setRole(req.role());
    user.setEnabled(req.enabled());
    user.setUpdatedAt(Instant.now());
    users.flush();
    return 1;
  }

  public void password(UUID id, String hash) {
    AppUser user = users.findById(id).orElseThrow();
    user.setPasswordHash(hash);
    user.setUpdatedAt(Instant.now());
    users.flush();
  }

  public void delete(UUID id) {
    users.deleteById(id);
    users.flush();
  }

  public void audit(String actor, String action, UUID id) {
    UserAudit event = new UserAudit();
    event.setId(UUID.randomUUID());
    event.setActor(actor);
    event.setAction(action);
    event.setTargetId(id);
    event.setOccurredAt(Instant.now());
    audits.save(event);
  }

  public void revoke(String email) {
    users.findByEmail(email).ifPresent(user -> loginSessions.deleteByUserId(user.getId()));
    consents.deleteByPrincipalName(email);
    authorizations.deleteByPrincipalName(email);
  }

  public void failedLogin(String email) {
    users.recordFailure(email, Instant.now().plusSeconds(900));
  }

  public void successfulLogin(String email) {
    users.clearFailures(email);
  }
}
