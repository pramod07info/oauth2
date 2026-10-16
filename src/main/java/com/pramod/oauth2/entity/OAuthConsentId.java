package com.pramod.oauth2.entity;

import java.io.Serializable;
import java.util.Objects;

public class OAuthConsentId implements Serializable {
  private static final long serialVersionUID = 1L;
  private String registeredClientId;
  private String principalName;

  public OAuthConsentId() {}

  public OAuthConsentId(String client, String principal) {
    registeredClientId = client;
    principalName = principal;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof OAuthConsentId id
        && Objects.equals(registeredClientId, id.registeredClientId)
        && Objects.equals(principalName, id.principalName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(registeredClientId, principalName);
  }
}
