Feature: Consuming trainer workload messages
  As the trainer workload service
  I want to apply every valid workload message from the queue to the trainer summary in MongoDB
  So that invalid messages never corrupt the stored workload

  Background:
    Given the workload store is empty

  Scenario: An ADD message creates the trainer summary in MongoDB
    When a "ADD" message for trainer "Anna.Lee" on "2026-05-02" for 60 minutes is received with a valid token
    Then the trainer "Anna.Lee" has 60 minutes in 2026 month 5
    And the summary of "Anna.Lee" is stored in MongoDB as "Anna" "Lee" with status active

  Scenario: ADD messages in the same month are summed up
    When a "ADD" message for trainer "Anna.Lee" on "2026-05-02" for 60 minutes is received with a valid token
    And a "ADD" message for trainer "Anna.Lee" on "2026-05-20" for 30 minutes is received with a valid token
    Then the trainer "Anna.Lee" has 90 minutes in 2026 month 5

  Scenario: A DELETE message reduces the monthly workload
    Given trainer "Anna.Lee" "Anna" "Lee" has a training on "2026-05-02" for 60 minutes
    And trainer "Anna.Lee" "Anna" "Lee" has a training on "2026-05-20" for 30 minutes
    When a "DELETE" message for trainer "Anna.Lee" on "2026-05-20" for 30 minutes is received with a valid token
    Then the trainer "Anna.Lee" has 60 minutes in 2026 month 5

  Scenario: A message without a service token goes to the dead letter queue
    When a "ADD" message for trainer "NoToken.Trainer" on "2026-05-02" for 60 minutes is received without a token
    Then the message is moved to the dead letter queue with a reason starting with "Missing or invalid service token"
    And no summary is stored for trainer "NoToken.Trainer"

  Scenario: A message with an expired service token goes to the dead letter queue
    When a "ADD" message for trainer "Expired.Trainer" on "2026-05-02" for 60 minutes is received with an expired token
    Then the message is moved to the dead letter queue with a reason starting with "Missing or invalid service token"
    And no summary is stored for trainer "Expired.Trainer"

  Scenario Outline: An invalid message goes to the dead letter queue with the reason
    When a message with a valid token and this body is received:
      """
      <body>
      """
    Then the message is moved to the dead letter queue with a reason starting with "<reason>"
    And the dead letter keeps the original queue and transaction id
    And no summary is stored for trainer "Dlq.Trainer"

    Examples:
      | body                                                                                                                                                                  | reason                                                            |
      | {"trainerUsername":"Dlq.Trainer","trainingDuration":60}                                                                                                               | Required information missing or invalid: actionType               |
      | {"trainerUsername":"Dlq.Trainer","trainerFirstName":"A","trainerLastName":"B","isActive":true,"trainingDate":"2026-05-02","trainingDuration":-5,"actionType":"ADD"}   | Required information missing or invalid: trainingDuration         |
      | {"trainerUsername":"Dlq.Trainer","trainerFirstName":"A","trainerLastName":"B","isActive":true,"trainingDate":"2026-05-02","trainingDuration":30,"actionType":"MOVE"}  | Malformed JSON                                                    |
      | not a json                                                                                                                                                            | Malformed JSON                                                    |
