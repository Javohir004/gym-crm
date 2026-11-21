Feature: Authentication in gym-crm
  As a gym user
  I want to sign in with my credentials and use a JWT token
  So that only I can reach my profile

  Background:
    Given a trainee "Anna" "Smith" is registered

  Scenario: Login with the correct password returns a token
    When the trainee logs in with the correct password
    Then the response status is 200
    And the response contains a token

  Scenario: The token opens the trainee profile
    When the trainee requests their profile with their token
    Then the response status is 200
    And the response field "firstName" is "Anna"

  Scenario: Login with a wrong password is rejected
    When the trainee logs in with the password "wrongPassword1"
    Then the response status is 401
    And the response field "message" is "Invalid username or password"

  Scenario: Login of an unknown user is rejected
    When "Nobody.Here" logs in with the password "whatever12345"
    Then the response status is 401

  Scenario: A protected endpoint without a token is rejected
    When the trainee requests their profile without a token
    Then the response status is 401

  Scenario: A tampered token is rejected
    When the trainee requests their profile with a tampered token
    Then the response status is 401

  Scenario: Three wrong passwords block the user
    When the trainee logs in with the password "wrongPassword1" 3 times
    And the trainee logs in with the correct password
    Then the response status is 429
