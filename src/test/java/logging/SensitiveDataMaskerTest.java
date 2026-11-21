package logging;

import com.epam.training.gym.logging.SensitiveDataMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataMaskerTest {

    @Test
    void masksEveryPasswordAndTokenField_andKeepsTheRest() {
        String json = "{\"username\":\"John.Doe\",\"oldPassword\":\"old1\",\"newPassword\":\"new2\","
                + "\"password\":\"p3\",\"token\":\"t4\"}";

        assertEquals("{\"username\":\"John.Doe\",\"oldPassword\":\"***\",\"newPassword\":\"***\","
                + "\"password\":\"***\",\"token\":\"***\"}", SensitiveDataMasker.mask(json));
    }

    @Test
    void handlesSpacesAndEscapedQuotesInsideTheValue() {
        assertEquals("{ \"password\" : \"***\", \"a\":1}",
                SensitiveDataMasker.mask("{ \"password\" : \"pa\\\"ss\", \"a\":1}"));
    }

    @Test
    void leavesTextWithoutSensitiveFieldsUnchanged() {
        String json = "{\"trainingName\":\"Yoga\",\"passwordHint\":\"none\"}";

        assertEquals(json, SensitiveDataMasker.mask(json));
    }
}
