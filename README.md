# Gym CRM — Hibernate Module

Second task of the Gym CRM project. It is built on top of the Spring Core
module: the in-memory storage (Maps + CSV files) was replaced with a real
database accessed through **Hibernate**.

## Tech stack

- Java 21
- Spring Context / Spring ORM 6.1.13 (Spring Core, no Spring Boot)
- Hibernate ORM 6.5.3 (bootstrapped through JPA: `EntityManager`)
- H2 2.2.224 (in-memory database) + HikariCP 5.1.0 (connection pool)
- Lombok, SLF4J + Logback
- JUnit 5 + Mockito, JaCoCo (coverage)

## What was done

- Database schema from the task mapped to JPA entities
- DAO layer rewritten with `EntityManager` and JPQL
- All 18 required functions implemented in the service layer
- Authentication (username + password) before every function except "create profile"
- Required-field validation before create/update
- Transaction management with `@Transactional`
- Logging (passwords are never logged) and unit + integration tests

## Database schema

| Table | Notes |
|---|---|
| `users` | first/last name, unique username, password, is_active |
| `trainee` | date of birth, address, `user_id` (FK, one-to-one with `users`) |
| `trainer` | `specialization_id` (FK to `training_type`), `user_id` (FK, one-to-one) |
| `training` | FKs to trainee, trainer and training type; name, date, duration |
| `training_type` | constant list (Fitness, Yoga, Zumba, Stretching, Resistance) |
| `trainee_trainer` | join table for the many-to-many Trainee ↔ Trainer relation |

Tables are created from the entities on startup (`hibernate.hbm2ddl.auto=create`).
`import.sql` fills `training_type`; the application can only read it.

## Project structure

```
model/      JPA entities (User, Trainee, Trainer, Training, TrainingType)
dao/        UserDao, TraineeDao, TrainerDao, TrainingDao, TrainingTypeDao
service/    TraineeService, TrainerService, TrainingService,
            AuthenticationService, UserAccountService,
            UsernameGenerator, PasswordGenerator
facade/     GymFacade — one method per required function
exception/  AuthenticationException, ValidationException, NotFoundException
dto/        TrainingRequest
util/       Validation
config/     AppConfig (DataSource, EntityManagerFactory, TransactionManager)
Main.java   demo that runs all 18 functions
```

Flow: `Main → GymFacade → Service (@Transactional) → DAO (EntityManager) → H2`

## The 18 functions

| # | Function | Method |
|---|---|---|
| 1, 2 | Create Trainer / Trainee | `createTrainer`, `createTrainee` |
| 3, 4 | Username + password matching | `traineeCredentialsMatch`, `trainerCredentialsMatch` |
| 5, 6 | Select profile by username | `getTrainer`, `getTrainee` |
| 7, 8 | Change password | `changeTraineePassword`, `changeTrainerPassword` |
| 9, 10 | Update profile | `updateTrainer`, `updateTrainee` |
| 11, 12 | Activate / De-activate | `toggleTraineeActive`, `toggleTrainerActive` |
| 13 | Delete Trainee by username | `deleteTrainee` |
| 14, 15 | Trainings list with criteria | `getTraineeTrainings`, `getTrainerTrainings` |
| 16 | Add training | `addTraining` |
| 17 | Trainers not assigned to a trainee | `getUnassignedTrainers` |
| 18 | Update trainee's trainers list | `updateTraineeTrainers` |

## Main rules

- **Username / password:** `firstName.lastName`; a serial number is added if taken
  (`John.Doe1`). Password is a random 10-character string (`SecureRandom`).
- **Delete Trainee:** hard delete; the user row and all trainings are deleted too (cascade).
- **Activate / De-activate:** not idempotent — every call switches the current state.
- **Filters** in functions 14 and 15 are all optional; a missing filter is simply skipped.

## How to run

```
mvn clean test     # 175 tests + coverage report (target/site/jacoco/index.html)
```

or run `Main.java` from the IDE. Logs go to the console and to `logs/gym-crm.log`.

## Tests

175 tests, all passing. Line coverage is **81.5%** overall (every class is at 100%
except `Main`, the demo entry point). Service tests use Mockito; DAO and
end-to-end tests run against a real in-memory H2 database.

## Design notes

- **Why Training and Training Type are separate tables (one-to-many).**
  Training types are a small fixed list. Keeping them in their own table means the
  name is stored once instead of being repeated (and possibly misspelled) in every
  training row, a type can be renamed in one place, and Trainer specialization
  reuses the same list. One type has many trainings, hence one-to-many.
- **`EntityManager` instead of `SessionFactory`:** Spring 6 recommends bootstrapping
  Hibernate through JPA; Hibernate is still the implementation underneath.
- **Own profile only:** every function works on the authenticated user's own profile.
- **Trainer / trainee name filter:** case-insensitive, matches part of "first last".
- **Passwords are stored as plain text** because the task does not ask for hashing;
  a real system would hash them (e.g. BCrypt).
