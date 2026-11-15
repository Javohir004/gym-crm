package com.epam.training.gym.workload;

import com.epam.training.gym.logging.TransactionIdFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class WorkloadClientInterceptor implements RequestInterceptor {

    private final ServiceTokenProvider tokenProvider;

    public WorkloadClientInterceptor(ServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getToken());
        String transactionId = MDC.get(TransactionIdFilter.MDC_KEY);
        if (transactionId != null) {
            template.header(TransactionIdFilter.HEADER_NAME, transactionId);
        }
    }
}
