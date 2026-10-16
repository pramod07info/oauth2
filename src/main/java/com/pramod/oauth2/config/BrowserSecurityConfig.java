package com.pramod.oauth2.config;

import com.pramod.oauth2.repository.UserRepository;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
public class BrowserSecurityConfig {
  public static final String CLIENT = "identity-ui";

  @Bean
  ClientRegistrationRepository browserRegistration(
      @Value("${app.issuer}") String issuer, @Value("${app.client-secret}") String secret) {
    return new InMemoryClientRegistrationRepository(
        ClientRegistration.withRegistrationId(CLIENT)
            .clientId(CLIENT)
            .clientSecret(secret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(issuer + "/login/oauth2/code/" + CLIENT)
            .scope("openid", "profile", "users.read", "users.write")
            .authorizationUri(issuer + "/oauth2/authorize")
            .tokenUri(issuer + "/oauth2/token")
            .jwkSetUri(issuer + "/oauth2/jwks")
            .issuerUri(issuer)
            .userNameAttributeName("email")
            .clientName("Identity Console")
            .build());
  }

  @Bean
  OAuth2AuthorizedClientRepository browserAuthorizedClients() {
    return new HttpSessionOAuth2AuthorizedClientRepository();
  }

  @Bean
  OAuth2AuthorizedClientManager browserClientManager(
      ClientRegistrationRepository registrations, OAuth2AuthorizedClientRepository repository) {
    var manager = new DefaultOAuth2AuthorizedClientManager(registrations, repository);
    manager.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build());
    return manager;
  }

  @Bean
  @Order(3)
  SecurityFilterChain browser(
      HttpSecurity http,
      ClientRegistrationRepository registrations,
      OAuth2AuthorizedClientRepository clients,
      UserRepository users,
      @Value("${app.ui-base-url:http://localhost:4200}") String ui)
      throws Exception {
    var resolver =
        new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    http.securityMatcher("/bff/**", "/oauth2/authorization/**", "/login/oauth2/code/**")
        .addFilterAfter(new AccountSessionFilter(users), SecurityContextHolderFilter.class)
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/bff/csrf",
                        "/bff/register",
                        "/oauth2/authorization/**",
                        "/login/oauth2/code/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .requestCache(c -> c.disable())
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .oauth2Login(
            o ->
                o.authorizedClientRepository(clients)
                    .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
                    .successHandler(
                        (request, response, authentication) -> {
                          var account = users.byEmail(authentication.getName()).orElseThrow();
                          request
                              .getSession()
                              .setAttribute(AccountSessionFilter.VERSION, account.view().version());
                          response.sendRedirect(ui + "/dashboard");
                        })
                    .failureHandler(
                        (request, response, error) ->
                            response.sendRedirect(ui + "/sign-in?error=authentication")));
    return http.build();
  }

  @Bean
  ApplicationRunner registerBrowserClient(
      RegisteredClientRepository clients,
      PasswordEncoder encoder,
      @Value("${app.issuer}") String issuer,
      @Value("${app.client-secret}") String secret) {
    return args -> {
      if (clients.findByClientId(CLIENT) == null)
        clients.save(
            RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(CLIENT)
                .clientName("Identity Console")
                .clientSecret(encoder.encode(secret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(issuer + "/login/oauth2/code/" + CLIENT)
                .scope("openid")
                .scope("profile")
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
    };
  }
}
