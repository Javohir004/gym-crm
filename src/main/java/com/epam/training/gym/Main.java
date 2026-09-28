package com.epam.training.gym;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            GymFacade facade = context.getBean(GymFacade.class);

            // ---- 1, 2. Create profiles ----
            step("1-2. Create Trainer and Trainee profiles");
            Trainer anna = facade.createTrainer("Anna", "Lee", "Yoga");
            Trainer mark = facade.createTrainer("Mark", "King", "Fitness");
            Trainee john = facade.createTrainee("John", "Doe", LocalDate.of(2000, 1, 15), "Tashkent, Chilonzor");
            Trainee john2 = facade.createTrainee("John", "Doe", LocalDate.of(1999, 5, 20), "Samarkand"); // same name -> serial number

            String annaUser = anna.getUser().getUsername();
            String annaPass = anna.getUser().getPassword();
            String markUser = mark.getUser().getUsername();
            String johnUser = john.getUser().getUsername();
            String johnPass = john.getUser().getPassword();
            String john2User = john2.getUser().getUsername();
            String john2Pass = john2.getUser().getPassword();
            log.info("Generated usernames: {}, {}, {}, {}", annaUser, markUser, johnUser, john2User);

            // ---- Validation and authentication rules ----
            step("Validation and authentication");
            try {
                facade.createTrainee(" ", "Doe", null, null);
            } catch (ValidationException e) {
                log.warn("Rejected as expected: {}", e.getMessage());
            }
            try {
                facade.getTrainee(johnUser, "wrong-password");
            } catch (AuthenticationException e) {
                log.warn("Rejected as expected: {}", e.getMessage());
            }

            // ---- 3, 4. Username and password matching ----
            step("3-4. Username and password matching");
            log.info("Trainee, correct password: {}", facade.traineeCredentialsMatch(johnUser, johnPass));
            log.info("Trainee, wrong password:   {}", facade.traineeCredentialsMatch(johnUser, "wrong-password"));
            log.info("Trainer, correct password: {}", facade.trainerCredentialsMatch(annaUser, annaPass));
            log.info("Trainer, wrong password:   {}", facade.trainerCredentialsMatch(annaUser, "wrong-password"));

            // ---- 5, 6. Select profile by username ----
            step("5-6. Select profile by username");
            log.info("Trainer {} specialization: {}", annaUser,
                    facade.getTrainer(annaUser, annaPass).getSpecialization().getTrainingTypeName());
            log.info("Trainee {} address: {}", johnUser, facade.getTrainee(johnUser, johnPass).getAddress());

            // ---- 9, 10. Update profile ----
            step("9-10. Update profiles");
            facade.updateTrainer(annaUser, annaPass, "Anna", "Lee", "Stretching");
            log.info("Trainer {} new specialization: {}", annaUser,
                    facade.getTrainer(annaUser, annaPass).getSpecialization().getTrainingTypeName());
            facade.updateTrainee(johnUser, johnPass, "John", "Doe", LocalDate.of(2000, 1, 15), "Tashkent, Yunusobod");
            log.info("Trainee {} new address: {}", johnUser, facade.getTrainee(johnUser, johnPass).getAddress());

            // ---- 7, 8. Change password ----
            step("7-8. Change password");
            String oldJohnPass = johnPass;
            johnPass = "NewTraineePass1";
            facade.changeTraineePassword(johnUser, oldJohnPass, johnPass);
            log.info("Trainee, old password still works: {}", facade.traineeCredentialsMatch(johnUser, oldJohnPass));
            log.info("Trainee, new password works:       {}", facade.traineeCredentialsMatch(johnUser, johnPass));
            String oldAnnaPass = annaPass;
            annaPass = "NewTrainerPass1";
            facade.changeTrainerPassword(annaUser, oldAnnaPass, annaPass);
            log.info("Trainer, old password still works: {}", facade.trainerCredentialsMatch(annaUser, oldAnnaPass));
            log.info("Trainer, new password works:       {}", facade.trainerCredentialsMatch(annaUser, annaPass));

            // ---- 11, 12. Activate / De-activate (each call switches the state) ----
            step("11-12. Activate / De-activate");
            log.info("Trainee active after 1st call: {}", facade.toggleTraineeActive(johnUser, johnPass));
            log.info("Trainee active after 2nd call: {}", facade.toggleTraineeActive(johnUser, johnPass));
            log.info("Trainer active after 1st call: {}", facade.toggleTrainerActive(annaUser, annaPass));
            log.info("Trainer active after 2nd call: {}", facade.toggleTrainerActive(annaUser, annaPass));

            // ---- 17, 18. Trainee's trainers ----
            step("17-18. Trainee's trainers list");
            log.info("Not assigned to {}: {}", johnUser, trainerNames(facade.getUnassignedTrainers(johnUser, johnPass)));
            facade.updateTraineeTrainers(johnUser, johnPass, List.of(annaUser));
            log.info("After assigning {}, not assigned: {}", annaUser,
                    trainerNames(facade.getUnassignedTrainers(johnUser, johnPass)));
            try {
                facade.updateTraineeTrainers(johnUser, johnPass, List.of("No.Such"));
            } catch (NotFoundException e) {
                log.warn("Rejected as expected: {}", e.getMessage());
            }

            // ---- 16. Add training ----
            step("16. Add training");
            facade.addTraining(johnUser, johnPass, new TrainingRequest(
                    johnUser, annaUser, "Morning Yoga", "Yoga", LocalDate.of(2026, 9, 1), 60));
            facade.addTraining(johnUser, johnPass, new TrainingRequest(
                    johnUser, markUser, "Strength Basics", "Fitness", LocalDate.of(2026, 9, 15), 45));
            facade.addTraining(john2User, john2Pass, new TrainingRequest(
                    john2User, annaUser, "Evening Stretch", "Stretching", LocalDate.of(2026, 9, 20), 30));
            log.info("3 trainings added");

            // ---- 14. Trainee's trainings by criteria ----
            step("14. Trainee's trainings");
            log.info("All:                  {}", trainingNames(facade.getTraineeTrainings(johnUser, johnPass, null, null, null, null)));
            log.info("From 10 Sep to 30 Sep: {}", trainingNames(facade.getTraineeTrainings(
                    johnUser, johnPass, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 30), null, null)));
            log.info("Trainer name 'anna':  {}", trainingNames(facade.getTraineeTrainings(johnUser, johnPass, null, null, "anna", null)));
            log.info("Type 'Fitness':       {}", trainingNames(facade.getTraineeTrainings(johnUser, johnPass, null, null, null, "Fitness")));

            // ---- 15. Trainer's trainings by criteria ----
            step("15. Trainer's trainings");
            log.info("All:                  {}", trainingNames(facade.getTrainerTrainings(annaUser, annaPass, null, null, null)));
            log.info("From 15 Sep:          {}", trainingNames(facade.getTrainerTrainings(
                    annaUser, annaPass, LocalDate.of(2026, 9, 15), null, null)));
            log.info("Trainee name 'john':  {}", trainingNames(facade.getTrainerTrainings(annaUser, annaPass, null, null, "john")));

            // ---- 13. Delete trainee (hard delete + cascade to trainings) ----
            step("13. Delete trainee");
            facade.deleteTrainee(johnUser, johnPass);
            log.info("Deleted trainee still can log in: {}", facade.traineeCredentialsMatch(johnUser, johnPass));
            log.info("Trainer {} trainings after delete: {}", annaUser,
                    trainingNames(facade.getTrainerTrainings(annaUser, annaPass, null, null, null)));
        }
    }

    private static void step(String title) {
        log.info("=================== {} ===================", title);
    }

    private static List<String> trainerNames(List<Trainer> trainers) {
        return trainers.stream().map(trainer -> trainer.getUser().getUsername()).toList();
    }

    private static List<String> trainingNames(List<Training> trainings) {
        return trainings.stream().map(Training::getTrainingName).toList();
    }
}