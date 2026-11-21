package com.epam.training.integration;

import com.epam.training.gym.GymCrmApplication;
import com.epam.training.workload.WorkloadServiceApplication;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class IntegrationEnvironment {

    static final String BROKER_URL = "vm://integration?create=false";
    static final String SERVICE_SECRET = "integration-workload-secret-0123456789-abcdefghij";

    private static final String JPA_AUTO_CONFIGURATION = String.join(",",
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");
    private static final String MONGO_AUTO_CONFIGURATION = String.join(",",
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration",
            "de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration");

    public record RunningService(ConfigurableApplicationContext context, String baseUrl) {
        public void close() {
            context.close();
        }
    }

    @Bean(destroyMethod = "stop")
    public BrokerService broker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName("integration");
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        broker.waitUntilStarted();
        return broker;
    }

    @Bean
    public JmsTemplate testJmsTemplate(BrokerService broker) {
        return new JmsTemplate(new ActiveMQConnectionFactory(BROKER_URL));
    }

    @Bean(destroyMethod = "close")
    public RunningService workloadService(BrokerService broker) {
        return start(WorkloadServiceApplication.class, List.of(
                "--workload.security.jwt.secret=" + SERVICE_SECRET,
                "--workload.jms.redelivery-delay-ms=50",
                "--de.flapdoodle.mongodb.embedded.version=7.0.14",
                "--spring.data.mongodb.port=0",
                "--spring.autoconfigure.exclude=" + JPA_AUTO_CONFIGURATION));
    }

    @Bean(destroyMethod = "close")
    public RunningService gymCrm(BrokerService broker, RunningService workloadService) {
        return start(GymCrmApplication.class, List.of(
                "--spring.profiles.active=local",
                "--spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1",
                "--gym.workload.jwt.secret=" + SERVICE_SECRET,
                "--spring.autoconfigure.exclude=" + MONGO_AUTO_CONFIGURATION));
    }

    private static RunningService start(Class<?> application, List<String> specificArgs) {
        List<String> args = new ArrayList<>(List.of(
                "--spring.config.location=optional:" + configDirectory(application),
                "--server.port=0",
                "--eureka.client.enabled=false",
                "--spring.cloud.discovery.enabled=false",
                "--spring.activemq.broker-url=" + BROKER_URL));
        args.addAll(specificArgs);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(application)
                .run(args.toArray(String[]::new));
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        return new RunningService(context, "http://localhost:" + port);
    }

    private static String configDirectory(Class<?> application) {
        String location = application.getProtectionDomain().getCodeSource().getLocation().toString();
        return location.endsWith(".jar") ? "jar:" + location + "!/" : location;
    }
}
