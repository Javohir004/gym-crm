package com.epam.training.gym;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            GymFacade facade = context.getBean(GymFacade.class);

            log.info("Seeded trainees: {}", facade.selectAllTrainees().size());
            log.info("Seeded trainers: {}", facade.selectAllTrainers().size());
            log.info("Seeded trainings: {}", facade.selectAllTrainings().size());

            Trainee trainee = facade.createTrainee(Trainee.builder()
                    .firstName("John")
                    .lastName("Doe")               // duplicate ataylab -> John.Doe1
                    .dateOfBirth(LocalDate.of(2000, 1, 15))
                    .address("Tashkent, Chilonzor")
                    .build());
            log.info("New trainee -> username={}", trainee.getUsername());

            Trainer trainer = facade.createTrainer(Trainer.builder()
                    .firstName("Anna")
                    .lastName("Lee")               // duplicate ataylab-> Anna.Lee1
                    .specialization(new TrainingType(1L, "Yoga"))
                    .build());
            log.info("New trainer -> username={}", trainer.getUsername());


            trainer.setSpecialization(new TrainingType(2L, "Fitness"));
            facade.updateTrainer(trainer);
            log.info("Trainer after update: specialization={}",
                    facade.selectTrainer(trainer.getUserId())
                            .map(t -> t.getSpecialization().getTrainingTypeName())
                            .orElse("-"));

            Training training = facade.createTraining(Training.builder()
                    .traineeId(trainee.getUserId())
                    .trainerId(trainer.getUserId())
                    .trainingName("Evening Yoga")
                    .trainingType(new TrainingType(1L, "Yoga"))
                    .trainingDate(LocalDate.now())
                    .trainingDuration(60)
                    .build());
            log.info("New training -> id={}, name='{}'", training.getTrainingId(), training.getTrainingName());

            trainee.setAddress("Tashkent, Yunusobod");
            facade.updateTrainee(trainee);
            log.info("Trainee after update: address={}",
                    facade.selectTrainee(trainee.getUserId()).map(Trainee::getAddress).orElse("-"));

            facade.deleteTrainee(trainee.getUserId());
            log.info("Trainee exists after delete: {}", facade.selectTrainee(trainee.getUserId()).isPresent());
        }
    }
}