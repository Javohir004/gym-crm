package component;

import com.epam.training.gym.GymCrmApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(classes = GymCrmApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:gym-component;DB_CLOSE_DELAY=-1",
                "spring.activemq.broker-url=vm://gym-component?broker.persistent=false&broker.useJmx=false"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(GymWorld.class)
public class CucumberSpringConfiguration {
}
