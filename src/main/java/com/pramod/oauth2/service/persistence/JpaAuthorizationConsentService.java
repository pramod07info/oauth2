package com.pramod.oauth2.service.persistence;

import com.pramod.oauth2.entity.*;
import com.pramod.oauth2.repository.OAuthConsentJpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;

@Component
@Transactional
public class JpaAuthorizationConsentService implements OAuth2AuthorizationConsentService {
  private final OAuthConsentJpaRepository consents;

  public JpaAuthorizationConsentService(OAuthConsentJpaRepository consents) {
    this.consents = consents;
  }

  @Override
  public void save(OAuth2AuthorizationConsent c) {
    Assert.notNull(c, "Consent is required");
    OAuthConsent e = new OAuthConsent();
    e.setRegisteredClientId(c.getRegisteredClientId());
    e.setPrincipalName(c.getPrincipalName());
    e.setAuthorities(
        String.join(",", c.getAuthorities().stream().map(a -> a.getAuthority()).toList()));
    consents.saveAndFlush(e);
  }

  @Override
  public void remove(OAuth2AuthorizationConsent c) {
    Assert.notNull(c, "Consent is required");
    consents.deleteById(new OAuthConsentId(c.getRegisteredClientId(), c.getPrincipalName()));
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2AuthorizationConsent findById(String client, String principal) {
    Assert.hasText(client, "Client is required");
    Assert.hasText(principal, "Principal is required");
    return consents
        .findById(new OAuthConsentId(client, principal))
        .map(
            e -> {
              var builder = OAuth2AuthorizationConsent.withId(client, principal);
              StringUtils.commaDelimitedListToSet(e.getAuthorities())
                  .forEach(a -> builder.authority(new SimpleGrantedAuthority(a)));
              return builder.build();
            })
        .orElse(null);
  }
}
