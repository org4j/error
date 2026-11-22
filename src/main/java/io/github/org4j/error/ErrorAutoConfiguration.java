package io.github.org4j.error;

import io.github.org4j.masking.Masking;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Problem details for a servlet web app, ahead of Boot's own handler so that one backs off. With
 * Spring Security on the classpath, also a {@link ProblemSecurityHandler} for the security chain.
 */
@AutoConfiguration(beforeName = ErrorConstants.BOOT_WEB_MVC)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ErrorStarterProperties.class)
public class ErrorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
  ProblemDetailsAdvice problemDetailsAdvice(
      ErrorStarterProperties errors, ObjectProvider<Masking> masking) {
    return new ProblemDetailsAdvice(errors, masking.getIfAvailable(Masking::defaults));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(AuthenticationEntryPoint.class)
  static class SecurityProblems {

    @Bean
    @ConditionalOnMissingBean
    ProblemSecurityHandler problemSecurityHandler(
        @Qualifier(ErrorConstants.EXCEPTION_RESOLVER) HandlerExceptionResolver resolver) {
      return new ProblemSecurityHandler(resolver);
    }

    @Bean
    @ConditionalOnMissingBean({AccessDeniedAdvice.class, ResponseEntityExceptionHandler.class})
    AccessDeniedAdvice accessDeniedAdvice(ProblemDetailsAdvice problems) {
      return new AccessDeniedAdvice(problems);
    }
  }
}
