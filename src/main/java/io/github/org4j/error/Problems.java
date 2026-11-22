package io.github.org4j.error;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the problem detail another service answered with, so a caller never parses one by hand.
 *
 * {@snippet :
 * catch (RestClientResponseException e) {
 *   var code = Problems.of(e).map(Problems::code).orElse(null);
 * }
 * }
 */
public final class Problems {

  private static final JsonMapper JSON =
      JsonMapper.builder().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build();

  private Problems() {}

  /**
   * The problem detail in the response body; empty for no body, an HTML page or anything else that
   * is not one. Works for an exception made by hand, without the client's converters, too.
   */
  public static Optional<ProblemDetail> of(RestClientResponseException e) {
    try {
      return Optional.ofNullable(e.getResponseBodyAs(ProblemDetail.class));
    } catch (IllegalStateException noConverters) {
      return read(e.getResponseBodyAsByteArray());
    } catch (RuntimeException notAProblem) {
      return Optional.empty();
    }
  }

  /** The problem's {@code code}, or {@code null} when it has none. */
  public static @Nullable String code(ProblemDetail problem) {
    return property(problem, ErrorConstants.CODE_PROPERTY) instanceof String code ? code : null;
  }

  /** A third party's own codes the other service passed on; empty when there are none. */
  public static List<GeneralException.SubError> subErrors(ProblemDetail problem) {
    if (!(property(problem, ErrorConstants.SUB_ERRORS_PROPERTY) instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .<GeneralException.SubError>mapMulti(
            (item, found) -> {
              if (item instanceof Map<?, ?> entry) {
                found.accept(
                    new GeneralException.SubError(
                        text(entry.get(ErrorConstants.CODE_PROPERTY)),
                        text(entry.get(ErrorConstants.MESSAGE))));
              }
            })
        .toList();
  }

  private static Optional<ProblemDetail> read(byte[] body) {
    if (body.length == 0) {
      return Optional.empty();
    }
    try {
      return Optional.of(JSON.readValue(body, ProblemDetail.class));
    } catch (RuntimeException notAProblem) {
      return Optional.empty();
    }
  }

  private static @Nullable Object property(ProblemDetail problem, String name) {
    var properties = problem.getProperties();
    return properties == null ? null : properties.get(name);
  }

  private static @Nullable String text(@Nullable Object value) {
    return value == null ? null : value.toString();
  }
}
