package com.epam.training.gym.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Per-call logging (task note 17.2): which endpoint was called, and what the
 * service responded with (status code). Must run AFTER TransactionIdFilter,
 * so these two log lines carry the transactionId too. Request/response
 * bodies are intentionally not logged here, since some of them (login,
 * change password) carry a password - logging the status code is enough to
 * know whether the call succeeded.
 */
public class RestCallLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RestCallLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        log.info("REST call received: {} {}", method, uri);

        chain.doFilter(request, response);

        log.info("REST call finished: {} {} -> status={}", method, uri, httpResponse.getStatus());
    }
}