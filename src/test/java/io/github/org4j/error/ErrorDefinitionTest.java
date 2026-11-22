package io.github.org4j.error;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.org4j.error.ErrorStarterProperties.ErrorDefinition;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ErrorDefinitionTest {

  private final ErrorDefinition definition =
      new ErrorDefinition(
          409, "transfer-refused", false, "{0} can't pay {1}.", "{0} не может оплатить {1}.", null);

  @Test
  void speaksTheLocalesLanguageAndFallsBackToEnglish() {
    assertThat(definition.message(Locale.forLanguageTag("ru-RU"), "Aziz", 100))
        .isEqualTo("Aziz не может оплатить 100.");
    assertThat(definition.message(Locale.forLanguageTag("uz"), "Aziz", 100))
        .isEqualTo("Aziz can't pay 100.");
    assertThat(definition.message(Locale.GERMAN, "Aziz", 100)).isEqualTo("Aziz can't pay 100.");
  }

  @Test
  void aParameterIsNeverFilledInAgain() {
    assertThat(definition.message(Locale.ENGLISH, "{1}", "$5")).isEqualTo("{1} can't pay $5.");
  }

  @Test
  void aPlaceholderWithoutAParameterStays() {
    assertThat(definition.message(Locale.ENGLISH, "Aziz")).isEqualTo("Aziz can't pay {1}.");
  }
}
