package com.pramod.oauth2.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "oauth2_authorization_consent")
@IdClass(OAuthConsentId.class)
public class OAuthConsent {
  @Id
  @Column(name = "registered_client_id", length = 100, nullable = false)
  private String registeredClientId;

  @Id
  @Column(name = "principal_name", length = 254, nullable = false)
  private String principalName;

  @Column(name = "authorities", length = 1000, nullable = false)
  private String authorities;

  public OAuthConsent() {}

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

  public String getAuthorities() {
    return authorities;
  }

  public void setAuthorities(String value) {
    this.authorities = value;
  }
}
