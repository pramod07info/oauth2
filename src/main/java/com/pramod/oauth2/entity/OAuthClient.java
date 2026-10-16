package com.pramod.oauth2.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth2_registered_client")
public class OAuthClient {
  @Id
  @Column(name = "id", length = 100, nullable = false)
  private String id;

  @Column(name = "client_id", length = 100, nullable = false)
  private String clientId;

  @Column(name = "client_id_issued_at", nullable = false)
  private Instant clientIdIssuedAt;

  @Column(name = "client_secret", length = 200)
  private String clientSecret;

  @Column(name = "client_secret_expires_at")
  private Instant clientSecretExpiresAt;

  @Column(name = "client_name", length = 200, nullable = false)
  private String clientName;

  @Column(name = "client_authentication_methods", length = 1000, nullable = false)
  private String clientAuthenticationMethods;

  @Column(name = "authorization_grant_types", length = 1000, nullable = false)
  private String authorizationGrantTypes;

  @Column(name = "redirect_uris", length = 1000)
  private String redirectUris;

  @Column(name = "post_logout_redirect_uris", length = 1000)
  private String postLogoutRedirectUris;

  @Column(name = "scopes", length = 1000, nullable = false)
  private String scopes;

  @Column(name = "client_settings", length = 2000, nullable = false)
  private String clientSettings;

  @Column(name = "token_settings", length = 2000, nullable = false)
  private String tokenSettings;

  public OAuthClient() {}

  public String getId() {
    return id;
  }

  public void setId(String value) {
    this.id = value;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String value) {
    this.clientId = value;
  }

  public Instant getClientIdIssuedAt() {
    return clientIdIssuedAt;
  }

  public void setClientIdIssuedAt(Instant value) {
    this.clientIdIssuedAt = value;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String value) {
    this.clientSecret = value;
  }

  public Instant getClientSecretExpiresAt() {
    return clientSecretExpiresAt;
  }

  public void setClientSecretExpiresAt(Instant value) {
    this.clientSecretExpiresAt = value;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String value) {
    this.clientName = value;
  }

  public String getClientAuthenticationMethods() {
    return clientAuthenticationMethods;
  }

  public void setClientAuthenticationMethods(String value) {
    this.clientAuthenticationMethods = value;
  }

  public String getAuthorizationGrantTypes() {
    return authorizationGrantTypes;
  }

  public void setAuthorizationGrantTypes(String value) {
    this.authorizationGrantTypes = value;
  }

  public String getRedirectUris() {
    return redirectUris;
  }

  public void setRedirectUris(String value) {
    this.redirectUris = value;
  }

  public String getPostLogoutRedirectUris() {
    return postLogoutRedirectUris;
  }

  public void setPostLogoutRedirectUris(String value) {
    this.postLogoutRedirectUris = value;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String value) {
    this.scopes = value;
  }

  public String getClientSettings() {
    return clientSettings;
  }

  public void setClientSettings(String value) {
    this.clientSettings = value;
  }

  public String getTokenSettings() {
    return tokenSettings;
  }

  public void setTokenSettings(String value) {
    this.tokenSettings = value;
  }
}
