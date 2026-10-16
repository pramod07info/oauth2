package com.pramod.oauth2.config;

import com.pramod.oauth2.dto.UserDtos.*;
import com.pramod.oauth2.repository.UserRepository;
import com.pramod.oauth2.service.UserService;
import jakarta.validation.Validator;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;

@Configuration
public class BootstrapConfig {
  @Bean
  ApplicationRunner initialize(
      RegisteredClientRepository clients,
      PasswordEncoder encoder,
      UserRepository users,
      UserService service,
      Validator validator,
      @Value("${app.client-id}") String clientId,
      @Value("${app.client-secret}") String secret,
      @Value("${app.redirect-uri}") String redirect,
      @Value("${app.bootstrap-email}") String email,
      @Value("${app.bootstrap-password}") String password) {
    return args -> {
      if (secret.length() < 32)
        throw new IllegalArgumentException(
            "OAUTH_CLIENT_SECRET must contain at least 32 characters");
      if (clients.findByClientId(clientId) == null)
        clients.save(
            RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(encoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirect)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("users.read")
                .scope("users.write")
                .clientSettings(
                    ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofHours(8))
                        .reuseRefreshTokens(false)
                        .build())
                .build());
      if (!email.isBlank() && users.byEmail(UserService.normalize(email)).isEmpty()) {
        var registration = new Registration(email, password, "Administrator");
        if (!validator.validate(registration).isEmpty())
          throw new IllegalArgumentException("Invalid bootstrap administrator configuration");
        service.create(registration, Role.ADMIN, "bootstrap");
      }
    };
  }
}
