package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.LoginSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from LoginSession s where s.id=:id")
  Optional<LoginSession> findLocked(@Param("id") UUID id);

  @Modifying
  @Query("delete from LoginSession s where s.userId=:id")
  void deleteByUserId(@Param("id") UUID id);

  @Modifying
  @Query("delete from LoginSession s where s.expiresAt<:now")
  void deleteExpired(@Param("now") Instant now);
}
