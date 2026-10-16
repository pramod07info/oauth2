package com.pramod.oauth2.service.persistence;

import com.pramod.oauth2.entity.OAuthClient;
import com.pramod.oauth2.repository.OAuthClientJpaRepository;
import java.time.Instant;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;

@Component
@Transactional
public class JpaRegisteredClientRepository implements RegisteredClientRepository {
  private final OAuthClientJpaRepository clients;
  private final OAuthJson json;

  public JpaRegisteredClientRepository(OAuthClientJpaRepository clients, OAuthJson json) {
    this.clients = clients;
    this.json = json;
  }

  @Override
  public void save(RegisteredClient c) {
    Assert.notNull(c, "Client is required");
    OAuthClient e = clients.findById(c.getId()).orElseGet(OAuthClient::new);
    e.setId(c.getId());
    e.setClientId(c.getClientId());
    e.setClientIdIssuedAt(
        c.getClientIdIssuedAt() == null ? Instant.now() : c.getClientIdIssuedAt());
    e.setClientSecret(c.getClientSecret());
    e.setClientSecretExpiresAt(c.getClientSecretExpiresAt());
    e.setClientName(c.getClientName());
    e.setClientAuthenticationMethods(
        String.join(
            ",",
            c.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .toList()));
    e.setAuthorizationGrantTypes(
        String.join(
            ",",
            c.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .toList()));
    e.setRedirectUris(String.join(",", c.getRedirectUris()));
    e.setPostLogoutRedirectUris(String.join(",", c.getPostLogoutRedirectUris()));
    e.setScopes(String.join(",", c.getScopes()));
    e.setClientSettings(json.write(c.getClientSettings().getSettings()));
    e.setTokenSettings(json.write(c.getTokenSettings().getSettings()));
    clients.saveAndFlush(e);
  }

  @Override
  @Transactional(readOnly = true)
  public RegisteredClient findById(String id) {
    Assert.hasText(id, "Client ID is required");
    return clients.findById(id).map(this::model).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public RegisteredClient findByClientId(String id) {
    Assert.hasText(id, "Client ID is required");
    return clients.findByClientId(id).map(this::model).orElse(null);
  }

  private RegisteredClient model(OAuthClient e) {
    return RegisteredClient.withId(e.getId())
        .clientId(e.getClientId())
        .clientIdIssuedAt(e.getClientIdIssuedAt())
        .clientSecret(e.getClientSecret())
        .clientSecretExpiresAt(e.getClientSecretExpiresAt())
        .clientName(e.getClientName())
        .clientAuthenticationMethods(
            s ->
                StringUtils.commaDelimitedListToSet(e.getClientAuthenticationMethods())
                    .forEach(v -> s.add(new ClientAuthenticationMethod(v))))
        .authorizationGrantTypes(
            s ->
                StringUtils.commaDelimitedListToSet(e.getAuthorizationGrantTypes())
                    .forEach(v -> s.add(new AuthorizationGrantType(v))))
        .redirectUris(s -> s.addAll(StringUtils.commaDelimitedListToSet(e.getRedirectUris())))
        .postLogoutRedirectUris(
            s -> s.addAll(StringUtils.commaDelimitedListToSet(e.getPostLogoutRedirectUris())))
        .scopes(s -> s.addAll(StringUtils.commaDelimitedListToSet(e.getScopes())))
        .clientSettings(ClientSettings.withSettings(json.read(e.getClientSettings())).build())
        .tokenSettings(TokenSettings.withSettings(json.read(e.getTokenSettings())).build())
        .build();
  }
}
