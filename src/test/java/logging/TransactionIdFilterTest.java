package logging;

import com.epam.training.gym.logging.TransactionIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionIdFilterTest {

    private final TransactionIdFilter filter = new TransactionIdFilter();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void doFilter_putsATransactionIdInMdc_duringTheCall() throws IOException, ServletException {
        String[] seenDuringChain = new String[1];
        doAnswerCaptureMdc(seenDuringChain);

        filter.doFilter(request, response, chain);

        assertNotNull(seenDuringChain[0], "transactionId must be set in MDC while the chain runs");
    }

    @Test
    void doFilter_clearsMdc_afterTheCall() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertNull(MDC.get(TransactionIdFilter.MDC_KEY), "MDC must be cleared so the next request on this thread starts clean");
    }

    @Test
    void doFilter_setsTheResponseHeader_sameValueAsMdc() throws IOException, ServletException {
        String[] seenDuringChain = new String[1];
        doAnswerCaptureMdc(seenDuringChain);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(TransactionIdFilter.HEADER_NAME, seenDuringChain[0]);
    }

    @Test
    void doFilter_generatesADifferentId_onEachCall() throws IOException, ServletException {
        String[] first = new String[1];
        doAnswerCaptureMdc(first);
        filter.doFilter(request, response, chain);

        String[] second = new String[1];
        doAnswerCaptureMdc(second);
        filter.doFilter(request, response, chain);

        assertTrue(!first[0].equals(second[0]));
    }

    private void doAnswerCaptureMdc(String[] out) throws IOException, ServletException {
        org.mockito.Mockito.doAnswer(invocation -> {
            out[0] = MDC.get(TransactionIdFilter.MDC_KEY);
            return null;
        }).when(chain).doFilter(request, response);
    }
}