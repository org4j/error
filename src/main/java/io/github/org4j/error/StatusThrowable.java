package io.github.org4j.error;

/**
 * One error a service can throw, usually an enum constant. Its type is the code clients see and the
 * key of its definition under {@code org4j.error.codes}.
 *
 * {@snippet :
 * public enum PartnerExceptionStatus implements StatusThrowable {
 *   PARTNER_EMAIL_TAKEN;
 *
 *   @Override
 *   public String getType() {
 *     return name();
 *   }
 * }
 * }
 */
public interface StatusThrowable {

  String getType();
}
