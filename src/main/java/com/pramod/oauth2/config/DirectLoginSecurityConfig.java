package com.pramod.oauth2.config;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableScheduling
public class DirectLoginSecurityConfig {
  @Bean
  AuthenticationManager directAuthenticationManager(
      UserDetailsService details, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(details);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  @Order(2)
  SecurityFilterChain directLogin(HttpSecurity http) throws Exception {
    // Session contains CSRF state only. Bearer credentials are never persisted in it.
    http.securityMatcher(
            "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/csrf")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }
}
