package support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BasicAuthHeader {

    private BasicAuthHeader() {
    }

    public static String value(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}