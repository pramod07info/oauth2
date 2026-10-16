package com.pramod.oauth2.service.persistence;

import com.pramod.oauth2.entity.OAuthAuthorization;
import com.pramod.oauth2.repository.OAuthAuthorizationJpaRepository;
import java.util.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;

@Component
@Transactional
public class JpaAuthorizationService implements OAuth2AuthorizationService {
  private final OAuthAuthorizationJpaRepository authorizations;
  private final RegisteredClientRepository clients;
  private final OAuthJson json;

  public JpaAuthorizationService(
      OAuthAuthorizationJpaRepository authorizations,
      RegisteredClientRepository clients,
      OAuthJson json) {
    this.authorizations = authorizations;
    this.clients = clients;
    this.json = json;
  }

  @Override
  public void save(OAuth2Authorization a) {
    Assert.notNull(a, "Authorization is required");
    OAuthAuthorization e = authorizations.findById(a.getId()).orElseGet(OAuthAuthorization::new);
    e.setId(a.getId());
    e.setRegisteredClientId(a.getRegisteredClientId());
    e.setPrincipalName(a.getPrincipalName());
    e.setAuthorizationGrantType(a.getAuthorizationGrantType().getValue());
    e.setAuthorizedScopes(String.join(",", a.getAuthorizedScopes()));
    e.setAttributes(json.write(a.getAttributes()));
    e.setState(a.getAttribute("state"));
    var authorizationCode = a.getToken(OAuth2AuthorizationCode.class);
    e.setAuthorizationCodeValue(
        authorizationCode == null ? null : authorizationCode.getToken().getTokenValue());
    e.setAuthorizationCodeIssuedAt(
        authorizationCode == null ? null : authorizationCode.getToken().getIssuedAt());
    e.setAuthorizationCodeExpiresAt(
        authorizationCode == null ? null : authorizationCode.getToken().getExpiresAt());
    e.setAuthorizationCodeMetadata(
        authorizationCode == null ? null : json.write(authorizationCode.getMetadata()));
    var accessToken = a.getToken(OAuth2AccessToken.class);
    e.setAccessTokenValue(accessToken == null ? null : accessToken.getToken().getTokenValue());
    e.setAccessTokenIssuedAt(accessToken == null ? null : accessToken.getToken().getIssuedAt());
    e.setAccessTokenExpiresAt(accessToken == null ? null : accessToken.getToken().getExpiresAt());
    e.setAccessTokenMetadata(accessToken == null ? null : json.write(accessToken.getMetadata()));
    var oidcIdToken = a.getToken(OidcIdToken.class);
    e.setOidcIdTokenValue(oidcIdToken == null ? null : oidcIdToken.getToken().getTokenValue());
    e.setOidcIdTokenIssuedAt(oidcIdToken == null ? null : oidcIdToken.getToken().getIssuedAt());
    e.setOidcIdTokenExpiresAt(oidcIdToken == null ? null : oidcIdToken.getToken().getExpiresAt());
    e.setOidcIdTokenMetadata(oidcIdToken == null ? null : json.write(oidcIdToken.getMetadata()));
    var refreshToken = a.getToken(OAuth2RefreshToken.class);
    e.setRefreshTokenValue(refreshToken == null ? null : refreshToken.getToken().getTokenValue());
    e.setRefreshTokenIssuedAt(refreshToken == null ? null : refreshToken.getToken().getIssuedAt());
    e.setRefreshTokenExpiresAt(
        refreshToken == null ? null : refreshToken.getToken().getExpiresAt());
    e.setRefreshTokenMetadata(refreshToken == null ? null : json.write(refreshToken.getMetadata()));
    var userCode = a.getToken(OAuth2UserCode.class);
    e.setUserCodeValue(userCode == null ? null : userCode.getToken().getTokenValue());
    e.setUserCodeIssuedAt(userCode == null ? null : userCode.getToken().getIssuedAt());
    e.setUserCodeExpiresAt(userCode == null ? null : userCode.getToken().getExpiresAt());
    e.setUserCodeMetadata(userCode == null ? null : json.write(userCode.getMetadata()));
    var deviceCode = a.getToken(OAuth2DeviceCode.class);
    e.setDeviceCodeValue(deviceCode == null ? null : deviceCode.getToken().getTokenValue());
    e.setDeviceCodeIssuedAt(deviceCode == null ? null : deviceCode.getToken().getIssuedAt());
    e.setDeviceCodeExpiresAt(deviceCode == null ? null : deviceCode.getToken().getExpiresAt());
    e.setDeviceCodeMetadata(deviceCode == null ? null : json.write(deviceCode.getMetadata()));
    e.setAccessTokenType(
        a.getAccessToken() == null
            ? null
            : a.getAccessToken().getToken().getTokenType().getValue());
    e.setAccessTokenScopes(
        a.getAccessToken() == null
            ? null
            : String.join(",", a.getAccessToken().getToken().getScopes()));
    authorizations.saveAndFlush(e);
  }

  @Override
  public void remove(OAuth2Authorization a) {
    Assert.notNull(a, "Authorization is required");
    authorizations.deleteById(a.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "Authorization ID is required");
    return authorizations.findById(id).map(this::model).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public OAuth2Authorization findByToken(String token, OAuth2TokenType type) {
    Assert.hasText(token, "Token is required");
    Optional<OAuthAuthorization> result;
    if (type == null) {
      result =
          authorizations
              .findByState(token)
              .or(() -> authorizations.findByAuthorizationCodeValue(token))
              .or(() -> authorizations.findByAccessTokenValue(token))
              .or(() -> authorizations.findByRefreshTokenValue(token))
              .or(() -> authorizations.findByOidcIdTokenValue(token))
              .or(() -> authorizations.findByUserCodeValue(token))
              .or(() -> authorizations.findByDeviceCodeValue(token));
    } else
      result =
          switch (type.getValue()) {
            case "state" -> authorizations.findByState(token);
            case "code" -> authorizations.findByAuthorizationCodeValue(token);
            case "access_token" -> authorizations.findByAccessTokenValue(token);
            case "refresh_token" -> authorizations.findByRefreshTokenValue(token);
            case "id_token" -> authorizations.findByOidcIdTokenValue(token);
            case "user_code" -> authorizations.findByUserCodeValue(token);
            case "device_code" -> authorizations.findByDeviceCodeValue(token);
            default -> Optional.empty();
          };
    return result.map(this::model).orElse(null);
  }

  private OAuth2Authorization model(OAuthAuthorization e) {
    RegisteredClient client = clients.findById(e.getRegisteredClientId());
    if (client == null)
      throw new IllegalStateException("Authorization references a missing client");
    var builder =
        OAuth2Authorization.withRegisteredClient(client)
            .id(e.getId())
            .principalName(e.getPrincipalName())
            .authorizationGrantType(new AuthorizationGrantType(e.getAuthorizationGrantType()))
            .authorizedScopes(StringUtils.commaDelimitedListToSet(e.getAuthorizedScopes()))
            .attributes(a -> a.putAll(json.read(e.getAttributes())));
    if (StringUtils.hasText(e.getState())) builder.attribute("state", e.getState());
    if (StringUtils.hasText(e.getAuthorizationCodeValue()))
      builder.token(
          new OAuth2AuthorizationCode(
              e.getAuthorizationCodeValue(),
              e.getAuthorizationCodeIssuedAt(),
              e.getAuthorizationCodeExpiresAt()),
          m -> m.putAll(json.read(e.getAuthorizationCodeMetadata())));
    if (StringUtils.hasText(e.getAccessTokenValue()))
      builder.token(
          new OAuth2AccessToken(
              OAuth2AccessToken.TokenType.BEARER,
              e.getAccessTokenValue(),
              e.getAccessTokenIssuedAt(),
              e.getAccessTokenExpiresAt(),
              StringUtils.commaDelimitedListToSet(e.getAccessTokenScopes())),
          m -> m.putAll(json.read(e.getAccessTokenMetadata())));
    if (StringUtils.hasText(e.getOidcIdTokenValue()))
      builder.token(
          new OidcIdToken(
              e.getOidcIdTokenValue(),
              e.getOidcIdTokenIssuedAt(),
              e.getOidcIdTokenExpiresAt(),
              idClaims(json.read(e.getOidcIdTokenMetadata()))),
          m -> m.putAll(json.read(e.getOidcIdTokenMetadata())));
    if (StringUtils.hasText(e.getRefreshTokenValue()))
      builder.token(
          new OAuth2RefreshToken(
              e.getRefreshTokenValue(), e.getRefreshTokenIssuedAt(), e.getRefreshTokenExpiresAt()),
          m -> m.putAll(json.read(e.getRefreshTokenMetadata())));
    if (StringUtils.hasText(e.getUserCodeValue()))
      builder.token(
          new OAuth2UserCode(
              e.getUserCodeValue(), e.getUserCodeIssuedAt(), e.getUserCodeExpiresAt()),
          m -> m.putAll(json.read(e.getUserCodeMetadata())));
    if (StringUtils.hasText(e.getDeviceCodeValue()))
      builder.token(
          new OAuth2DeviceCode(
              e.getDeviceCodeValue(), e.getDeviceCodeIssuedAt(), e.getDeviceCodeExpiresAt()),
          m -> m.putAll(json.read(e.getDeviceCodeMetadata())));
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> idClaims(Map<String, Object> metadata) {
    Object claims = metadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
    if (!(claims instanceof Map<?, ?>))
      throw new IllegalStateException("ID token claims are missing");
    return (Map<String, Object>) claims;
  }
}
