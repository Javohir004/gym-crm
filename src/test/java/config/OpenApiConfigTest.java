package config;

import com.epam.training.gym.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiConfigTest {

    @Test
    void openApi_declaresBearerJwtScheme() {
        OpenAPI api = new OpenApiConfig().gymOpenAPI();

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("bearerAuth");

        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertEquals("Gym CRM API", api.getInfo().getTitle());
    }
}