package com.epam.training.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void eurekaRegistry_isServed() {
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
