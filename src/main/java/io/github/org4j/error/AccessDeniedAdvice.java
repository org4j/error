package io.github.org4j.error;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Method security's refusal, thrown inside the controller, as a 403 rather than a 500. Apart from
 * {@link ProblemDetailsAdvice} so that one loads without Spring Security on the classpath.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AccessDeniedAdvice {

  private final ProblemDetailsAdvice problems;

  AccessDeniedAdvice(ProblemDetailsAdvice problems) {
    this.problems = problems;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException ex) {
    return problems.general(new GeneralException(ErrorExceptionStatus.FORBIDDEN));
  }
}
