package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.AppUser;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {
  Optional<AppUser> findByEmail(String email);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update AppUser u set u.failedAttempts=u.failedAttempts+1, u.lockedUntil=case when"
          + " u.failedAttempts>=4 then :until else u.lockedUntil end where u.email=:email")
  int recordFailure(@Param("email") String email, @Param("until") Instant until);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update AppUser u set u.failedAttempts=0,u.lockedUntil=null where u.email=:email")
  int clearFailures(@Param("email") String email);
}
