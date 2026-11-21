package com.epam.training.integration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = IntegrationEnvironment.class)
public class CucumberSpringConfiguration {
}
