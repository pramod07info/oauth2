package com.pramod.oauth2.service;

import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.repository.UserRepository;
import com.pramod.oauth2.repository.UserRepository.Account;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {
  private final UserRepository users;
  private final PasswordEncoder passwords;

  public UserService(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  public static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public static void validatePassword(String password) {
    if (password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Password must be at least 12 characters and at most 72 UTF-8 bytes");
  }

  public View register(Registration req) {
    return create(req, Role.USER, "registration");
  }

  public View create(Registration req, Role role, String actor) {
    validatePassword(req.password());
    UUID id = UUID.randomUUID();
    users.create(
        id,
        normalize(req.email()),
        passwords.encode(req.password()),
        req.displayName().trim(),
        role);
    users.audit(actor, "CREATE", id);
    return get(id);
  }

  @Transactional(readOnly = true)
  public Account account(UUID id) {
    return users
        .byId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    return account(id).view();
  }

  @Transactional(readOnly = true)
  public Page list(int page, int size) {
    return new Page(users.list(page, size), page, size, users.count());
  }

  public View update(UUID id, Update req, String actor) {
    get(id);
    if (users.update(id, req) != 1) conflict();
    users.audit(actor, "UPDATE", id);
    return get(id);
  }

  public View permissions(UUID id, Permissions req, UUID actor) {
    if (id.equals(actor))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Administrators cannot change their own permissions");
    View old = get(id);
    if (users.permissions(id, req) != 1) conflict();
    users.revoke(old.email());
    users.audit(actor.toString(), "PERMISSIONS", id);
    return get(id);
  }

  public void changePassword(UUID id, PasswordChange req) {
    validatePassword(req.newPassword());
    Account old = account(id);
    if (!passwords.matches(req.currentPassword(), old.passwordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    users.password(id, passwords.encode(req.newPassword()));
    users.revoke(old.view().email());
    users.audit(id.toString(), "PASSWORD_CHANGE", id);
  }

  public void delete(UUID id, UUID actor) {
    if (id.equals(actor))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Administrators cannot delete their own account");
    View old = get(id);
    users.revoke(old.email());
    users.delete(id);
    users.audit(actor.toString(), "DELETE", id);
  }

  private static void conflict() {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "User changed; reload and retry");
  }
}
