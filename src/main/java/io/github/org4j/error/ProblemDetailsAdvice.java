package io.github.org4j.error;

import io.github.org4j.masking.Masking;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Answers every failure with a problem detail: a {@link GeneralException} from its definition, a
 * validation failure as {@code VALIDATION_FAILED}, Spring's own errors with a code named after
 * their status, and anything else as {@code INTERNAL_ERROR}. Each answer is logged once, masked.
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

  private static final Log log = LogFactory.getLog(ProblemDetailsAdvice.class);

  private final ErrorStarterProperties errors;
  private final Masking masking;

  ProblemDetailsAdvice(ErrorStarterProperties errors, Masking masking) {
    this.errors = errors;
    this.masking = masking;
  }

  @ExceptionHandler(GeneralException.class)
  ResponseEntity<ProblemDetail> general(GeneralException ex) {
    var definition = errors.codes().get(ex.getStatus().getType());
    var problem = problem(ex.getStatus(), ex.getParams());
    if (!ex.getSubErrors().isEmpty()) {
      problem.setProperty(
          ErrorConstants.SUB_ERRORS_PROPERTY,
          ex.getSubErrors().stream()
              .map(sub -> new GeneralException.SubError(sub.code(), masking.text(sub.message())))
              .toList());
    }
    log(
        problem,
        definition == null ? null : definition.messageEn(),
        definition != null && definition.isLoggable(),
        ex.getCause());
    var response =
        ResponseEntity.status(problem.getStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON);
    var wait = ex.getRetryAfter();
    if (wait != null) {
      // Whole seconds, rounded up: a client that waits this long is never early.
      var seconds = Math.max(1, (wait.toMillis() + 999) / 1000);
      response.header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
    }
    return response.body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex) {
    var problem = problem(ErrorExceptionStatus.VALIDATION_FAILED);
    var fields =
        ex.getConstraintViolations().stream()
            .map(violation -> field(violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
    problem.setProperty(ErrorConstants.ERRORS_PROPERTY, fields);
    log(problem, ErrorConstants.LOG_INVALID + fieldNames(fields), false, null);
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception ex) {
    return general(new GeneralException(ErrorExceptionStatus.INTERNAL_ERROR, ex));
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var fields =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> field(error.getField(), error.getDefaultMessage()))
            .toList();
    return invalid(ex, ex.getBody(), fields, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var fields =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                field(
                                    String.valueOf(result.getMethodParameter().getParameterName()),
                                    error.getDefaultMessage())))
            .toList();
    return invalid(ex, ex.getBody(), fields, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (body == null && ex instanceof ErrorResponse error) {
      // Spring passes no body for its own errors; fill and translate it as Spring would.
      body = error.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    }
    if (body instanceof ProblemDetail problem) {
      if (problem.getProperties() == null
          || !problem.getProperties().containsKey(ErrorConstants.CODE_PROPERTY)) {
        var named = HttpStatus.resolve(status.value());
        problem.setProperty(
            ErrorConstants.CODE_PROPERTY,
            named == null ? String.valueOf(status.value()) : named.name());
        // Spring's messages quote the caller's input ("For input string: …"); the class name
        // doesn't.
        log(problem, ex.getClass().getSimpleName(), false, status.is5xxServerError() ? ex : null);
      }
      identify(problem);
    }
    return super.handleExceptionInternal(ex, body, headers, status, request);
  }

  private ProblemDetail problem(StatusThrowable status, Object... params) {
    var type = status.getType();
    var definition = errors.codes().get(type);
    var filled = params;
    if (definition == null) {
      log.error(ErrorConstants.LOG_NO_DEFINITION.formatted(type));
      type = ErrorExceptionStatus.INTERNAL_ERROR.getType();
      definition = errors.codes().getOrDefault(type, ErrorConstants.LAST_RESORT);
      filled = new Object[0];
    }
    var detail = masking.text(definition.message(LocaleContextHolder.getLocale(), filled));
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(definition.httpCode()), detail);
    problem.setTitle(definition.title());
    problem.setProperty(ErrorConstants.CODE_PROPERTY, type);
    identify(problem);
    return problem;
  }

  private @Nullable ResponseEntity<Object> invalid(
      Exception ex,
      ProblemDetail problem,
      List<Map<String, String>> fields,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var invalid = problem(ErrorExceptionStatus.VALIDATION_FAILED);
    problem.setTitle(invalid.getTitle());
    problem.setDetail(invalid.getDetail());
    problem.setProperty(
        ErrorConstants.CODE_PROPERTY, ErrorExceptionStatus.VALIDATION_FAILED.getType());
    problem.setProperty(ErrorConstants.ERRORS_PROPERTY, fields);
    log(problem, ErrorConstants.LOG_INVALID + fieldNames(fields), false, null);
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  private Map<String, String> field(String name, @Nullable String message) {
    return Map.of(
        ErrorConstants.FIELD,
        name,
        ErrorConstants.MESSAGE,
        String.valueOf(masking.text(String.valueOf(message))));
  }

  private static List<String> fieldNames(List<Map<String, String>> fields) {
    return fields.stream().map(field -> field.get(ErrorConstants.FIELD)).toList();
  }

  private static void identify(ProblemDetail problem) {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes current) {
      var path = current.getRequest().getRequestURI();
      try {
        problem.setInstance(URI.create(path));
      } catch (IllegalArgumentException notAUri) {
        // A path the server let through (relaxedPathChars) that no URI holds as it is: encoded.
        problem.setInstance(UriComponentsBuilder.fromPath(path).encode().build().toUri());
      }
    }
    var traceId = MDC.get(ErrorConstants.TRACE_ID_MDC);
    if (traceId != null && !traceId.isBlank()) {
      problem.setProperty(ErrorConstants.REQUEST_ID_PROPERTY, traceId);
    }
  }

  private void log(
      ProblemDetail problem, @Nullable String reason, boolean loggable, @Nullable Throwable ex) {
    var properties = problem.getProperties() == null ? Map.of() : problem.getProperties();
    var line =
        masking.text(
            ErrorConstants.LOG_PROBLEM.formatted(
                problem.getStatus(),
                properties.get(ErrorConstants.CODE_PROPERTY),
                request(),
                reason == null ? problem.getTitle() : reason));
    if (problem.getStatus() >= 500) {
      log.error(line, ex);
    } else if (loggable) {
      log.warn(line);
    } else {
      log.info(line);
    }
  }

  private static String request() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes current
        ? current.getRequest().getMethod() + " " + current.getRequest().getRequestURI()
        : "-";
  }
}
