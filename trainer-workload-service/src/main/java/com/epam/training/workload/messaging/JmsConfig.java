package com.epam.training.workload.messaging;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
public class JmsConfig {

    private static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    @Bean
    public ActiveMQConnectionFactoryCustomizer redeliveryPolicy(
            @Value("${workload.jms.max-attempts}") int maxAttempts,
            @Value("${workload.jms.redelivery-delay-ms}") long redeliveryDelayMs) {
        return factory -> {
            RedeliveryPolicy policy = factory.getRedeliveryPolicy();
            policy.setMaximumRedeliveries(maxAttempts);
            policy.setInitialRedeliveryDelay(redeliveryDelayMs);
            policy.setRedeliveryDelay(redeliveryDelayMs);
            policy.setUseExponentialBackOff(true);
            policy.setBackOffMultiplier(2);
        };
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        factory.setErrorHandler(error -> log.warn("Message processing failed, it will be redelivered: {}",
                error.getMessage()));
        return factory;
    }
}
