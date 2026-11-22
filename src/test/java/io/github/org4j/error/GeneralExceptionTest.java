package io.github.org4j.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GeneralExceptionTest {

  @Test
  void carriesItsStatusParametersAndCause() {
    var cause = new IllegalStateException("down");
    var ex = new GeneralException(ErrorExceptionStatus.INTERNAL_ERROR, cause, "a", 1);
    assertThat(ex.getStatus()).isEqualTo(ErrorExceptionStatus.INTERNAL_ERROR);
    assertThat(ex.getMessage()).isEqualTo("INTERNAL_ERROR");
    assertThat(ex.getParams()).containsExactly("a", 1);
    assertThat(ex.getCause()).isSameAs(cause);
    assertThat(ex.getSubErrors()).isEmpty();
    assertThat(ex.getRetryAfter()).isNull();
  }

  @Test
  void collectsSubErrorsAndAWait() {
    var ex =
        new GeneralException(ErrorExceptionStatus.FORBIDDEN)
            .subError("-240", "Insufficient funds")
            .subError(null, null)
            .retryAfter(Duration.ofSeconds(3));
    assertThat(ex.getSubErrors())
        .containsExactly(
            new GeneralException.SubError("-240", "Insufficient funds"),
            new GeneralException.SubError(null, null));
    assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(3));
  }

  @Test
  void nothingItHoldsChangesFromOutside() {
    var params = new Object[] {"a"};
    var ex = new GeneralException(ErrorExceptionStatus.FORBIDDEN, params);
    params[0] = "b";
    ex.getParams()[0] = "c";
    assertThat(ex.getParams()).containsExactly("a");
    assertThatThrownBy(() -> ex.getSubErrors().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
