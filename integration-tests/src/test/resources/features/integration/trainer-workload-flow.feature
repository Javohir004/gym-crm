Feature: Trainer workload flows from gym-crm to the trainer workload service
  As a gym manager
  I want every training added or cancelled in gym-crm to reach the trainer workload service through ActiveMQ
  So that the trainer's monthly summary in MongoDB is always correct

  Background:
    Given a trainee is registered in gym-crm
    And a trainer with specialization "Yoga" is registered in gym-crm

  Scenario: An added training appears in the trainer's monthly workload
    When the trainee adds a training 10 days from now for 60 minutes in gym-crm
    Then gym-crm responds with status 200
    And the workload service shows 60 minutes for the trainer in the month of the training

  Scenario: Two trainings on the same day are summed up
    When the trainee adds a training 10 days from now for 60 minutes in gym-crm
    And the trainee adds a training 10 days from now for 30 minutes in gym-crm
    Then gym-crm responds with status 200
    And the workload service shows 90 minutes for the trainer in the month of the training

  Scenario: Deleting the trainee cancels the upcoming training in the workload
    Given the trainee has a training 10 days from now for 60 minutes that reached the workload service
    When the trainee deletes their profile in gym-crm
    Then gym-crm responds with status 200
    And the workload service shows 0 minutes for the trainer in the month of the training

  Scenario: A training for an unknown trainer never reaches the workload service
    When the trainee adds a training 10 days from now for 60 minutes with trainer "Ghost.Trainer" in gym-crm
    Then gym-crm responds with status 404
    And the workload service has no summary for trainer "Ghost.Trainer"

  Scenario: A training added without a token never reaches the workload service
    When a training for the trainer is added in gym-crm without a token
    Then gym-crm responds with status 401
    And the workload service has no summary for the trainer

  Scenario: A message not issued by gym-crm is moved to the dead letter queue
    When a workload message for the trainer is put on the queue with a token not issued by gym-crm
    Then the message is moved to the dead letter queue as not issued by gym-crm
    And the workload service has no summary for the trainer

  Scenario: A gym-crm user token is not accepted by the workload service
    When the workload service is called with the trainee's gym-crm token
    Then the workload service responds with status 401
