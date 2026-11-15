package workload;

import com.epam.training.gym.logging.TransactionIdFilter;
import com.epam.training.gym.workload.ServiceTokenProvider;
import com.epam.training.gym.workload.WorkloadClientInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadClientInterceptorTest {

    @Mock
    private ServiceTokenProvider tokenProvider;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void addsBearerToken_andPassesTheTransactionIdDownstream() {
        when(tokenProvider.getToken()).thenReturn("service-token");
        MDC.put(TransactionIdFilter.MDC_KEY, "tx-42");
        RequestTemplate template = new RequestTemplate();

        new WorkloadClientInterceptor(tokenProvider).apply(template);

        assertEquals(List.of("Bearer service-token"), List.copyOf(template.headers().get("Authorization")));
        assertEquals(List.of("tx-42"), List.copyOf(template.headers().get(TransactionIdFilter.HEADER_NAME)));
    }

    @Test
    void outsideARequest_sendsNoTransactionIdHeader() {
        when(tokenProvider.getToken()).thenReturn("service-token");
        RequestTemplate template = new RequestTemplate();

        new WorkloadClientInterceptor(tokenProvider).apply(template);

        assertFalse(template.headers().containsKey(TransactionIdFilter.HEADER_NAME));
    }
}
