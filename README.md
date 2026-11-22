# error

One error format for every service: RFC 9457 problem details with a stable `code` and a message in
the caller's language (`Accept-Language`: en, ru, uz). Use it in every web app and adapter.

```groovy
implementation libs.error
testImplementation testFixtures(libs.error) // ErrorDefinitions
```

## The answer

| Field | What |
|---|---|
| `status`, `title` | from the error's definition |
| `detail` | the definition's message in the caller's language, parameters filled in, masked |
| `code` | the error's type: `PARTNER_EMAIL_TAKEN`, or `NOT_FOUND` for Spring's own errors |
| `instance` | the request's path |
| `requestId` | the trace id, when the logging context has one |
| `errors` | for `VALIDATION_FAILED`: each failing `{field, message}` |
| `subErrors` | a third party's own `{code, message}`, masked |

The names are in `ErrorConstants`. The content type is `application/problem+json`.

## Throwing an error

A service lists its errors in an enum that implements `StatusThrowable` and defines each one in
`src/main/resources/properties/<name>-exception.yaml`:

```yaml
org4j:
  error:
    codes:
      PARTNER_EMAIL_TAKEN:
        http-code: 409
        title: partner-email-taken
        is-loggable: false
        message-en: "{0} is already registered."
        message-ru: "{0} уже зарегистрирован."
        message-uz: "{0} allaqachon roʻyxatdan oʻtgan."
```

`throw new GeneralException(PartnerExceptionStatus.PARTNER_EMAIL_TAKEN, email)` answers 409 with
that message. Each parameter fills its placeholder once, as written: a parameter that holds `{1}`
stays `{1}`.

| Call | Does |
|---|---|
| `.subError(code, message)` | passes on a third party's own code, such as a card scheme's |
| `.retryAfter(duration)` | sends `Retry-After` in whole seconds, rounded up, for a limit that lifts by itself |

Every error response is logged once: status, code, request and the English message without its
parameters, masked. A 5xx is an error, an `is-loggable` one a warning, anything else info.

## Properties

| Property | What |
|---|---|
| `org4j.error.codes` | every error's definition by its code: `http-code`, `title`, `is-loggable`, `message-en`, `message-ru`, `message-uz` |

The library loads its own `properties/error-exception.yaml` (below the app's configuration); an app
imports its modules' files with `spring.config.import`.

## Errors

| Code | Status | When |
|---|---|---|
| `INTERNAL_ERROR` | 500 | anything unexpected, or an error without a definition; never a stack trace |
| `VALIDATION_FAILED` | 400 | invalid input, from a controller or a `@Validated` service; `errors` lists `{field, message}` |
| `UNAUTHORIZED` | 401 | no valid token, with `WWW-Authenticate: Bearer` |
| `FORBIDDEN` | 403 | the token's roles don't allow it, from the security chain or method security |

Spring's own errors get a code named after their status (`BAD_REQUEST`, `NOT_FOUND`,
`METHOD_NOT_ALLOWED`, …) and Spring's message translated (`messages/error*.properties`).

## Reading another service's problem

`Problems.of(RestClientResponseException)` gives the problem detail the other service answered (or
empty for an HTML page or no body), `Problems.code(problem)` its code and
`Problems.subErrors(problem)` a third party's codes. Never parse a problem by hand.

## Beans you can replace

| Bean | Replace with |
|---|---|
| `ProblemDetailsAdvice` | any `ResponseEntityExceptionHandler` of your own (the library's backs off, and Boot's too) |
| `ProblemSecurityHandler` | your own; the security library uses it for 401 and 403 |

## Tests

- `./gradlew test`: `ProblemDetailsAdviceTest` is the specification, one nested group per
  behaviour (own errors, invalid input, framework errors, the answer, logging). Beside it:
  `GeneralException`, message placeholders, the security handler, `Problems`, and
  `ErrorDefinitions` against fixture definitions.
- `./gradlew integrationTest`: on a real server with Spring Security and Boot's
  own problem-details handler switched on, and `Problems` against WireMock.
- Every service runs `ErrorDefinitions.assertComplete("com.example")` in a unit test: each code
  has a status, a title and a message in all three languages with the same placeholders, and no
  definition is left over.
