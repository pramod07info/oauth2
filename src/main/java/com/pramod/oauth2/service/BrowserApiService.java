package com.pramod.oauth2.service;

import com.pramod.oauth2.config.BrowserSecurityConfig;
import com.pramod.oauth2.repository.UserRepository;
import jakarta.servlet.http.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BrowserApiService {
  private final OAuth2AuthorizedClientManager manager;
  private final OAuth2AuthorizedClientRepository clients;
  private final OAuth2AuthorizationService authorizations;
  private final UserRepository users;
  private final RestClient http;

  public BrowserApiService(
      OAuth2AuthorizedClientManager manager,
      OAuth2AuthorizedClientRepository clients,
      OAuth2AuthorizationService authorizations,
      UserRepository users,
      @Value("${app.issuer}") String issuer) {
    this.manager = manager;
    this.clients = clients;
    this.authorizations = authorizations;
    this.users = users;
    this.http = RestClient.builder().baseUrl(issuer).build();
  }

  private OAuth2AuthorizedClient client(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    var result =
        manager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(BrowserSecurityConfig.CLIENT)
                .principal(auth)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build());
    if (result == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
    return result;
  }

  public Object call(
      HttpMethod method,
      String path,
      Object body,
      Authentication auth,
      HttpServletRequest request,
      HttpServletResponse response) {
    synchronized (request.getSession()) {
      var client = client(auth, request, response);
      var spec =
          http.method(method)
              .uri(path)
              .headers(h -> h.setBearerAuth(client.getAccessToken().getTokenValue()));
      if (body != null) spec.contentType(MediaType.APPLICATION_JSON).body(body);
      Object result = spec.retrieve().body(Object.class);
      if (method == HttpMethod.PUT
          && (path.equals("/api/users/me")
              || path.equals(
                  "/api/users/" + users.byEmail(auth.getName()).orElseThrow().view().id()))) {
        refresh(auth, request, response);
        request
            .getSession()
            .setAttribute(
                "accountVersion", users.byEmail(auth.getName()).orElseThrow().view().version());
      }
      if (method == HttpMethod.PUT && path.equals("/api/users/me/password"))
        logout(auth, request, response);
      return result;
    }
  }

  public Map<String, Object> session(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    synchronized (request.getSession()) {
      Object profile = call(HttpMethod.GET, "/api/users/me", null, auth, request, response);
      var client = client(auth, request, response);
      return Map.of(
          "user", profile, "expiresAt", client.getAccessToken().getExpiresAt().toString());
    }
  }

  public void refresh(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    synchronized (request.getSession()) {
      OAuth2AuthorizedClient current =
          clients.loadAuthorizedClient(BrowserSecurityConfig.CLIENT, auth, request);
      if (current == null || current.getRefreshToken() == null)
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
      var token =
          new RestClientRefreshTokenTokenResponseClient()
              .getTokenResponse(
                  new OAuth2RefreshTokenGrantRequest(
                      current.getClientRegistration(),
                      current.getAccessToken(),
                      current.getRefreshToken()));
      clients.saveAuthorizedClient(
          new OAuth2AuthorizedClient(
              current.getClientRegistration(),
              auth.getName(),
              token.getAccessToken(),
              token.getRefreshToken() == null
                  ? current.getRefreshToken()
                  : token.getRefreshToken()),
          auth,
          request,
          response);
    }
  }

  public Object inspect(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    synchronized (request.getSession()) {
      var current = client(auth, request, response);
      return http.post()
          .uri("/oauth2/introspect")
          .headers(
              h ->
                  h.setBasicAuth(
                      current.getClientRegistration().getClientId(),
                      current.getClientRegistration().getClientSecret()))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(
              "token="
                  + java.net.URLEncoder.encode(
                      current.getAccessToken().getTokenValue(),
                      java.nio.charset.StandardCharsets.UTF_8))
          .retrieve()
          .body(Object.class);
    }
  }

  public void logout(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    synchronized (request.getSession()) {
      OAuth2AuthorizedClient current =
          clients.loadAuthorizedClient(BrowserSecurityConfig.CLIENT, auth, request);
      if (current != null) {
        var record =
            authorizations.findByToken(
                current.getAccessToken().getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
        if (record != null) authorizations.remove(record);
      }
      clients.removeAuthorizedClient(BrowserSecurityConfig.CLIENT, auth, request, response);
      request.getSession().invalidate();
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }
}
