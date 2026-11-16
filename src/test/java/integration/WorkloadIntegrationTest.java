package integration;

import com.epam.training.gym.GymCrmApplication;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.workload.WorkloadMessagePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:workload-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("local")
class WorkloadIntegrationTest {

    private static final String QUEUE = "trainer.workload";

    @Autowired
    private TraineeService traineeService;
    @Autowired
    private TrainerService trainerService;
    @Autowired
    private TrainingService trainingService;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void drainQueue() {
        jmsTemplate.setReceiveTimeout(200);
        while (jmsTemplate.receive(QUEUE) != null) {
        }
    }

    private List<Message> receiveAll() {
        jmsTemplate.setReceiveTimeout(2000);
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = jmsTemplate.receive(QUEUE)) != null) {
            messages.add(message);
            jmsTemplate.setReceiveTimeout(500);
        }
        return messages;
    }

    private JsonNode json(Message message) throws JMSException, com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapper.readTree(((TextMessage) message).getText());
    }

    @Test
    void addingTraining_queuesAdd_andDeletingTrainee_queuesDeleteForUpcomingTrainingsOnly() throws Exception {
        Trainee trainee = traineeService.createTrainee("Queue", "Student", null, null).profile();
        Trainer trainer = trainerService.createTrainer("Queue", "Coach", "Yoga").profile();
        String traineeName = trainee.getUser().getUsername();
        String trainerName = trainer.getUser().getUsername();
        LocalDate past = LocalDate.now().minusDays(10);
        LocalDate upcoming = LocalDate.now().plusDays(10);

        trainingService.addTraining(traineeName, trainerName, "Past session", past, 30);
        trainingService.addTraining(traineeName, trainerName, "Next session", upcoming, 60);
        traineeService.delete(traineeName);

        List<Message> messages = receiveAll();
        assertEquals(3, messages.size());

        JsonNode cancelled = json(messages.get(2));
        assertEquals("DELETE", cancelled.get("actionType").asText());
        assertEquals(upcoming.toString(), cancelled.get("trainingDate").asText());
        assertEquals(60, cancelled.get("trainingDuration").asInt());
        assertEquals(trainerName, cancelled.get("trainerUsername").asText());
        assertEquals("Coach", cancelled.get("trainerLastName").asText());
        assertTrue(cancelled.get("isActive").asBoolean());

        Message first = messages.get(0);
        assertEquals("ADD", json(first).get("actionType").asText());
        assertNotNull(first.getStringProperty(WorkloadMessagePublisher.TOKEN_PROPERTY));
        assertEquals(trainerName, first.getStringProperty(WorkloadMessagePublisher.GROUP_ID_PROPERTY));
    }

    @Test
    void failedTransaction_queuesNothing() {
        assertThrows(NotFoundException.class,
                () -> trainingService.addTraining("No.Trainee", "No.Trainer", "Ghost", LocalDate.now(), 30));

        jmsTemplate.setReceiveTimeout(500);
        assertNull(jmsTemplate.receive(QUEUE));
    }
}
