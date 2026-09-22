package com.epam.training.gym.storage;

import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StorageInitializer implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(StorageInitializer.class);

    private String traineeDataFile;
    private String trainerDataFile;
    private String trainingDataFile;

    @Value("${trainee.data.file}")
    public void setTraineeDataFile(String traineeDataFile) {
        this.traineeDataFile = traineeDataFile;
    }

    @Value("${trainer.data.file}")
    public void setTrainerDataFile(String trainerDataFile) {
        this.trainerDataFile = trainerDataFile;
    }

    @Value("${training.data.file}")
    public void setTrainingDataFile(String trainingDataFile) {
        this.trainingDataFile = trainingDataFile;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof TraineeStorage traineeStorage) {
            loadTrainees(traineeStorage);
        } else if (bean instanceof TrainerStorage trainerStorage) {
            loadTrainers(trainerStorage);
        } else if (bean instanceof TrainingStorage trainingStorage) {
            loadTrainings(trainingStorage);
        }
        return bean;
    }

    private void loadTrainees(TraineeStorage storage) {
        for (String line : readDataLines(traineeDataFile)) {
            String[] c = line.split(",");
            Trainee trainee = Trainee.builder()
                    .userId(Long.parseLong(c[0].trim()))
                    .firstName(c[1].trim())
                    .lastName(c[2].trim())
                    .username(c[3].trim())
                    .password(c[4].trim())
                    .isActive(Boolean.parseBoolean(c[5].trim()))
                    .dateOfBirth(c[6].isBlank() ? null : LocalDate.parse(c[6].trim()))
                    .address(c[7].trim())
                    .build();
            storage.save(trainee.getUserId(), trainee);
        }
        log.info("StorageInitializer: seeded {} trainee(s) from {}", storage.size(), traineeDataFile);
    }

    private void loadTrainers(TrainerStorage storage) {
        for (String line : readDataLines(trainerDataFile)) {
            String[] c = line.split(",");
            TrainingType specialization = new TrainingType(Long.parseLong(c[6].trim()), c[7].trim());
            Trainer trainer = Trainer.builder()
                    .userId(Long.parseLong(c[0].trim()))
                    .firstName(c[1].trim())
                    .lastName(c[2].trim())
                    .username(c[3].trim())
                    .password(c[4].trim())
                    .isActive(Boolean.parseBoolean(c[5].trim()))
                    .specialization(specialization)
                    .build();
            storage.save(trainer.getUserId(), trainer);
        }
        log.info("StorageInitializer: seeded {} trainer(s) from {}", storage.size(), trainerDataFile);
    }

    private void loadTrainings(TrainingStorage storage) {
        for (String line : readDataLines(trainingDataFile)) {
            String[] c = line.split(",");
            TrainingType trainingType = new TrainingType(Long.parseLong(c[4].trim()), c[5].trim());
            Training training = Training.builder()
                    .trainingId(Long.parseLong(c[0].trim()))
                    .traineeId(Long.parseLong(c[1].trim()))
                    .trainerId(Long.parseLong(c[2].trim()))
                    .trainingName(c[3].trim())
                    .trainingType(trainingType)
                    .trainingDate(LocalDate.parse(c[6].trim()))
                    .trainingDuration(Integer.parseInt(c[7].trim()))
                    .build();
            storage.save(training.getTrainingId(), training);
        }
        log.info("StorageInitializer: seeded {} training(s) from {}", storage.size(), trainingDataFile);
    }

    private List<String> readDataLines(String classpathLocation) {
        String resourcePath = classpathLocation.replace("classpath:", "");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("StorageInitializer: data file not found on classpath: {}", classpathLocation);
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .skip(1) // header row
                        .filter(l -> !l.isBlank())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("StorageInitializer: failed to read data file {}", classpathLocation, e);
            return List.of();
        }
    }
}