package io.github.org4j.error;

import java.util.regex.Pattern;

/** Names a problem detail carries on the wire, and where the error definitions are bound. */
public final class ErrorConstants {

  /** The prefix of the error definitions: {@code org4j.error.codes.<CODE>}. */
  public static final String PROPERTIES_PREFIX = "org4j.error";

  /** The problem's stable code, and a sub-error's. */
  public static final String CODE_PROPERTY = "code";

  /** The trace id of the request that failed. */
  public static final String REQUEST_ID_PROPERTY = "requestId";

  /** The failing fields of a {@code VALIDATION_FAILED} problem, each {@code {field, message}}. */
  public static final String ERRORS_PROPERTY = "errors";

  /** A third party's own codes, each {@code {code, message}}. */
  public static final String SUB_ERRORS_PROPERTY = "subErrors";

  static final String FIELD = "field";
  static final String MESSAGE = "message";
  static final String TRACE_ID_MDC = "traceId";

  static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d{1,9})}");

  static final String LOG_PROBLEM = "✗ %d %s | %s | %s";
  static final String LOG_INVALID = "invalid fields ";
  static final String LOG_NO_DEFINITION = "No definition for error %s; answering 500";

  static final ErrorStarterProperties.ErrorDefinition LAST_RESORT =
      new ErrorStarterProperties.ErrorDefinition(
          500, "internal-error", true, "Something went wrong.", null, null);

  static final String BEARER_CHALLENGE = "Bearer";
  static final String BOOT_WEB_MVC =
      "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration";
  static final String EXCEPTION_RESOLVER = "handlerExceptionResolver";

  private ErrorConstants() {}
}
