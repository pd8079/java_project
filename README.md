# CyberOps Arena

CyberOps Arena is an educational Java backend for deterministic, isolated cyber attack and defense exercises. It models changes to database records only: it does not scan hosts, execute uploaded code, modify real accounts, or configure real firewalls.

The repository currently contains the Spring Boot backend under [`Backend/`](Backend/). It exposes a REST API for accounts, network templates, scenarios, simulations, detection rules, alerts, and modeled defenses. There is no Swing desktop application or other frontend in this repository yet; the GUI requirement remains separate work for a client application.

## Backend features

- Spring Boot 3.5.16 REST API, Java 21, Maven, MySQL 8.4, and Flyway schema migrations.
- Stateless bearer-token authentication with signed JWTs, BCrypt password hashes, account enable/disable, and role-based access rules.
- User CRUD implemented explicitly with `Connection`, `PreparedStatement`, and `ResultSet`; input values are bound parameters.
- Editable network templates and scenarios, per-run snapshots, lifecycle operations, event history, alert rules, modeled defenses, audit records, and overview/run metrics.
- Deterministic attack steps dispatched through the `AttackStrategy` interface. `NetworkGraph` checks directed reachability with breadth-first search.
- Optional background run execution on a named Java `Thread` executing a `Runnable`; clients can poll run status and events.
- Docker Compose development stack, OpenAPI docs, and MySQL Testcontainers integration tests.

This is an educational project, not production intrusion detection. Detection rules and generated alerts represent simulated evidence only. Defenses change only run-local simulation records.

## Java project requirements represented in the backend

The current code demonstrates organized Java packages, classes and constructors, encapsulated services, user-defined interfaces and implementations, strategy polymorphism, method overriding, lambdas and streams, validation and exception handling, explicit JDBC CRUD, and meaningful background work using `Thread` and `Runnable`. For example, `AttackStrategy` has multiple implementations, while `BackgroundSimulationRunner` executes each requested simulation on a worker thread.

The backend does **not** implement professor requirement 9's Java Swing GUI, Swing event handling, or desktop MVC presentation layer. This repository is an API server; a Swing client would need to be added separately and call the REST endpoints. The backend already separates HTTP controllers, business services, persistence repositories, and domain/simulation code.

## Prerequisites

- JDK 21
- Maven 3.6.3 or newer
- Docker with Compose, or a local MySQL 8.4 server for running the application
- A working Docker-compatible container runtime for the MySQL integration test (Docker or rootless Podman)

The project targets Java 21. Spring Boot 3.5.16 supports Java 17 through 25. The Maven test configuration detects Bazzite's rootless Podman socket at `${XDG_RUNTIME_DIR}/podman/podman.sock` and configures Testcontainers automatically when it exists and `DOCKER_HOST` is not already set. On Bazzite, enable the user Podman socket if needed with `systemctl --user enable --now podman.socket`, then run the commands below in the same user session. No machine-specific socket settings are committed as project-wide environment requirements.

## Start the backend with Compose

Run these commands from the backend directory:

```sh
cd Backend
cp .env.example .env
```

Edit `.env` and replace the database password, MySQL root password, and JWT secret with unique values. `JWT_SECRET` must contain at least 32 characters. The example includes optional development bootstrap-admin settings; replace its password with a unique value of at least 14 characters or leave all three `BOOTSTRAP_ADMIN_*` values blank to disable bootstrap creation.

```sh
docker compose up --build -d
```

The backend listens on `http://localhost:8080`. Check health at [`http://localhost:8080/actuator/health`](http://localhost:8080/actuator/health), readiness at [`http://localhost:8080/actuator/health/readiness`](http://localhost:8080/actuator/health/readiness), and interactive API documentation at [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html).

Compose starts MySQL 8.4 and waits for its health check before starting the backend. Flyway applies migrations at startup. Database data persists in the `mysql_data` volume. To stop the services while keeping data, run `docker compose down`. To also erase local database data, run `docker compose down -v`.

## Run tests

From `Backend/`, run the full unit and integration suite:

```sh
mvn test
```

The MySQL lifecycle integration test uses Testcontainers with a real disposable MySQL 8.4 container; it runs Flyway migrations and starts the Spring application on a random HTTP port. It checks health, prepared-statement user CRUD, background simulation completion, and run snapshot/event/alert/defense behavior. `@Testcontainers(disabledWithoutDocker = true)` means the integration test is skipped only when Testcontainers cannot find a usable Docker-compatible runtime. With Docker or the configured Podman socket available, `mvn test` runs it as part of the ordinary suite.

The Bazzite Podman Maven profile is activated automatically when the rootless Podman socket exists and `DOCKER_HOST` is unset. To run only the MySQL integration test:

```sh
mvn -Dtest=MySqlLifecycleTest test
```

The unit tests cover directed graph reachability and attack strategy registration. When the full suite was last run for this project, Maven reported 9 tests, 0 failures, 0 errors, and 0 skipped; rerun `mvn test` to see the result for your current machine and checkout.

## Run locally with an existing MySQL server

Create the database and an application user. Flyway runs at application startup, so the account needs migration permissions for initial setup:

```sql
CREATE DATABASE cyberops_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'cyberops'@'localhost' IDENTIFIED BY 'replace-with-a-long-random-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON cyberops_arena.* TO 'cyberops'@'localhost';
```

From `Backend/`, set the environment variables below (or load the values from a local `.env` file) and start the app:

```sh
export DB_HOST=localhost DB_PORT=3306 DB_NAME=cyberops_arena
export DB_USERNAME=cyberops DB_PASSWORD='your-database-password'
export JWT_SECRET='replace-with-at-least-32-random-characters'
mvn spring-boot:run
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `cyberops_arena` | Database name |
| `DB_USERNAME` | `cyberops` | Application database user |
| `DB_PASSWORD` | `cyberops-local` in local Spring config | Application database password; set explicitly outside local development |
| `JWT_SECRET` | empty | HMAC signing secret; required and at least 32 characters |
| `JWT_EXPIRATION` | `1800` | Access-token lifetime in seconds |
| `SERVER_PORT` | `8080` | HTTP port |
| `CORS_ORIGINS` | `http://localhost:3000` | Comma-separated allowed browser origins |
| `BOOTSTRAP_ADMIN_USERNAME` | empty | Optional one-time administrator username |
| `BOOTSTRAP_ADMIN_EMAIL` | empty | Optional administrator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | empty | Optional administrator password, at least 14 characters |

Bootstrap administrator values must be supplied together. Bootstrap only inserts the account if the username does not already exist. Database timestamps use UTC. Production deployments should use a secret manager, a least-privilege database identity, and HTTPS at a trusted proxy or load balancer.

## API overview

Except for login, health endpoints, and API documentation, API requests require `Authorization: Bearer <token>`. The current routes are:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Authenticate and receive a token |
| `GET` | `/api/v1/auth/me` | Get the authenticated account |
| `GET`, `POST` | `/api/v1/users` | List users (paged) and create a user (admin) |
| `GET`, `PATCH`, `DELETE` | `/api/v1/users/{id}` | Read, update, or delete a user (admin) |
| `PATCH` | `/api/v1/users/{id}/enabled` | Enable or disable a user (admin) |
| `GET`, `POST` | `/api/v1/network-templates` | List or create network templates |
| `GET` | `/api/v1/network-templates/{id}` | Read a template |
| `GET` | `/api/v1/network-templates/{id}/graph` | Read its graph representation |
| `POST` | `/api/v1/network-templates/{id}/assets` | Add an asset to a template |
| `POST` | `/api/v1/network-templates/{id}/edges` | Add an edge to a template |
| `GET`, `POST` | `/api/v1/scenarios` | List or create scenarios |
| `GET` | `/api/v1/scenarios/{id}` | Read a scenario and its steps |
| `POST` | `/api/v1/scenarios/{id}/steps` | Append a validated step |
| `POST`, `GET` | `/api/v1/simulation-runs` | Create a run or list the caller's runs |
| `GET` | `/api/v1/simulation-runs/{id}` | Read a run and its lifecycle status |
| `POST` | `/api/v1/simulation-runs/{id}/start` | Start a run |
| `POST` | `/api/v1/simulation-runs/{id}/execute` | Schedule a run on a background thread (returns `202`) |
| `POST` | `/api/v1/simulation-runs/{id}/step` | Execute one simulation step |
| `POST` | `/api/v1/simulation-runs/{id}/pause` or `/stop` | Change run lifecycle state |
| `GET` | `/api/v1/simulation-runs/{id}/events`, `/alerts`, `/defenses`, `/metrics` | Read run activity and metrics |
| `POST`, `GET` | `/api/v1/simulation-runs/{id}/defenses` | Apply a modeled defense or list defense history |
| `GET` | `/api/v1/simulation-runs/compare?firstRunId=1&secondRunId=2` | Compare two runs' metrics |
| `GET` | `/api/v1/alerts`, `/api/v1/alerts/{id}` | List the caller's alerts or read an alert |
| `POST` | `/api/v1/alerts/{id}/acknowledge` or `/resolve` | Update alert state |
| `GET` | `/api/v1/analytics/overview` | Read overview metrics |
| `GET`, `POST` | `/api/v1/detection-rules` | List or create detection rules |
| `GET`, `PATCH` | `/api/v1/detection-rules/{id}` | Read or update a detection rule |

Role checks apply to administrative user operations and the scenario/detection-rule write operations annotated in the controllers. Other API operations require authentication and apply run ownership/defender checks where relevant. See Swagger UI for request/response schemas and current validation constraints.

Login example (using the configured bootstrap administrator):

```sh
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"your-bootstrap-password"}'
```

Use the returned token for authenticated requests:

```sh
curl -H 'Authorization: Bearer <token>' http://localhost:8080/api/v1/scenarios
curl -X POST http://localhost:8080/api/v1/simulation-runs \
  -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
  -d '{"scenarioId":1}'
curl -X POST http://localhost:8080/api/v1/simulation-runs/1/execute \
  -H 'Authorization: Bearer <token>'
curl -H 'Authorization: Bearer <token>' http://localhost:8080/api/v1/simulation-runs/1
```

OpenAPI JSON is served at `/v3/api-docs`; Swagger UI is served at `/swagger-ui/index.html`.

## Simulation and data model

Flyway migrations in `Backend/src/main/resources/db/migration` create the core account/network/scenario schema, run and event tables, detection/defense tables, audit/reporting tables, and deterministic demo records (V1 through V5). Hibernate is configured to validate the migrated schema rather than create or mutate it.

Seeded step types include `ACCOUNT_COMPROMISE`, `LATERAL_MOVEMENT`, and `SUSPICIOUS_LOGIN`. Each simulation run snapshots its template assets and edges so simulated defenses remain isolated to that run. Supported defenses are `DISABLE_ACCOUNT`, `ISOLATE_ASSET`, `BLOCK_EDGE`, and `ACKNOWLEDGE_ALERT`. A run's event order and lifecycle are persisted in MySQL; transactions and row locks protect lifecycle transitions and run-local changes. The seeded suspicious-login scenario models a count of failed attempts in one event payload, which is an educational simplification rather than a production time-window detector.

## Troubleshooting

- **MySQL connection refused:** check that the database is running, the host/port are reachable, and all `DB_*` values match.
- **Access denied or Flyway migration failure:** verify the configured database user/password and migration grants. Do not edit a migration that has already been applied; add a new forward migration for schema changes.
- **JWT configuration error:** set a `JWT_SECRET` of at least 32 characters.
- **No admin can log in:** configure all three `BOOTSTRAP_ADMIN_*` values before first startup; demo seed data does not create a known-password administrator.
- **MySQL lifecycle test skipped:** Docker or Podman is unavailable to Testcontainers. On Bazzite, enable the user Podman socket (`systemctl --user enable --now podman.socket`) and rerun `mvn test`; the configured Maven profile picks up the standard rootless socket automatically when Maven is run in that same user session.
