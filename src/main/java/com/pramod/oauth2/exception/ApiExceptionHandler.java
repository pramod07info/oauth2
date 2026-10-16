package com.pramod.oauth2.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
  public ProblemDetail invalid(jakarta.validation.ConstraintViolationException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Request parameters are invalid");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail conflict(DataIntegrityViolationException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Request conflicts with an existing record");
  }
}
