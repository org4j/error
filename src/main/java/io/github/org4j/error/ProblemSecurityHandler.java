package io.github.org4j.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The security chain's refusals as problem details: no valid token is {@code UNAUTHORIZED} with a
 * {@code Bearer} challenge, the wrong role {@code FORBIDDEN}. Both go through the MVC exception
 * resolver, so they are answered and logged like every other error.
 *
 * {@snippet :
 * http.exceptionHandling(errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems));
 * }
 */
public class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final HandlerExceptionResolver resolver;

  public ProblemSecurityHandler(HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, ErrorConstants.BEARER_CHALLENGE);
    resolver.resolveException(
        request, response, null, new GeneralException(ErrorExceptionStatus.UNAUTHORIZED));
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
    resolver.resolveException(
        request, response, null, new GeneralException(ErrorExceptionStatus.FORBIDDEN));
  }
}
