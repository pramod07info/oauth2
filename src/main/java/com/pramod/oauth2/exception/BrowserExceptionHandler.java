package com.pramod.oauth2.exception;

import com.pramod.oauth2.controller.BrowserController;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice(assignableTypes = BrowserController.class)
public class BrowserExceptionHandler {
  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<String> upstream(RestClientResponseException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(ex.getResponseBodyAsString());
  }

  @ExceptionHandler(OAuth2AuthorizationException.class)
  public ProblemDetail expired(OAuth2AuthorizationException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED, "Session expired; sign in again");
  }
}
