package io.github.org4j.error;

/** The errors every service shares, defined in the library's own {@code error-exception.yaml}. */
public enum ErrorExceptionStatus implements StatusThrowable {
  /** Anything unexpected, or an error without a definition: 500. */
  INTERNAL_ERROR,
  /** Invalid input: 400, listing the failing fields. */
  VALIDATION_FAILED,
  /** No valid token: 401. */
  UNAUTHORIZED,
  /** The token's roles don't allow it: 403. */
  FORBIDDEN;

  @Override
  public String getType() {
    return name();
  }
}
