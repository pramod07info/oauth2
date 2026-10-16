package com.pramod.oauth2.service.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Component;

/** Isolated mapper for trusted persisted OAuth data; never used for HTTP request bodies. */
@Component
public class OAuthJson {
  private final ObjectMapper mapper = new ObjectMapper();

  public OAuthJson() {
    mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
    mapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
  }

  public String write(Map<String, Object> value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot serialize OAuth record", ex);
    }
  }

  public Map<String, Object> read(String value) {
    if (value == null || value.isBlank()) return new HashMap<>();
    try {
      return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot deserialize OAuth record", ex);
    }
  }
}
