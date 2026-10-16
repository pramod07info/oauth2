package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthClientJpaRepository extends JpaRepository<OAuthClient, String> {
  Optional<OAuthClient> findByClientId(String clientId);
}
