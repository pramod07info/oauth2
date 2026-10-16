package com.pramod.oauth2.config;

import com.pramod.oauth2.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revalidates browser sessions after account changes, including across app instances. */
final class AccountSessionFilter extends OncePerRequestFilter {
  static final String VERSION = "accountVersion";
  private final UserRepository users;

  AccountSessionFilter(UserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var session = request.getSession(false);
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (session != null
        && auth instanceof UsernamePasswordAuthenticationToken
        && auth.isAuthenticated()) {
      var account = users.byEmail(auth.getName());
      var version = session.getAttribute(VERSION);
      if (account.isEmpty()
          || !account.get().view().enabled()
          || !(version instanceof Long)
          || ((Long) version).longValue() != account.get().view().version()) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        response.sendError(401, "Session expired; sign in again");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
