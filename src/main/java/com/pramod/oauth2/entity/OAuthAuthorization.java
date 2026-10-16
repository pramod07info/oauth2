package com.pramod.oauth2.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth2_authorization")
public class OAuthAuthorization {
  @Id
  @Column(name = "id", length = 100, nullable = false)
  private String id;

  @Column(name = "registered_client_id", length = 100, nullable = false)
  private String registeredClientId;

  @Column(name = "principal_name", length = 254, nullable = false)
  private String principalName;

  @Column(name = "authorization_grant_type", length = 100, nullable = false)
  private String authorizationGrantType;

  @Column(name = "authorized_scopes", length = 1000)
  private String authorizedScopes;

  @Column(name = "attributes", columnDefinition = "text")
  private String attributes;

  @Column(name = "state", length = 500)
  private String state;

  @Column(name = "authorization_code_value", columnDefinition = "text")
  private String authorizationCodeValue;

  @Column(name = "authorization_code_issued_at")
  private Instant authorizationCodeIssuedAt;

  @Column(name = "authorization_code_expires_at")
  private Instant authorizationCodeExpiresAt;

  @Column(name = "authorization_code_metadata", columnDefinition = "text")
  private String authorizationCodeMetadata;

  @Column(name = "access_token_value", columnDefinition = "text")
  private String accessTokenValue;

  @Column(name = "access_token_issued_at")
  private Instant accessTokenIssuedAt;

  @Column(name = "access_token_expires_at")
  private Instant accessTokenExpiresAt;

  @Column(name = "access_token_metadata", columnDefinition = "text")
  private String accessTokenMetadata;

  @Column(name = "access_token_type", length = 100)
  private String accessTokenType;

  @Column(name = "access_token_scopes", length = 1000)
  private String accessTokenScopes;

  @Column(name = "oidc_id_token_value", columnDefinition = "text")
  private String oidcIdTokenValue;

  @Column(name = "oidc_id_token_issued_at")
  private Instant oidcIdTokenIssuedAt;

  @Column(name = "oidc_id_token_expires_at")
  private Instant oidcIdTokenExpiresAt;

  @Column(name = "oidc_id_token_metadata", columnDefinition = "text")
  private String oidcIdTokenMetadata;

  @Column(name = "refresh_token_value", columnDefinition = "text")
  private String refreshTokenValue;

  @Column(name = "refresh_token_issued_at")
  private Instant refreshTokenIssuedAt;

  @Column(name = "refresh_token_expires_at")
  private Instant refreshTokenExpiresAt;

  @Column(name = "refresh_token_metadata", columnDefinition = "text")
  private String refreshTokenMetadata;

  @Column(name = "user_code_value", columnDefinition = "text")
  private String userCodeValue;

  @Column(name = "user_code_issued_at")
  private Instant userCodeIssuedAt;

  @Column(name = "user_code_expires_at")
  private Instant userCodeExpiresAt;

  @Column(name = "user_code_metadata", columnDefinition = "text")
  private String userCodeMetadata;

  @Column(name = "device_code_value", columnDefinition = "text")
  private String deviceCodeValue;

  @Column(name = "device_code_issued_at")
  private Instant deviceCodeIssuedAt;

  @Column(name = "device_code_expires_at")
  private Instant deviceCodeExpiresAt;

  @Column(name = "device_code_metadata", columnDefinition = "text")
  private String deviceCodeMetadata;

  public OAuthAuthorization() {}

  public String getId() {
    return id;
  }

  public void setId(String value) {
    this.id = value;
  }

  public String getRegisteredClientId() {
    return registeredClientId;
  }

  public void setRegisteredClientId(String value) {
    this.registeredClientId = value;
  }

  public String getPrincipalName() {
    return principalName;
  }

  public void setPrincipalName(String value) {
    this.principalName = value;
  }

  public String getAuthorizationGrantType() {
    return authorizationGrantType;
  }

  public void setAuthorizationGrantType(String value) {
    this.authorizationGrantType = value;
  }

  public String getAuthorizedScopes() {
    return authorizedScopes;
  }

  public void setAuthorizedScopes(String value) {
    this.authorizedScopes = value;
  }

  public String getAttributes() {
    return attributes;
  }

  public void setAttributes(String value) {
    this.attributes = value;
  }

  public String getState() {
    return state;
  }

  public void setState(String value) {
    this.state = value;
  }

  public String getAuthorizationCodeValue() {
    return authorizationCodeValue;
  }

  public void setAuthorizationCodeValue(String value) {
    this.authorizationCodeValue = value;
  }

  public Instant getAuthorizationCodeIssuedAt() {
    return authorizationCodeIssuedAt;
  }

  public void setAuthorizationCodeIssuedAt(Instant value) {
    this.authorizationCodeIssuedAt = value;
  }

  public Instant getAuthorizationCodeExpiresAt() {
    return authorizationCodeExpiresAt;
  }

  public void setAuthorizationCodeExpiresAt(Instant value) {
    this.authorizationCodeExpiresAt = value;
  }

  public String getAuthorizationCodeMetadata() {
    return authorizationCodeMetadata;
  }

  public void setAuthorizationCodeMetadata(String value) {
    this.authorizationCodeMetadata = value;
  }

  public String getAccessTokenValue() {
    return accessTokenValue;
  }

  public void setAccessTokenValue(String value) {
    this.accessTokenValue = value;
  }

  public Instant getAccessTokenIssuedAt() {
    return accessTokenIssuedAt;
  }

  public void setAccessTokenIssuedAt(Instant value) {
    this.accessTokenIssuedAt = value;
  }

  public Instant getAccessTokenExpiresAt() {
    return accessTokenExpiresAt;
  }

  public void setAccessTokenExpiresAt(Instant value) {
    this.accessTokenExpiresAt = value;
  }

  public String getAccessTokenMetadata() {
    return accessTokenMetadata;
  }

  public void setAccessTokenMetadata(String value) {
    this.accessTokenMetadata = value;
  }

  public String getAccessTokenType() {
    return accessTokenType;
  }

  public void setAccessTokenType(String value) {
    this.accessTokenType = value;
  }

  public String getAccessTokenScopes() {
    return accessTokenScopes;
  }

  public void setAccessTokenScopes(String value) {
    this.accessTokenScopes = value;
  }

  public String getOidcIdTokenValue() {
    return oidcIdTokenValue;
  }

  public void setOidcIdTokenValue(String value) {
    this.oidcIdTokenValue = value;
  }

  public Instant getOidcIdTokenIssuedAt() {
    return oidcIdTokenIssuedAt;
  }

  public void setOidcIdTokenIssuedAt(Instant value) {
    this.oidcIdTokenIssuedAt = value;
  }

  public Instant getOidcIdTokenExpiresAt() {
    return oidcIdTokenExpiresAt;
  }

  public void setOidcIdTokenExpiresAt(Instant value) {
    this.oidcIdTokenExpiresAt = value;
  }

  public String getOidcIdTokenMetadata() {
    return oidcIdTokenMetadata;
  }

  public void setOidcIdTokenMetadata(String value) {
    this.oidcIdTokenMetadata = value;
  }

  public String getRefreshTokenValue() {
    return refreshTokenValue;
  }

  public void setRefreshTokenValue(String value) {
    this.refreshTokenValue = value;
  }

  public Instant getRefreshTokenIssuedAt() {
    return refreshTokenIssuedAt;
  }

  public void setRefreshTokenIssuedAt(Instant value) {
    this.refreshTokenIssuedAt = value;
  }

  public Instant getRefreshTokenExpiresAt() {
    return refreshTokenExpiresAt;
  }

  public void setRefreshTokenExpiresAt(Instant value) {
    this.refreshTokenExpiresAt = value;
  }

  public String getRefreshTokenMetadata() {
    return refreshTokenMetadata;
  }

  public void setRefreshTokenMetadata(String value) {
    this.refreshTokenMetadata = value;
  }

  public String getUserCodeValue() {
    return userCodeValue;
  }

  public void setUserCodeValue(String value) {
    this.userCodeValue = value;
  }

  public Instant getUserCodeIssuedAt() {
    return userCodeIssuedAt;
  }

  public void setUserCodeIssuedAt(Instant value) {
    this.userCodeIssuedAt = value;
  }

  public Instant getUserCodeExpiresAt() {
    return userCodeExpiresAt;
  }

  public void setUserCodeExpiresAt(Instant value) {
    this.userCodeExpiresAt = value;
  }

  public String getUserCodeMetadata() {
    return userCodeMetadata;
  }

  public void setUserCodeMetadata(String value) {
    this.userCodeMetadata = value;
  }

  public String getDeviceCodeValue() {
    return deviceCodeValue;
  }

  public void setDeviceCodeValue(String value) {
    this.deviceCodeValue = value;
  }

  public Instant getDeviceCodeIssuedAt() {
    return deviceCodeIssuedAt;
  }

  public void setDeviceCodeIssuedAt(Instant value) {
    this.deviceCodeIssuedAt = value;
  }

  public Instant getDeviceCodeExpiresAt() {
    return deviceCodeExpiresAt;
  }

  public void setDeviceCodeExpiresAt(Instant value) {
    this.deviceCodeExpiresAt = value;
  }

  public String getDeviceCodeMetadata() {
    return deviceCodeMetadata;
  }

  public void setDeviceCodeMetadata(String value) {
    this.deviceCodeMetadata = value;
  }
}
