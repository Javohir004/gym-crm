# Gym CRM

A Gym CRM built in stages: Spring Core, Hibernate (real database), a REST API,
and finally **Spring Boot** with Actuator, custom health indicators, custom
Prometheus metrics, Swagger UI and environment profiles
(`local`, `dev`, `stg`, `prod`).

## Tech stack

- Java 21
- Spring Boot 3.3.4 (Spring Framework 6.1.13, Hibernate ORM 6.5.3 via JPA `EntityManager`)
- Spring Web MVC (embedded Tomcat), Spring Data JPA starter (Hibernate + HikariCP + transactions)
- Spring Boot Actuator + Micrometer Prometheus registry
- Spring AOP (metrics aspect)
- H2 database (in-memory or file, depending on the profile)
- springdoc-openapi (Swagger UI)
- Jackson (+ JSR-310 for `LocalDate`), Lombok, SLF4J + Logback
- JUnit 5 + Mockito + Spring Boot Test, JaCoCo

## Project structure

```
src/main/java/com/epam/training/gym/
  GymCrmApplication.java   @SpringBootApplication entry point
  model/       JPA entities (User, Trainee, Trainer, Training, TrainingType)
  dao/         EntityManager-based DAOs
  service/     Business logic, authentication, validation, @Transactional
  facade/      GymFacade - one method per required function
  controller/  @RestController classes - the 17 REST endpoints
  dto/
    requests/  Request bodies
    responses/ Response bodies
  exception/   Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
  logging/     TransactionIdFilter, RestCallLoggingFilter
  health/      Custom Actuator health indicators
  metrics/     GymMetrics (Micrometer meters) + GymMetricsAspect
  config/      OpenApiConfig (Swagger / Basic Auth scheme)
  util/        Validation, BasicAuthUtil

src/main/resources/
  application.properties            common settings (default profile: local)
  application-local.properties
  application-dev.properties
  application-stg.properties
  application-prod.properties
  data.sql                          training types (idempotent MERGE)
  logback.xml                       log pattern with the transaction id

postman/       Postman collections
```

## How to run

```
mvn clean test                                          # all tests + coverage report (target/site/jacoco/index.html)
mvn spring-boot:run                                     # profile "local" (default), http://localhost:8081
mvn spring-boot:run -Dspring-boot.run.profiles=dev      # another profile
```

From the IDE: run `GymCrmApplication`; set **Active profiles** in the run
configuration to pick a profile.

From the packaged jar:

```
mvn clean package
java -jar target/gym-crm-app-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

The profile can also be set with the `SPRING_PROFILES_ACTIVE` environment
variable. The startup log confirms the choice:
`The following 1 profile is active: "dev"`.

## Environments (Spring profiles)

The default profile is `local` (`spring.profiles.active=local` in
`application.properties`). Each profile has its own database properties.

| | `local` | `dev` | `stg` | `prod` |
|---|---|---|---|---|
| Database URL | `jdbc:h2:mem:gymdb-local` | `jdbc:h2:file:./data/gymdb-dev` | `jdbc:h2:file:./data/gymdb-stg` | `jdbc:h2:file:./data/gymdb-prod` |
| Credentials | `sa` / empty | `sa` / empty | `DB_USERNAME` / `DB_PASSWORD` (defaults for local testing) | `DB_USERNAME` / `DB_PASSWORD` **required** |
| Hikari max pool size | 5 | 5 | 10 | 20 |
| `hibernate.ddl-auto` | `create` (clean DB on every start) | `update` | `update` | `update` |
| SQL in the log | yes (formatted) | no | no | no |
| App log level (`com.epam.training.gym`) | DEBUG | DEBUG | INFO | INFO |
| Actuator endpoints | health, info, metrics, prometheus | same | same | health, prometheus only |
| Health details | shown | shown | shown | hidden |
| Swagger UI | on | on | on | **off** |

Notes:

- In `prod` the credentials have no defaults on purpose: the application
  refuses to start if `DB_USERNAME` / `DB_PASSWORD` are missing.
- The H2 files are created in the `data/` folder (git-ignored). With the
  `update` mode a fresh file database gets its tables on the first start; to
  reset a `dev` / `stg` / `prod` database, stop the app and delete its file
  in `data/`.
- H2 is used in every profile. For a real deployment only the `spring.datasource.*`
  properties would change.

## Database schema

| Table | Notes |
|---|---|
| `users` | first/last name, unique username, password, is_active |
| `trainee` | date of birth, address, `user_id` (FK, one-to-one with `users`) |
| `trainer` | `specialization_id` (FK to `training_type`), `user_id` (FK, one-to-one) |
| `training` | FKs to trainee, trainer and training type; name, date, duration |
| `training_type` | constant list (Fitness, Yoga, Zumba, Stretching, Resistance) |
| `trainee_trainer` | join table for the many-to-many Trainee <-> Trainer relation |

`data.sql` fills `training_type` on every start. It uses `MERGE ... KEY`, so
running it again never creates duplicates (this works with every
`ddl-auto` mode). Nothing in the application can change that list.

---

## Actuator, health indicators and metrics

Base URL: `http://localhost:8081/actuator`

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | health status with all indicators |
| `/actuator/metrics` | Micrometer metrics (JSON) |
| `/actuator/prometheus` | metrics in Prometheus format |

### Custom health indicators

| Indicator (name in `/health`) | Behaviour |
|---|---|
| `trainingTypes` | `UP` with the number of training types; `DOWN` if the table is empty (e.g. `data.sql` did not run) or cannot be read |
| `trainerAvailability` | `UP` with total / active / inactive trainer counts; `UNKNOWN` (a warning, the overall status stays `UP`) when trainers exist but all are inactive |
| `databaseResponseTime` | `UP` with the connection check time; `OUT_OF_SERVICE` if slower than `gym.health.db-slow-threshold-ms` (default 500); `DOWN` if no valid connection |

Spring Boot's own `db`, `diskSpace` and `ping` indicators run alongside them.

### Custom Prometheus metrics

| Prometheus name | Type | Meaning |
|---|---|---|
| `gym_registrations_total{role="trainee"}` | counter | registered trainees |
| `gym_registrations_total{role="trainer"}` | counter | registered trainers |
| `gym_trainings_added_total` | counter | trainings added |
| `gym_authentication_failures_total` | counter | failed credential checks in `AuthenticationService` |
| `gym_trainers_active` | gauge | current number of active trainers (read from the DB on each scrape) |

The counters are updated by `GymMetricsAspect` (Spring AOP), so the service
code is untouched. The aspect runs outside the transaction (`@Order(0)`), so a
registration is counted only after it was committed successfully. All
metrics also carry the `application="gym-crm"` tag.

Example Prometheus scrape config:

```yaml
scrape_configs:
  - job_name: gym-crm
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['localhost:8081']
```

---

## REST API (17 endpoints)

Base URL: `http://localhost:8081/api`

### Swagger UI

- UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Click **Authorize** and enter a username and password to call the protected
endpoints (HTTP Basic). Swagger is disabled in the `prod` profile.

### Postman

Import `postman/GymCRM.postman_collection.json` for the 17 endpoints, or
`postman/GymCRM_TestScenario.postman_collection.json` for a ready end-to-end
scenario (registration requests save the generated username/password into
collection variables; the collection also checks the health indicators and
the custom metrics). Run it with the Collection Runner.

### Authentication

Every endpoint except registration (1, 2), login (3, 4) and "Get Training
types" (17) requires **HTTP Basic Auth**
(`Authorization: Basic base64(username:password)`). The username in the
path must be the authenticated user, otherwise the call is rejected with 401.
Actuator and Swagger endpoints are not behind this check.

### Endpoints

| # | Function | Method + path |
|---|---|---|
| 1 | Trainee Registration | `POST /trainees` |
| 2 | Trainer Registration | `POST /trainers` |
| 3 | Login | `GET /login?username=&password=` |
| 4 | Change Login | `PUT /login` |
| 5 | Get Trainee Profile | `GET /trainees/{username}` |
| 6 | Update Trainee Profile | `PUT /trainees/{username}` |
| 7 | Delete Trainee Profile | `DELETE /trainees/{username}` |
| 8 | Get Trainer Profile | `GET /trainers/{username}` |
| 9 | Update Trainer Profile | `PUT /trainers/{username}` |
| 10 | Not-assigned active trainers | `GET /trainees/{username}/unassigned-trainers` |
| 11 | Update Trainee's Trainer List | `PUT /trainees/{username}/trainers` |
| 12 | Get Trainee Trainings List | `GET /trainees/{username}/trainings` |
| 13 | Get Trainer Trainings List | `GET /trainers/{username}/trainings` |
| 14 | Add Training | `POST /trainings` |
| 15 | Activate/De-activate Trainee | `PATCH /trainees/{username}/status` |
| 16 | Activate/De-activate Trainer | `PATCH /trainers/{username}/status` |
| 17 | Get Training types | `GET /training-types` |

### Design decisions worth knowing

- **Username / password:** `firstName.lastName`, a serial number added if the
  name is taken (`John.Doe1`). Password: random 10 characters (`SecureRandom`).
- **Add Training (14) has no Training Type field.** The training's type is
  taken from the **trainer's own specialization**.
- **Update Trainer Profile (9): specialization is read-only.** The request may
  carry a value for it, but it is always ignored.
- **Activate/De-activate (15, 16) is not idempotent, on purpose.** The
  endpoints take an explicit `isActive` value, but calling them with the
  state the resource is **already** in returns **409 Conflict**.
- **Delete Trainee (7):** hard delete; the user row and all of the trainee's
  trainings are deleted too (cascade).
- **Get Training types (17) needs no authentication.** It is a small, public,
  constant list.
- **Error handling:** every exception maps to the same JSON shape
  (`message`, `status`, `transactionId`) via one `@RestControllerAdvice`:
  400 (validation), 401 (auth), 404 (not found, including unknown URLs),
  405 (wrong HTTP method), 409 (conflict), 500 (anything else, logged
  server-side only, never leaked to the client).
- **Logging:** `TransactionIdFilter` gives every request a UUID, carried
  through every log line of that request (the `[txId=...]` part of the log
  pattern) and returned as the `X-Transaction-Id` response header.
  `RestCallLoggingFilter` logs which endpoint was called and the final
  status code. Both are `@Component` filters registered by Spring Boot.

## Tests

Run `mvn clean test`. The JaCoCo report is in `target/site/jacoco/index.html`
(`GymCrmApplication` is excluded - it only starts the application).

Instruction coverage: **97%** (3,506 of 3,612 instructions covered).

- **Service layer:** Mockito-based unit tests.
- **DAO layer:** `DaoIntegrationTest` runs every DAO against a real H2
  database through the Spring Boot context (`@SpringBootTest`, profile
  `local`); each test runs in its own transaction, rolled back afterwards.
- **Controllers:** `MockMvc` (standalone setup, `GlobalExceptionHandler`
  registered) - success paths, missing/invalid auth, validation errors,
  not-found and conflict cases; `GlobalExceptionHandlerHttpErrorsTest` covers
  the 404 / 405 mapping.
- **Logging filters:** direct unit tests (MDC content, response header,
  log output).
- **Health indicators:** unit tests for all three custom indicators (UP /
  DOWN / warning states).
- **Metrics:** `GymMetricsTest` and `GymMetricsAspectTest` (unit);
  `MetricsIntegrationTest` sends real HTTP requests through the Spring
  proxies and checks that the counters move.
- **Actuator and Swagger:** `ActuatorIntegrationTest` checks `/actuator/health`
  and `/actuator/prometheus`; `OpenApiConfigTest` and `OpenApiIntegrationTest`
  check the Swagger configuration and `/v3/api-docs`.
