package com.epam.training.workload.component;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(properties = "spring.activemq.broker-url=vm://workload-component?broker.persistent=false&broker.useJmx=false")
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {
}
