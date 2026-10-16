package com.pramod.oauth2.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class UserDtos {
  private UserDtos() {}

  public enum Role {
    USER,
    ADMIN
  }

  public record Registration(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Size(max = 100) String displayName) {}

  public record Update(@NotBlank @Size(max = 100) String displayName, @Min(0) long version) {}

  public record Permissions(@NotNull Role role, boolean enabled, @Min(0) long version) {}

  public record PasswordChange(
      @NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}

  public record View(
      UUID id,
      String email,
      String displayName,
      Role role,
      boolean enabled,
      long version,
      Instant createdAt,
      Instant updatedAt) {}

  public record Page(List<View> content, int page, int size, long totalElements) {}
}
