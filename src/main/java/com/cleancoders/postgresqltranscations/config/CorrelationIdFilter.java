package com.cleancoders.postgresqltranscations.config;

import com.cleancoders.postgresqltranscations.context.RequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/// Servlet filter that binds a **Scoped Value** for the correlation ID.
///
/// **Java 25 feature used:** Scoped Values (JEP 506) — `ScopedValue.callWhere`
///
/// Lifecycle per request:
/// 1. Read `X-Correlation-Id` from the incoming HTTP header (or generate a UUID).
/// 2. Echo the ID back in the HTTP response header for distributed tracing.
/// 3. Bind [RequestContext#CORRELATION_ID] via `ScopedValue.callWhere` for the
///    duration of the entire filter chain — including every service method and
///    fallback called downstream.
///
/// Because scoped values propagate automatically into child virtual threads forked
/// via `StructuredTaskScope.fork()`, this works transparently with
/// **Virtual Threads** (JEP 444) and **Structured Concurrency** (JEP 505).
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        var httpReq  = (HttpServletRequest)  req;
        var httpResp = (HttpServletResponse) resp;

        // Java 22: var in every local declaration where type is obvious
        String correlationId = resolveCorrelationId(httpReq);
        httpResp.setHeader(CORRELATION_HEADER, correlationId);

        // Java 25: ScopedValue.callWhere — binds CORRELATION_ID immutably for this
        // Java 25: ScopedValue.where(key, value).call(callable) — binds CORRELATION_ID
        // immutably for the duration of this request's call-stack.
        // Carrier.call() propagates the binding into every virtual thread forked here
        // and throws Exception, which we unwrap to the specific servlet types below.
        try {
            ScopedValue.where(RequestContext.CORRELATION_ID, correlationId)
                       .call(() -> {
                           chain.doFilter(req, resp);
                           return null;
                       });
        } catch (IOException | ServletException e) {
            throw e;                                        // rethrow servlet exceptions as-is
        } catch (Exception e) {
            throw new ServletException("Unexpected error in filter chain", e);
        }
    }

    /// Resolves the correlation ID: uses the incoming header value when present and
    /// non-blank, otherwise generates a fresh random [UUID].
    private static String resolveCorrelationId(HttpServletRequest req) {
        String header = req.getHeader(CORRELATION_HEADER);
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }
}

