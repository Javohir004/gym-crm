package com.epam.training.gym.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter implements Filter {

    public static final String HEADER_NAME = "X-Transaction-Id";
    public static final String MDC_KEY = "transactionId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String transactionId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, transactionId);
        try {
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader(HEADER_NAME, transactionId);
            }
            chain.doFilter(request, response);
        } finally {
            // Always clear - this thread will be reused for the next request.
            MDC.remove(MDC_KEY);
        }
    }
}