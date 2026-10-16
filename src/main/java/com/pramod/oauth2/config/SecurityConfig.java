package com.pramod.oauth2.config;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.pramod.oauth2.repository.UserRepository;
import com.pramod.oauth2.service.UserService;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email ->
        users
            .byEmail(UserService.normalize(email))
            .map(
                a ->
                    User.withUsername(a.view().email())
                        .password(a.passwordHash())
                        .roles(a.view().role().name())
                        .disabled(!a.view().enabled())
                        .accountLocked(
                            a.lockedUntil() != null
                                && a.lockedUntil().isAfter(java.time.Instant.now()))
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServer(HttpSecurity http, UserRepository users)
      throws Exception {
    http.addFilterAfter(
        new AccountSessionFilter(users),
        org.springframework.security.web.context.SecurityContextHolderFilter.class);
    var server = OAuth2AuthorizationServerConfigurer.authorizationServer();
    http.securityMatcher(server.getEndpointsMatcher())
        .with(server, c -> c.oidc(Customizer.withDefaults()))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .oauth2ResourceServer(r -> r.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain api(
      HttpSecurity http,
      UserRepository users,
      OAuth2AuthorizationService authorizations,
      @Value("${app.audience}") String audience)
      throws Exception {
    http.securityMatcher("/api/**")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(c -> c.disable())
        .requestCache(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAuthority("SCOPE_users.read")
                    .anyRequest()
                    .hasAuthority("SCOPE_users.write"))
        .oauth2ResourceServer(
            r ->
                r.jwt(
                    j ->
                        j.jwtAuthenticationConverter(
                            jwt -> {
                              try {
                                if (!jwt.getAudience().contains(audience))
                                  throw new BadCredentialsException("Invalid audience");
                                var user =
                                    users
                                        .byId(UUID.fromString(jwt.getSubject()))
                                        .orElseThrow(
                                            () -> new BadCredentialsException("Invalid account"));
                                String version = jwt.getClaimAsString("user_version");
                                if (!user.view().enabled()
                                    || version == null
                                    || !version.equals(Long.toString(user.view().version())))
                                  throw new BadCredentialsException(
                                      "Account or token no longer valid");
                                var authorization =
                                    authorizations.findByToken(
                                        jwt.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
                                if (authorization == null
                                    || authorization.getAccessToken() == null
                                    || !authorization.getAccessToken().isActive())
                                  throw new BadCredentialsException("Token revoked");
                                var authorities =
                                    new ArrayList<
                                        org.springframework.security.core.GrantedAuthority>(
                                        new JwtGrantedAuthoritiesConverter().convert(jwt));
                                authorities.add(
                                    new SimpleGrantedAuthority(
                                        "ROLE_" + user.view().role().name()));
                                return new JwtAuthenticationToken(
                                    jwt, authorities, jwt.getSubject());
                              } catch (IllegalArgumentException ex) {
                                throw new BadCredentialsException("Invalid subject", ex);
                              }
                            })));
    return http.build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain login(HttpSecurity http, UserRepository users) throws Exception {
    http.addFilterAfter(
        new AccountSessionFilter(users),
        org.springframework.security.web.context.SecurityContextHolderFilter.class);
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/error", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .formLogin(
            f ->
                f.failureHandler(
                        (request, response, exception) -> {
                          users.failedLogin(
                              UserService.normalize(
                                  java.util.Objects.toString(
                                      request.getParameter("username"), "")));
                          new org.springframework.security.web.authentication
                                  .SimpleUrlAuthenticationFailureHandler("/login?error")
                              .onAuthenticationFailure(request, response, exception);
                        })
                    .successHandler(
                        (request, response, authentication) -> {
                          users.successfulLogin(authentication.getName());
                          var account = users.byEmail(authentication.getName()).orElseThrow();
                          request
                              .getSession()
                              .setAttribute(AccountSessionFilter.VERSION, account.view().version());
                          new org.springframework.security.web.authentication
                                  .SavedRequestAwareAuthenticationSuccessHandler()
                              .onAuthenticationSuccess(request, response, authentication);
                        }))
        .logout(l -> l.logoutSuccessUrl("/login?logout"))
        .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()));
    return http.build();
  }

  @Bean
  RegisteredClientRepository registeredClients(JdbcTemplate jdbc) {
    return new JdbcRegisteredClientRepository(jdbc);
  }

  @Bean
  OAuth2AuthorizationService authorizations(JdbcTemplate jdbc, RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationService(jdbc, clients);
  }

  @Bean
  OAuth2AuthorizationConsentService consents(
      JdbcTemplate jdbc, RegisteredClientRepository clients) {
    return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(@Value("${app.issuer}") String issuer) {
    return AuthorizationServerSettings.builder().issuer(issuer).build();
  }

  @Bean
  JWKSource<SecurityContext> jwkSource(@Value("${app.jwk-file}") String file) throws Exception {
    RSAKey key = RSAKey.parse(Files.readString(Path.of(file)));
    if (!key.isPrivate() || key.size() < 2048 || key.getKeyID() == null)
      throw new IllegalArgumentException(
          "RSA JWK requires private key, kid and at least 2048 bits");
    JWKSet keys = new JWKSet(key);
    return (selector, context) -> selector.select(keys);
  }

  @Bean
  JwtEncoder jwtEncoder(JWKSource<SecurityContext> source) {
    return new NimbusJwtEncoder(source);
  }

  @Bean
  JwtDecoder jwtDecoder(JWKSource<SecurityContext> source, @Value("${app.issuer}") String issuer) {
    NimbusJwtDecoder decoder =
        (NimbusJwtDecoder)
            org.springframework.security.oauth2.server.authorization.config.annotation.web
                .configuration.OAuth2AuthorizationServerConfiguration.jwtDecoder(source);
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
      UserRepository users, @Value("${app.audience}") String audience) {
    return context -> {
      var account =
          users
              .byEmail(context.getPrincipal().getName())
              .orElseThrow(() -> new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT));
      if (!account.view().enabled())
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
      context.getClaims().subject(account.view().id().toString());
      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        context
            .getClaims()
            .audience(new ArrayList<>(List.of(audience)))
            .claim("roles", new ArrayList<>(List.of(account.view().role().name())))
            .claim("user_version", Long.toString(account.view().version()));
      }
    };
  }
}
