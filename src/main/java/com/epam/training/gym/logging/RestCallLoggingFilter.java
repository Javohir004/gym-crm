package com.epam.training.gym.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RestCallLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RestCallLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        log.info("REST call received: {}", endpoint);
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            int status = wrappedResponse.getStatus();
            String requestBody = body(wrappedRequest.getContentAsByteArray(), wrappedRequest.getContentType());
            String responseBody = body(wrappedResponse.getContentAsByteArray(), wrappedResponse.getContentType());
            if (status >= 400) {
                log.warn("REST call finished: {} request={} -> status={} response={}",
                        endpoint, requestBody, status, responseBody);
            } else {
                log.info("REST call finished: {} request={} -> status={} response={}",
                        endpoint, requestBody, status, responseBody);
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    private static String body(byte[] content, String contentType) {
        if (content.length == 0) {
            return "<empty>";
        }
        if (contentType == null || !contentType.contains("json")) {
            return "<" + content.length + " bytes of " + contentType + ">";
        }
        String text = SensitiveDataMasker.mask(new String(content, StandardCharsets.UTF_8));
        return text.length() > MAX_BODY_LENGTH ? text.substring(0, MAX_BODY_LENGTH) + "...(truncated)" : text;
    }
}
