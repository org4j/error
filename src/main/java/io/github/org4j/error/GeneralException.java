package io.github.org4j.error;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An error the service means to answer with: the status names its definition, and the parameters
 * fill the definition's {@code {0}}, {@code {1}}… placeholders.
 *
 * {@snippet :
 * throw new GeneralException(PartnerExceptionStatus.PARTNER_EMAIL_TAKEN, email);
 * throw new GeneralException(SvGateExceptionStatus.DECLINED).subError("-240", "Insufficient funds");
 * }
 */
public class GeneralException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient StatusThrowable status;
  private final transient Object[] params;
  private final transient List<SubError> subErrors = new ArrayList<>();
  private @Nullable Duration retryAfter;

  public GeneralException(StatusThrowable status, Object... params) {
    this(status, null, params);
  }

  public GeneralException(StatusThrowable status, @Nullable Throwable cause, Object... params) {
    super(status.getType(), cause);
    this.status = status;
    this.params = params.clone();
  }

  /**
   * Passes on a third party's own code and message, such as a card scheme's decline reason. The
   * message is masked before it reaches the client.
   */
  public GeneralException subError(@Nullable String code, @Nullable String message) {
    subErrors.add(new SubError(code, message));
    return this;
  }

  /**
   * Tells the client when to try again, for a limit that lifts by itself. Sent as {@code
   * Retry-After} in whole seconds, rounded up.
   */
  public GeneralException retryAfter(Duration wait) {
    this.retryAfter = wait;
    return this;
  }

  public StatusThrowable getStatus() {
    return status;
  }

  public Object[] getParams() {
    return params.clone();
  }

  public List<SubError> getSubErrors() {
    return Collections.unmodifiableList(subErrors);
  }

  public @Nullable Duration getRetryAfter() {
    return retryAfter;
  }

  /** A third party's own code and message, carried in the problem's {@code subErrors}. */
  public record SubError(@Nullable String code, @Nullable String message) {}
}
