package util;

import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.util.BasicAuthUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import support.BasicAuthHeader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicAuthUtilTest {

    private static MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return request;
    }

    @Test
    void extract_decodesUsernameAndPassword_passwordMayContainColon() {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(requestWith(BasicAuthHeader.value("John.Doe", "a:b")));

        assertEquals("John.Doe", credentials.username());
        assertEquals("a:b", credentials.password());
    }

    @Test
    void extract_throws_whenHeaderIsMissingOrNotBasic() {
        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extract(requestWith(null)));
        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extract(requestWith("Bearer abc")));
    }

    @Test
    void extract_throws_whenHeaderIsNotValidBase64() {
        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extract(requestWith("Basic %%%")));
    }

    @Test
    void extract_throws_whenDecodedValueHasNoColon() {
        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extract(requestWith("Basic Sm9obg==")));
    }

    @Test
    void credentialsToString_neverContainsThePassword() {
        assertFalse(new BasicAuthUtil.Credentials("John.Doe", "secret123").toString().contains("secret123"));
    }

    @Test
    void extractFor_returnsCredentials_whenHeaderUserMatchesUrlAndBody() {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extractFor(
                requestWith(BasicAuthHeader.value("John.Doe", "pw")), "John.Doe", "John.Doe", null);

        assertEquals("pw", credentials.password());
    }

    @Test
    void extractFor_throws_whenHeaderBelongsToAnotherUser() {
        MockHttpServletRequest request = requestWith(BasicAuthHeader.value("Other.User", "pw"));

        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extractFor(request, "John.Doe"));
    }

    @Test
    void extractFor_throws_whenBodyNamesAnotherUser() {
        MockHttpServletRequest request = requestWith(BasicAuthHeader.value("John.Doe", "pw"));

        assertThrows(AuthenticationException.class, () -> BasicAuthUtil.extractFor(request, "John.Doe", "Other.User"));
    }
}
