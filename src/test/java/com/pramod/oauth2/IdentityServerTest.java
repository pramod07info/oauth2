package com.pramod.oauth2;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.repository.UserRepository;
import com.pramod.oauth2.service.UserService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityServerTest {
  static final String PASSWORD = "Test-password-123!";
  static final String SECRET = "integration-client-secret-with-32-characters";
  static final String VERIFIER = "01234567890123456789012345678901234567890123456789";

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) throws Exception {
    var key = Files.createTempFile("oauth-jwk-", ".json");
    Files.writeString(key, new RSAKeyGenerator(2048).keyID("test-key").generate().toJSONString());
    key.toFile().deleteOnExit();
    r.add("app.jwk-file", key::toString);
    r.add("app.client-secret", () -> SECRET);
    r.add("app.issuer", () -> "http://localhost:9000");
    r.add("app.redirect-uri", () -> "http://127.0.0.1:8080/callback");
    r.add("app.bootstrap-email", () -> "");
    r.add("app.bootstrap-password", () -> "");
    r.add(
        "spring.datasource.url",
        () ->
            System.getenv()
                .getOrDefault(
                    "TEST_DB_URL",
                    "jdbc:h2:mem:identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"));
    r.add(
        "spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "sa"));
    r.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    r.add("server.servlet.session.cookie.secure", () -> "false");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserService users;
  @Autowired UserRepository repository;
  @Autowired RegisteredClientRepository clients;
  @Autowired JwtEncoder encoder;
  @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

  @BeforeEach
  void configureTestClient() {
    var original = clients.findByClientId("user-management");
    // Consent is tested separately; bypass its UI for protocol round-trip tests.
    if (clients.findByClientId("test-browser") == null)
      clients.save(
          RegisteredClient.from(original)
              .id(UUID.randomUUID().toString())
              .clientId("test-browser")
              .clientSecret(passwordEncoder.encode(SECRET))
              .clientSettings(
                  ClientSettings.builder()
                      .requireProofKey(true)
                      .requireAuthorizationConsent(false)
                      .build())
              .build());
  }

  View create(Role role) {
    return users.create(
        new Registration(UUID.randomUUID() + "@example.com", PASSWORD, "Test User"), role, "test");
  }

  record Tokens(String access, String refresh) {}

  Tokens tokens(View user) throws Exception {
    var login =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", user.email())
                    .param("password", PASSWORD))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();
    String challenge =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
    var response =
        mvc.perform(
                get("/oauth2/authorize")
                    .session(session)
                    .queryParam("response_type", "code")
                    .queryParam("client_id", "test-browser")
                    .queryParam("redirect_uri", "http://127.0.0.1:8080/callback")
                    .queryParam("scope", "openid users.read users.write")
                    .queryParam("state", "test-state")
                    .queryParam("code_challenge", challenge)
                    .queryParam("code_challenge_method", "S256"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    String location = response.getResponse().getHeader("Location");
    String code =
        UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");
    assertThat(code).as(location).isNotBlank();
    var exchange =
        mvc.perform(
                post("/oauth2/token")
                    .with(httpBasic("test-browser", SECRET))
                    .param("grant_type", "authorization_code")
                    .param("code", code)
                    .param("redirect_uri", "http://127.0.0.1:8080/callback")
                    .param("code_verifier", VERIFIER))
            .andExpect(status().isOk())
            .andReturn();
    var body = json.readTree(exchange.getResponse().getContentAsString());
    return new Tokens(body.get("access_token").asText(), body.get("refresh_token").asText());
  }

  @Test
  void registrationHashesPasswordAndNeverGrantsAdmin() throws Exception {
    String email = UUID.randomUUID() + "@example.com";
    var body =
        Map.of("email", email, "password", PASSWORD, "displayName", "New User", "role", "ADMIN");
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    assertThat(repository.byEmail(email).orElseThrow().passwordHash())
        .startsWith("{bcrypt}")
        .doesNotContain(PASSWORD);
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isConflict());
  }

  @Test
  void loginRequiresCsrfAndRejectsBadPassword() throws Exception {
    View u = create(Role.USER);
    mvc.perform(post("/login").param("username", u.email()).param("password", PASSWORD))
        .andExpect(status().isForbidden());
    mvc.perform(post("/login").with(csrf()).param("username", u.email()).param("password", "wrong"))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void oauthPkceJwtAndRotatingRefreshTokens() throws Exception {
    assertThat(
            clients
                .findByClientId("user-management")
                .getClientSettings()
                .isRequireAuthorizationConsent())
        .isTrue();
    Tokens t = tokens(create(Role.USER));
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + t.access()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/users").header("Authorization", "Bearer " + t.access()))
        .andExpect(status().isForbidden());
    var refreshed =
        mvc.perform(
                post("/oauth2/token")
                    .with(httpBasic("test-browser", SECRET))
                    .param("grant_type", "refresh_token")
                    .param("refresh_token", t.refresh()))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
            json.readTree(refreshed.getResponse().getContentAsString())
                .get("refresh_token")
                .asText())
        .isNotEqualTo(t.refresh());
    mvc.perform(
            post("/oauth2/token")
                .with(httpBasic("test-browser", SECRET))
                .param("grant_type", "refresh_token")
                .param("refresh_token", t.refresh()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminCrudVersionConflictAndImmediateDisable() throws Exception {
    View admin = create(Role.ADMIN), user = create(Role.USER);
    Tokens a = tokens(admin), u = tokens(user);
    mvc.perform(get("/api/users").header("Authorization", "Bearer " + a.access()))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/users/" + user.id())
                .header("Authorization", "Bearer " + a.access())
                .contentType("application/json")
                .content("{\"displayName\":\"Updated\",\"version\":0}"))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/users/" + user.id())
                .header("Authorization", "Bearer " + a.access())
                .contentType("application/json")
                .content("{\"displayName\":\"Stale\",\"version\":0}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/users/" + user.id() + "/permissions")
                .header("Authorization", "Bearer " + a.access())
                .contentType("application/json")
                .content("{\"role\":\"USER\",\"enabled\":false,\"version\":1}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + u.access()))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/users/" + admin.id()).header("Authorization", "Bearer " + a.access()))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/users/" + user.id()).header("Authorization", "Bearer " + a.access()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + a.access()))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsMissingExpiredTamperedAndWrongAudienceTokens() throws Exception {
    View u = create(Role.USER);
    mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid.jwt.token"))
        .andExpect(status().isUnauthorized());
    for (String audience : List.of("wrong-audience", "user-api")) {
      var now = Instant.now();
      var claims =
          JwtClaimsSet.builder()
              .issuer("http://localhost:9000")
              .subject(u.id().toString())
              .audience(List.of(audience))
              .issuedAt(now.minusSeconds(600))
              .expiresAt(audience.equals("user-api") ? now.minusSeconds(300) : now.plusSeconds(300))
              .claim("user_version", 0)
              .claim("scope", List.of("users.read"))
              .build();
      String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
      mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void rejectsInvalidInputAndUnboundedPagination() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"bad\",\"password\":\"short\",\"displayName\":\"\"}"))
        .andExpect(status().isBadRequest());
    Tokens admin = tokens(create(Role.ADMIN));
    mvc.perform(get("/api/users?size=101").header("Authorization", "Bearer " + admin.access()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void passwordChangeRevokesAccessAndRefreshTokens() throws Exception {
    View user = create(Role.USER);
    Tokens t = tokens(user);
    mvc.perform(
            put("/api/users/me/password")
                .header("Authorization", "Bearer " + t.access())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        new PasswordChange(PASSWORD, "New-long-password-456!"))))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + t.access()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/oauth2/token")
                .with(httpBasic("test-browser", SECRET))
                .param("grant_type", "refresh_token")
                .param("refresh_token", t.refresh()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void oauthRevocationImmediatelyRejectsAccessToken() throws Exception {
    Tokens t = tokens(create(Role.USER));
    mvc.perform(
            post("/oauth2/revoke")
                .with(httpBasic("test-browser", SECRET))
                .param("token", t.access()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + t.access()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void requiresPkceAndDoesNotAcceptLoginCookieForApi() throws Exception {
    View user = create(Role.USER);
    var result =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", user.email())
                    .param("password", PASSWORD))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
    var authorize =
        mvc.perform(
                get("/oauth2/authorize")
                    .session(session)
                    .queryParam("response_type", "code")
                    .queryParam("client_id", "test-browser")
                    .queryParam("redirect_uri", "http://127.0.0.1:8080/callback")
                    .queryParam("scope", "users.read")
                    .queryParam("state", "test-state"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    assertThat(authorize.getResponse().getHeader("Location"))
        .contains("error=invalid_request")
        .doesNotContain("code=");
    users.permissions(user.id(), new Permissions(Role.USER, false, 0), UUID.randomUUID());
    mvc.perform(get("/oauth2/authorize").session(session).queryParam("client_id", "test-browser"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void locksAccountAfterFiveBadPasswords() throws Exception {
    View user = create(Role.USER);
    for (int attempt = 0; attempt < 5; attempt++) {
      mvc.perform(
              post("/login")
                  .with(csrf())
                  .param("username", user.email())
                  .param("password", "incorrect"))
          .andExpect(redirectedUrl("/login?error"));
    }
    assertThat(repository.byId(user.id()).orElseThrow().lockedUntil()).isAfter(Instant.now());
    mvc.perform(
            post("/login").with(csrf()).param("username", user.email()).param("password", PASSWORD))
        .andExpect(redirectedUrl("/login?error"));
  }

  @Autowired javax.sql.DataSource dataSource;
  @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;

  @Autowired
  org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
      authorizationService;

  @Autowired
  org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
      consentService;

  @Test
  void jpaAdaptersReadAndWriteLegacyJdbcRecords() {
    // JDBC appears only in this migration test to emulate records from the previous release.
    var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    var legacyClients =
        new org.springframework.security.oauth2.server.authorization.client
            .JdbcRegisteredClientRepository(jdbc);
    String clientId = "migration-" + UUID.randomUUID();
    var client =
        RegisteredClient.from(clients.findByClientId("user-management"))
            .id(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(SECRET))
            .build();
    legacyClients.save(client);
    assertThat(clients.findByClientId(clientId).getTokenSettings().getRefreshTokenTimeToLive())
        .isEqualTo(client.getTokenSettings().getRefreshTokenTimeToLive());
    clients.save(RegisteredClient.from(client).clientName("JPA client").build());
    assertThat(legacyClients.findById(client.getId()).getClientName()).isEqualTo("JPA client");

    var legacyAuthorizations =
        new org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService(
            jdbc, legacyClients);
    var now = Instant.now();
    var token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "legacy-" + UUID.randomUUID(),
            now,
            now.plusSeconds(300),
            Set.of("users.read"));
    var authorization =
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization
            .withRegisteredClient(client)
            .id(UUID.randomUUID().toString())
            .principalName("migration@example.com")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("users.read"))
            .token(
                token,
                m ->
                    m.put(
                        org.springframework.security.oauth2.server.authorization.OAuth2Authorization
                            .Token.CLAIMS_METADATA_NAME,
                        new java.util.HashMap<>(Map.of("sub", "migration-user"))))
            .build();
    legacyAuthorizations.save(authorization);
    var loaded =
        authorizationService.findByToken(
            token.getTokenValue(),
            org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN);
    assertThat(loaded.getAccessToken().getClaims()).containsEntry("sub", "migration-user");
    authorizationService.save(
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization.from(loaded)
            .attribute("migration", "jpa")
            .build());
    assertThat(
            (String) legacyAuthorizations.findById(authorization.getId()).getAttribute("migration"))
        .isEqualTo("jpa");

    var legacyConsents =
        new org.springframework.security.oauth2.server.authorization
            .JdbcOAuth2AuthorizationConsentService(jdbc, legacyClients);
    var consent =
        org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent.withId(
                client.getId(), "migration@example.com")
            .scope("users.read")
            .build();
    legacyConsents.save(consent);
    assertThat(consentService.findById(client.getId(), "migration@example.com").getScopes())
        .containsExactly("users.read");
    consentService.save(
        org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent.from(
                consent)
            .scope("users.write")
            .build());
    assertThat(legacyConsents.findById(client.getId(), "migration@example.com").getScopes())
        .contains("users.write");
    assertThat(entityManagerFactory.isOpen()).isTrue();
  }

  @Test
  void hibernateRejectsConcurrentUserUpdates() {
    View user = create(Role.USER);
    var first = entityManagerFactory.createEntityManager();
    var second = entityManagerFactory.createEntityManager();
    try {
      first.getTransaction().begin();
      second.getTransaction().begin();
      var firstCopy = first.find(com.pramod.oauth2.entity.AppUser.class, user.id());
      var secondCopy = second.find(com.pramod.oauth2.entity.AppUser.class, user.id());
      firstCopy.setDisplayName("First writer");
      first.getTransaction().commit();
      secondCopy.setDisplayName("Stale writer");
      assertThatThrownBy(() -> second.getTransaction().commit())
          .isInstanceOf(jakarta.persistence.RollbackException.class);
      assertThat(users.get(user.id()).displayName()).isEqualTo("First writer");
      assertThat(users.get(user.id()).version()).isEqualTo(1);
    } finally {
      if (first.getTransaction().isActive()) first.getTransaction().rollback();
      if (second.getTransaction().isActive()) second.getTransaction().rollback();
      first.close();
      second.close();
    }
  }

  @Test
  void browserEndpointsRequireCsrfAndStartPkceWithoutLeakingSecret() throws Exception {
    mvc.perform(get("/bff/session")).andExpect(status().isUnauthorized());
    mvc.perform(post("/bff/register").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/bff/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        new Registration(
                            UUID.randomUUID() + "@example.com", PASSWORD, "Browser member"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("USER"));
    var result =
        mvc.perform(get("/oauth2/authorization/identity-ui"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    assertThat(result.getResponse().getHeader("Location"))
        .contains("code_challenge_method=S256")
        .contains("code_challenge=")
        .doesNotContain(SECRET);
    assertThat(clients.findByClientId("identity-ui").getClientSettings().isRequireProofKey())
        .isTrue();
  }
}
