<p align="center">
  <img src="frontend/assets/c-ops-logo.svg" alt="CyberOps Arena" width="260">
</p>

<h1 align="center">CyberOps Arena</h1>

<p align="center">A simple website for learning how a security team finds and handles cyber threats.</p>

## What is this project?

CyberOps Arena is a practice tool. You can build a small pretend network, run a threat exercise, and review the alerts it creates.

> **Safe practice:** All activity is simulated and saved in the project database. The app does not attack real computers, scan real networks, or change real accounts or firewalls.

## Get started

To use the container setup, you need Docker Compose or Podman Compose. For a manual setup, you also need Java 21, Maven, and MySQL 8.4.

1. Open a terminal in this project.
2. Go to the backend folder and open a text editor. Make a new file named `.env`:

   ```sh
   cd Backend
   ```

   Put these settings in `Backend/.env`. Replace the sample text with your own private passwords and secret:

   ```env
   DB_NAME=cyberops_arena
   DB_USERNAME=cyberops
   DB_PASSWORD=choose-a-private-database-password
   MYSQL_ROOT_PASSWORD=choose-a-private-root-password
   JWT_SECRET=replace-with-at-least-32-random-characters
   ```

   The JWT secret must have 32 or more characters. Keep this file private.
3. Start the app:

   ```sh
   docker compose up --build -d
   ```

   If you use Podman, you can run `podman compose up --build -d` instead.

4. Open **http://localhost:3000** in your browser. Create an Analyst or Defender account, then sign in.

The website is on port `3000`. The Java backend API is on port `8080`. The first start may take a little while because the app needs to download its software and prepare the database.

### Stop the app

Run this command from the `Backend` folder:

```sh
docker compose down
```

This stops the app and keeps your saved data. To erase the local database too, run `docker compose down -v`.
If you started the app with Podman, use `podman compose down` to stop it.

## How to use the website

1. **Network Lab:** Choose a network or make your own. Add pretend devices and accounts, then connect them with edges.
2. **Simulation:** Choose a saved scenario and create a run. Start it or move through it one step at a time.
3. **Incidents:** Review alerts made by your runs. Acknowledge or resolve an alert, and try a simulated defense.
4. **Overview:** Check your latest runs, alerts, and networks.

Choose **Analyst** or **Defender** when you sign up. Each role has its own access. Analysts review alerts and study what happened. Defenders practice protecting the pretend network.

On the sign-in and sign-up screen, select the doodle cat beside a password to show or hide it. When the password is hidden, the cat covers its eyes.

## Project parts

| Folder | What it contains |
| --- | --- |
| `frontend/` | The website pages and styles |
| `Backend/` | The Java web API and database setup |
| `Backend/src/main/resources/db/migration/` | Database setup steps, run by Flyway |

The app uses Java 21, Spring Boot, MySQL 8.4, and a plain HTML frontend. It uses Docker Compose or Podman Compose to start the parts together.

## Run the website without containers

You can run the backend with a local MySQL server, then serve the website by itself.

First, create a MySQL database and user. Flyway needs permission to create and update database tables:

```sql
CREATE DATABASE cyberops_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'cyberops'@'localhost' IDENTIFIED BY 'choose-a-long-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON cyberops_arena.* TO 'cyberops'@'localhost';
```

In one terminal, go to `Backend/`, set your database and JWT settings, then start the backend:

```sh
cd Backend
export DB_HOST=localhost DB_PORT=3306 DB_NAME=cyberops_arena
export DB_USERNAME=cyberops DB_PASSWORD='your-database-password'
export JWT_SECRET='use-at-least-32-random-characters-here'
mvn spring-boot:run
```

In another terminal, go to the project folder and start the website:

```sh
python3 -m http.server 3000 --directory frontend
```

Open **http://localhost:3000**. The sign-in page uses `http://localhost:8080/api/v1` for the backend by default. You can change the backend address on that page.

## Test the backend

From the `Backend/` folder, run:

```sh
mvn test
```

Some tests need Docker or Podman to start a temporary MySQL database. If no container service is available, the database test may be skipped.

## Useful links

- Website: [http://localhost:3000](http://localhost:3000)
- Backend health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- API guide: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- API details in JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## A note about the Java class project

The backend shows Java packages, classes, constructors, interfaces, and different classes that implement those interfaces. It also uses methods, streams, error handling, JDBC database work, and background threads.

The project has a web page. It does **not** include a Java Swing desktop app. If a Swing app is required for your class, it must be made as a separate program and can connect to this backend API.

## Need help?

- **The website does not open:** Check that the frontend is running on port `3000`.
- **Sign-in cannot reach the backend:** Check that the backend is running on port `8080` and that the address on the sign-in page is correct.
- **The database will not start:** Check the database name, user, and passwords in `Backend/.env`.
- **The backend reports a JWT error:** Set a JWT secret that has at least 32 characters.
- **You need an Admin account:** Set `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` in `Backend/.env`, then restart the backend. The password must have at least 14 characters. New sign-ups can only choose Analyst or Defender.

Do not share your `.env` file or add it to Git. It contains private passwords and secrets.
