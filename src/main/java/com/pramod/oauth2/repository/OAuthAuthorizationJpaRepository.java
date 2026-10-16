package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAuthorizationJpaRepository extends JpaRepository<OAuthAuthorization, String> {
  void deleteByPrincipalName(String principalName);

  Optional<OAuthAuthorization> findByState(String value);

  Optional<OAuthAuthorization> findByAuthorizationCodeValue(String value);

  Optional<OAuthAuthorization> findByAccessTokenValue(String value);

  Optional<OAuthAuthorization> findByRefreshTokenValue(String value);

  Optional<OAuthAuthorization> findByOidcIdTokenValue(String value);

  Optional<OAuthAuthorization> findByUserCodeValue(String value);

  Optional<OAuthAuthorization> findByDeviceCodeValue(String value);
}
