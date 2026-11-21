Feature: Reading trainer workload over REST
  As gym-crm holding a service token
  I want to read a trainer's workload summary
  So that I can see how many minutes the trainer worked each month

  Background:
    Given the workload store is empty
    And trainer "Anna.Lee" "Anna" "Lee" has a training on "2026-03-01" for 60 minutes
    And trainer "Anna.Lee" "Anna" "Lee" has a training on "2026-03-15" for 45 minutes

  Scenario: The trainer summary is returned for a valid token
    When the summary of "Anna.Lee" is requested with a valid token
    Then the response status is 200
    And the response field "trainerUsername" is "Anna.Lee"
    And the response field "trainerStatus" is "true"
    And the response field "years[0].year" is "2026"
    And the response field "years[0].months[0].trainingSummaryDuration" is "105"

  Scenario: The monthly workload is returned for a valid token
    When the workload of "Anna.Lee" for 2026 month 3 is requested with a valid token
    Then the response status is 200
    And the response field "month" is "MARCH"
    And the response field "trainingSummaryDuration" is "105"

  Scenario: Trainers are found by first and last name
    When trainers named "Anna" "Lee" are searched with a valid token
    Then the response status is 200
    And the result contains trainer "Anna.Lee"

  Scenario: Searching for an unknown name returns an empty list
    When trainers named "No" "Body" are searched with a valid token
    Then the response status is 200
    And the result is empty

  Scenario: An unknown trainer is not found
    When the summary of "No.Such.Trainer" is requested with a valid token
    Then the response status is 404

  Scenario: An invalid month is rejected
    When the workload of "Anna.Lee" for 2026 month 13 is requested with a valid token
    Then the response status is 400

  Scenario: A request without a token is rejected
    When the summary of "Anna.Lee" is requested without a token
    Then the response status is 401
    And the response field "status" is "401"

  Scenario: A request with an expired token is rejected
    When the summary of "Anna.Lee" is requested with an expired token
    Then the response status is 401

  Scenario: A request with a token signed by another secret is rejected
    When the summary of "Anna.Lee" is requested with a token signed by another secret
    Then the response status is 401
