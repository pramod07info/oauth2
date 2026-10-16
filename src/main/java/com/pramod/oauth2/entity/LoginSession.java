package com.pramod.oauth2.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_session")
public class LoginSession {
  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "refresh_hash", nullable = false, length = 64)
  private String refreshHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  public LoginSession() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID value) {
    userId = value;
  }

  public String getRefreshHash() {
    return refreshHash;
  }

  public void setRefreshHash(String value) {
    refreshHash = value;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant value) {
    expiresAt = value;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean value) {
    revoked = value;
  }
}
