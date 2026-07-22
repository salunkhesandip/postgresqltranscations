package com.cleancoders.postgresqltranscations.context;

/// Holds per-request **Scoped Values** — immutable, inheritable-by-child-threads
/// values bound for the lifetime of one HTTP request.
///
/// **Java 25 feature used:** Scoped Values (JEP 506)
///
/// Scoped values are bound by [com.cleancoders.postgresqltranscations.config.CorrelationIdFilter]
/// and are readable anywhere in the call stack — including circuit-breaker
/// fallback methods — without passing parameters explicitly.
/// Unlike `ThreadLocal`, scoped values are **immutable** once bound, which
/// eliminates accidental mutation bugs and plays well with virtual threads.
///
/// ### Usage
/// ```java
/// // Reading inside any service method or fallback:
/// String reqId = RequestContext.CORRELATION_ID.orElse("n/a");
/// ```
public final class RequestContext {

    /// Correlation ID propagated from the `X-Correlation-Id` HTTP request header,
    /// or a freshly-generated UUID when the header is absent.
    /// Echoed back in the response header so callers can trace distributed logs.
    ///
    /// `ScopedValue` is in `java.lang` — no import required.
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private RequestContext() {
        // utility class — no instances
    }
}

