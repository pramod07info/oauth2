package com.pramod.oauth2.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_audit")
public class UserAudit {
  @Id private UUID id;

  @Column(nullable = false, length = 254)
  private String actor;

  @Column(nullable = false, length = 32)
  private String action;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  public UserAudit() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    this.id = value;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String value) {
    this.actor = value;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String value) {
    this.action = value;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public void setTargetId(UUID value) {
    this.targetId = value;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant value) {
    this.occurredAt = value;
  }
}
