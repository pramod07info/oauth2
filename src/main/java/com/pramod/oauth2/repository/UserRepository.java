package com.pramod.oauth2.repository;

import com.pramod.oauth2.dto.UserDtos.*;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Account(View view, String passwordHash, java.time.Instant lockedUntil) {}

  private static final RowMapper<Account> MAPPER =
      (rs, n) ->
          new Account(
              new View(
                  rs.getObject("id", UUID.class),
                  rs.getString("email"),
                  rs.getString("display_name"),
                  Role.valueOf(rs.getString("role")),
                  rs.getBoolean("enabled"),
                  rs.getLong("version"),
                  rs.getTimestamp("created_at").toInstant(),
                  rs.getTimestamp("updated_at").toInstant()),
              rs.getString("password_hash"),
              rs.getTimestamp("locked_until") == null
                  ? null
                  : rs.getTimestamp("locked_until").toInstant());

  public Optional<Account> byEmail(String email) {
    return jdbc.query("SELECT * FROM app_user WHERE email=?", MAPPER, email).stream().findFirst();
  }

  public Optional<Account> byId(UUID id) {
    return jdbc.query("SELECT * FROM app_user WHERE id=?", MAPPER, id).stream().findFirst();
  }

  public void create(UUID id, String email, String hash, String name, Role role) {
    jdbc.update(
        "INSERT INTO app_user(id,email,password_hash,display_name,role) VALUES(?,?,?,?,?)",
        id,
        email,
        hash,
        name,
        role.name());
  }

  public List<View> list(int page, int size) {
    return jdbc
        .query(
            "SELECT * FROM app_user ORDER BY created_at,id LIMIT ? OFFSET ?",
            MAPPER,
            size,
            (long) page * size)
        .stream()
        .map(Account::view)
        .toList();
  }

  public long count() {
    return jdbc.queryForObject("SELECT count(*) FROM app_user", Long.class);
  }

  public int update(UUID id, Update req) {
    return jdbc.update(
        "UPDATE app_user SET display_name=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE"
            + " id=? AND version=?",
        req.displayName().trim(),
        id,
        req.version());
  }

  public int permissions(UUID id, Permissions req) {
    return jdbc.update(
        "UPDATE app_user SET role=?,enabled=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE"
            + " id=? AND version=?",
        req.role().name(),
        req.enabled(),
        id,
        req.version());
  }

  public void password(UUID id, String hash) {
    jdbc.update(
        "UPDATE app_user SET password_hash=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE"
            + " id=?",
        hash,
        id);
  }

  public void delete(UUID id) {
    jdbc.update("DELETE FROM app_user WHERE id=?", id);
  }

  public void audit(String actor, String action, UUID id) {
    jdbc.update(
        "INSERT INTO user_audit(id,actor,action,target_id) VALUES(?,?,?,?)",
        UUID.randomUUID(),
        actor,
        action,
        id);
  }

  public void revoke(String email) {
    jdbc.update("DELETE FROM oauth2_authorization_consent WHERE principal_name=?", email);
    jdbc.update("DELETE FROM oauth2_authorization WHERE principal_name=?", email);
  }

  public void failedLogin(String email) {
    jdbc.update(
        "UPDATE app_user SET failed_attempts=failed_attempts+1, locked_until=CASE WHEN"
            + " failed_attempts>=4 THEN ? ELSE locked_until END WHERE email=?",
        java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(15),
        email);
  }

  public void successfulLogin(String email) {
    jdbc.update("UPDATE app_user SET failed_attempts=0,locked_until=NULL WHERE email=?", email);
  }
}
