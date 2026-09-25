# Gym CRM — Spring Core Module
 
A Spring Core module (no Spring Boot) managing Trainee/Trainer/Training
profiles, backed by in-memory storage seeded from CSV files on startup.
 
## Tech stack
 
- Java 21
- Spring Context 6.1.13 (Spring Core, not Spring Boot)
- Lombok — getters/setters/builders
- SLF4J + Logback — logging
- JUnit 5 + Mockito — unit tests
- JaCoCo — coverage report
## Project structure
 
```
model/     — Trainee, Trainer, Training, TrainingType, User
storage/   — one separate Map-based bean per entity
dao/       — Trainee (CRUD), Trainer (CUR), Training (CR)
service/   — business logic + username/password generation
facade/    — single entry point wrapping all services
Main.java  — demo running every required operation
```
 
## How it works
 
```
Main -> Facade -> Service -> DAO -> Storage (in-memory Map)
```
 
- Storage lives only in RAM (`ConcurrentHashMap`); data is lost on restart,
  except for what the CSV files re-seed on the next run.
- `StorageInitializer` (a `BeanPostProcessor`) fills each storage bean from
  its CSV file right after Spring creates it.
- File paths come from `application.properties` via `@Value("${...}")`
  placeholders — nothing is hardcoded.
## Dependency injection style (per task requirement)
 
- Storage → DAO: setter-based (`@Autowired` setter)
- DAO / generators → Service: setter-based (`@Autowired` setter)
- Service → Facade: constructor-based
## Username / password rules
 
- Username = `firstName.lastName` (e.g. `John.Doe`)
- If taken, a serial number is appended (`John.Doe1`, `John.Doe2`, ...)
- Password = random 10-character string (`SecureRandom`), never logged
## How to run
 
```
mvn clean test
```
or run `Main.java` directly from the IDE.
 
## Test results
 
53 tests, **85.9% line coverage** (requirement: 80%+).

