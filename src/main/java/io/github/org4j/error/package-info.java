/**
 * One error format for every service: RFC 9457 problem details with a stable code and a message in
 * the caller's language. A service throws a {@link io.github.org4j.error.GeneralException} for one
 * of its {@link io.github.org4j.error.StatusThrowable} errors; {@link
 * io.github.org4j.error.Problems} reads another service's answer.
 */
@NullMarked
package io.github.org4j.error;

import org.jspecify.annotations.NullMarked;
