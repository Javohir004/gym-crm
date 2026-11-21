package com.epam.training.workload.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String SERVICE_ROLE = "ROLE_SERVICE";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenAuthenticationFilter.class);

    private final ServiceTokenValidator validator;

    public ServiceTokenAuthenticationFilter(ServiceTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            validator.validate(header.substring(BEARER_PREFIX.length())).ifPresent(caller -> {
                var authentication = new UsernamePasswordAuthenticationToken(
                        caller, null, List.of(new SimpleGrantedAuthority(SERVICE_ROLE)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated calling service={}", caller);
            });
        }
        chain.doFilter(request, response);
    }
}
