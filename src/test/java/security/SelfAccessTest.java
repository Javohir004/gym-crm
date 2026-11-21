package security;

import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.ForbiddenException;
import com.epam.training.gym.security.SelfAccess;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelfAccessTest {

    private static final Principal JOHN = () -> "John.Doe";

    @Test
    void require_passes_forOwnUsername() {
        assertDoesNotThrow(() -> SelfAccess.require(JOHN, "John.Doe"));
    }

    @Test
    void require_throwsForbidden_forSomeoneElse() {
        assertThrows(ForbiddenException.class, () -> SelfAccess.require(JOHN, "Anna.Lee"));
    }

    @Test
    void require_throwsAuthentication_whenNobodyIsLoggedIn() {
        assertThrows(AuthenticationException.class, () -> SelfAccess.require(null, "John.Doe"));
    }

    @Test
    void requireWithBody_passes_whenBodyUsernameMatchesOrIsMissing() {
        assertDoesNotThrow(() -> SelfAccess.require(JOHN, "John.Doe", "John.Doe"));
        assertDoesNotThrow(() -> SelfAccess.require(JOHN, "John.Doe", null));
    }

    @Test
    void requireWithBody_throwsForbidden_whenBodyUsernameDiffers() {
        assertThrows(ForbiddenException.class, () -> SelfAccess.require(JOHN, "John.Doe", "Anna.Lee"));
    }

    @Test
    void requireWithBody_throwsForbidden_whenPathIsSomeoneElse() {
        assertThrows(ForbiddenException.class, () -> SelfAccess.require(JOHN, "Anna.Lee", "Anna.Lee"));
    }
}