# Body Mechanic

A small training-diary web application: sign up, post notes with tags and attached files, filter by tag, and manage users as an admin. Originally a learning project (2020), now cleaned up and updated.

## Features

- Registration with e-mail activation link and Google reCAPTCHA
- Login with remember-me, BCrypt-hashed passwords, role-based access (`USER`, `ADMIN`)
- Create / edit notes with a tag and an optional file attachment
- Filter notes by tag, view the notes of a single user
- Admin panel for editing users and roles
- Profile page (change e-mail / password)
- Custom 404 / 500 pages

## Stack

Java 17 · Spring Boot 2.7 (Web, Security, Data JPA, Session JDBC, Mail, Validation) · Freemarker · PostgreSQL · Flyway · Maven · Bootstrap 4

## Run locally

Requirements: JDK 17+, Maven, Docker (for PostgreSQL).

```bash
cp .env.example .env        # fill in the values
docker compose --env-file .env up -d db
set -a; . ./.env; set +a    # export the variables (bash)
mvn spring-boot:run
```

Open http://localhost:8080. Set `ADMIN_PASSWORD` to get an `admin` account on startup; there are no default credentials.

### Configuration

All settings come from environment variables (see [`.env.example`](.env.example)):

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |
| `APP_BASE_URL` | Public URL used in activation e-mails |
| `ADMIN_PASSWORD` | Creates or resets the `admin` user on startup (optional) |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | SMTP server for activation e-mails |
| `RECAPTCHA_SECRET` | Google reCAPTCHA v2 secret (the public site key is in `templates/parts/login.ftlh`) |
| `UPLOAD_PATH` | Directory for uploaded files (default `./uploads`) |

## Tests

```bash
mvn test
```

CI runs `mvn verify` on every push and pull request (see `.github/workflows/build.yml`).

## Build

```bash
mvn package
java -jar target/bodymechanic-1.0.0.jar
```
