package com.epam.training.gym.model;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "trainingTypeId")
public class TrainingType {

    private Long trainingTypeId;
    private String trainingTypeName;
}