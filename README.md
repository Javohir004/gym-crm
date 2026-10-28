# Gym CRM

A Gym CRM built in three stages: Spring Core (in-memory storage), Hibernate
(real database), and a REST API on top. No Spring Boot anywhere — Spring
Context/MVC and an embedded Tomcat are wired by hand.

## Tech stack

- Java 21
- Spring Context / ORM / WebMVC 6.1.13
- Hibernate ORM 6.5.3 (through JPA `EntityManager`)
- H2 (in-memory) + HikariCP
- Embedded Tomcat 10.1 (started manually, no Spring Boot)
- Jackson (+ JSR-310 for `LocalDate`), Swagger annotations
- Lombok, SLF4J + Logback
- JUnit 5 + Mockito, JaCoCo

## Project structure

```
model/       JPA entities (User, Trainee, Trainer, Training, TrainingType)
dao/         EntityManager-based DAOs
service/     Business logic, authentication, validation, @Transactional
controller/  @RestController classes — the 17 REST endpoints
dto/
  requests/  Request bodies
  responses/ Response bodies
exception/   Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
logging/     TransactionIdFilter, RestCallLoggingFilter
util/        Validation
security/    Spring Security config, JWT, login-attempt protection, logout
config/      AppConfig (DB/Hibernate/transactions), WebConfig (MVC)
Main.java            demo for the Hibernate-module functions (console)
RestApplication.java starts the embedded Tomcat + REST API
postman/     Postman collection covering all 17 endpoints
```

## How to run

```
mvn clean test              # all tests + coverage report (target/site/jacoco/index.html)
mvn compile exec:java       # starts the REST API (http://localhost:8081)
```

`Main.java` can also be run directly from the IDE for the Hibernate-module demo.

## Database schema

| Table | Notes |
|---|---|
| `users` | first/last name, unique username, password, is_active |
| `trainee` | date of birth, address, `user_id` (FK, one-to-one with `users`) |
| `trainer` | `specialization_id` (FK to `training_type`), `user_id` (FK, one-to-one) |
| `training` | FKs to trainee, trainer and training type; name, date, duration |
| `training_type` | constant list (Fitness, Yoga, Zumba, Stretching, Resistance) |
| `trainee_trainer` | join table for the many-to-many Trainee ↔ Trainer relation |

Tables are (re)created from the entities on every startup (`hibernate.hbm2ddl.auto=create`).
`import.sql` fills `training_type`; nothing in the app can change that list.

---

## Module 2 — Hibernate (console demo, 18 functions)

`Main.java` runs all 18 functions end to end and logs the result of each one.
Username/password generation, authentication, validation, cascade delete and
`@Transactional` all work the same way the REST module (below) uses them.

- **Username / password:** `firstName.lastName`, a serial number added if taken
  (`John.Doe1`). Password: random 10 characters (`SecureRandom`).
- **Delete Trainee:** hard delete; the user row and all its trainings are
  deleted too (cascade).
- Here, Activate/De-activate simply **flips** the current state (`toggleActive`).

---

## Module 3 — REST API (17 endpoints)

Base URL: `http://localhost:8081/api`. Full request/response examples for
every endpoint are in `postman/GymCRM.postman_collection.json` — import it
into Postman and run the requests top to bottom (registration requests
auto-save the generated username/password for the rest of the collection).

### Authentication

Every endpoint except registration (1, 2) and login (3) requires a **JWT**:
`Authorization: Bearer <token>`.

* **Registration** returns `username`, the generated `password` (shown once, only its
  BCrypt hash is stored) and a `token`.
* **Login** `POST /api/login` with `{"username": "...", "password": "..."}` returns `{"token": "..."}`.
* **Brute-force protection**: after 3 wrong passwords the username is blocked for
  5 minutes (HTTP 429, even for the correct password).
* **Logout** `POST /api/logout` with the Bearer token puts the token on a blacklist.
* **Own profile only**: a user can only read or change his or her own data (HTTP 403 otherwise).
* **CORS**: allowed origins are set in `gym.security.cors.allowed-origins`.
* Settings (`application.properties`): `gym.security.jwt.secret` (override with the
  `GYM_JWT_SECRET` environment variable, at least 32 characters),
  `gym.security.jwt.expiration-minutes`, `gym.security.login.max-attempts`,
  `gym.security.login.block-minutes`.

### Endpoints

| # | Function | Method + path |
|---|---|---|
| 1 | Trainer Registration | `POST /trainers` |
| 2 | Trainee Registration | `POST /trainees` |
| 3 | Login | `POST /login` (JSON body) |
| 4 | Change Login | `PUT /login` |
| 18 | Logout | `POST /logout` |
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

- **Add Training (14) has no Training Type field.** The task's own request
  fields for this endpoint omit it, so the training's type is taken from the
  **trainer's own specialization** instead.
- **Update Trainer Profile (9): specialization is read-only.** The request
  may carry a value for it, but it is always ignored.
- **Activate/De-activate (15, 16) is not idempotent (task note 9), on
  purpose.** Unlike the Hibernate-module demo's `toggleActive`, the REST
  endpoints take an explicit `isActive` value — but calling it with the
  state the resource is **already** in returns **409 Conflict** rather than
  silently succeeding.
- **Get Training types (17) needs no authentication.** Its own request
  description says "no data"; it is a small, public, constant list.
- **Error handling:** every exception maps to the same JSON shape
  (`message`, `status`, `transactionId`) via one `@RestControllerAdvice` —
  400 (validation), 401 (auth), 404 (not found), 409 (conflict), 500 (anything else, logged
  server-side only, never leaked to the client).
- **Logging (task note 17):** `TransactionIdFilter` gives every request a
  UUID, carried through every log line for that request (and returned as
  the `X-Transaction-Id` response header, so it could be forwarded to a
  downstream service). `RestCallLoggingFilter` logs which endpoint was
  called and the final status code.
- **Swagger:** every controller method carries `@Operation`/`@ApiResponse`
  annotations (task note 19). No Swagger UI is served — a UI server is
  tightly coupled to Spring Boot's auto-configuration, so it was left out
  rather than fought with; a Postman collection covers manual testing instead.
- **No Spring Boot:** an embedded Tomcat is started by hand in
  `RestApplication.java`, with a `DispatcherServlet` wired to a child
  `WebApplicationContext` (`WebConfig`) whose parent is the business-layer
  context (`AppConfig`).

## Tests

All tests pass; line coverage is **96.6%** (JaCoCo excludes `Main` and
`RestApplication` — they start a real server/console demo and are not
realistic to unit test, the same way a `main()` method usually is).

- **Service layer:** Mockito-based unit tests.
- **DAO layer:** `DaoIntegrationTest` runs every DAO against a real H2
  database through the actual Spring context; each test runs in its own
  transaction, rolled back automatically afterwards.
- **Controllers:** `MockMvc` (standalone setup, `GlobalExceptionHandler`
  registered) — success paths, missing/invalid auth, validation errors,
  not-found and conflict cases.
- **Logging filters:** direct unit tests (MDC content, response header,
  log output).
