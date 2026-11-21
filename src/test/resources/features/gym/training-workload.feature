Feature: Publishing trainer workload from gym-crm
  As gym-crm
  I want to send a workload message for every added or cancelled training
  So that the trainer workload service can keep the trainer's monthly hours

  Background:
    Given a trainee "Tom" "Walker" is registered
    And a trainer "Kate" "Moss" with specialization "Yoga" is registered

  Scenario: Adding a training publishes an ADD message
    When the trainee adds a training "Morning yoga" 10 days from now for 60 minutes
    Then the response status is 200
    And a "ADD" workload message for the trainer is published with duration 60
    And the workload message carries a service token and the trainer as group id

  Scenario: Deleting the trainee publishes a DELETE message for an upcoming training
    Given the trainee has a training "Evening yoga" 5 days from now for 45 minutes
    When the trainee deletes their profile
    Then the response status is 200
    And a "DELETE" workload message for the trainer is published with duration 45

  Scenario: Deleting the trainee does not cancel a past training
    Given the trainee has a training "Old yoga" 5 days ago for 30 minutes
    When the trainee deletes their profile
    Then the response status is 200
    And no workload message is published

  Scenario: Adding a training for an unknown trainer is rejected
    When the trainee adds a training "Ghost" with trainer "No.Trainer" 3 days from now for 30 minutes
    Then the response status is 404
    And no workload message is published

  Scenario: Adding a training without a token is rejected
    When a training is added without a token
    Then the response status is 401
    And no workload message is published

  Scenario: Adding a training without a name is rejected
    When the trainee adds a training without a name
    Then the response status is 400
    And no workload message is published
