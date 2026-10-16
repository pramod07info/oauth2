package com.pramod.oauth2.repository;

import com.pramod.oauth2.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthConsentJpaRepository extends JpaRepository<OAuthConsent, OAuthConsentId> {
  void deleteByPrincipalName(String principalName);
}
