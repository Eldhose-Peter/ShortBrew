package com.epproject.ShortBrew.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that extracts or generates a unique request_id (and OpenTelemetry trace_id/span_id)
 * for every incoming HTTP request and binds them to the SLF4J MDC context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "request_id";
    public static final String MDC_TRACE_ID_KEY = "trace_id";
    public static final String MDC_SPAN_ID_KEY = "span_id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = httpRequest.getHeader("x-request-id");
            }
            if (requestId == null || requestId.isBlank()) {
                requestId = httpRequest.getHeader("X-Correlation-ID");
            }
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_REQUEST_ID_KEY, requestId);
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            // Extract OpenTelemetry traceparent (00-{trace_id}-{span_id}-01) if present
            String traceId = httpRequest.getHeader("x-trace-id");
            if (traceId == null || traceId.isBlank()) {
                traceId = httpRequest.getHeader("trace_id");
            }
            String spanId = httpRequest.getHeader("x-span-id");
            if (spanId == null || spanId.isBlank()) {
                spanId = httpRequest.getHeader("span_id");
            }

            String traceparent = httpRequest.getHeader("traceparent");
            if (traceparent != null && !traceparent.isBlank()) {
                String[] parts = traceparent.split("-");
                if (parts.length >= 4) {
                    if (traceId == null || traceId.isBlank()) traceId = parts[1];
                    if (spanId == null || spanId.isBlank()) spanId = parts[2];
                }
            }

            if (traceId != null && !traceId.isBlank()) {
                MDC.put(MDC_TRACE_ID_KEY, traceId);
            }
            if (spanId != null && !spanId.isBlank()) {
                MDC.put(MDC_SPAN_ID_KEY, spanId);
            }

            long startTime = System.currentTimeMillis();
            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("HTTP {} {} - status={} duration={}ms",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        httpResponse.getStatus(),
                        duration
                );
                MDC.remove(MDC_REQUEST_ID_KEY);
                MDC.remove(MDC_TRACE_ID_KEY);
                MDC.remove(MDC_SPAN_ID_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
