# TBMJA API Module

### Introduction
TBMJA (TB Mukt Jan Abhiyan) is the TB screening programme run under AMRIT. Camps screen people for TB with
symptom screening, digital chest X-ray (CXR) and NAAT (Truenat) tests.

Some results do not give a clear outcome: an abnormal X-ray with a negative or not-done NAAT, a symptomatic or
high-risk person who could not get an X-ray, or a child aged 3 to 14. These beneficiaries need a review by a
Clinical Specialist (a Medical Officer, chest physician, radiologist or other designated doctor).

### About TBMJA-API
TBMJA-API is the backend of the Clinical Specialist web module. Planned features:
1. Line listing of beneficiaries waiting for clinical assessment (Pending: Initial and Re-review, Completed).
2. Beneficiary clinical summary and diagnostics (X-ray result and image, NAAT, liquid culture), read-only.
3. Clinical Assessment form: interpretation, outcome and recommended advice.
4. Clinical assessment dashboard indicators.

Clinical Specialist doctors are registered by Admin through Admin-API Employee Master (`/createNewUser` with
`isClinicianSpecialist: true`) and log in through Common-API (`/user/userAuthenticate`), like every AMRIT user.

Reference documents (Confluence, AMRIT space):
- TBMJA - Clinical Specialist PRD
- Clinical Specialist Module - Remote X-ray Review

### Building From Source
This microservice is built on Java, Spring Boot and MySQL.

### Pre-requisites
* JDK 17
* Maven 3.8 or later
* MySQL 8 with the `db_iemr` schema, migrated by [AMRIT-DB](https://github.com/PSMRI/AMRIT-DB)
* Redis
* IntelliJ IDEA, Eclipse or Spring Tool Suite

### Run locally
1. Copy `src/main/environment/common_example.properties` to `src/main/environment/common_local.properties`
   (this file is gitignored) and fill in the database, Redis and JWT values. `jwt.secret` must be the same as
   in the other AMRIT APIs.
2. Build and run:
   ```
   mvn clean install -DENV_VAR=local
   mvn spring-boot:run -DENV_VAR=local
   ```
3. Check the API:
   - Health: `GET http://localhost:8095/health`
   - Version: `GET http://localhost:8095/version`
   - Swagger UI: `http://localhost:8095/swagger-ui.html`

The build appends `src/main/environment/common_<ENV_VAR>.properties` to `application.properties`.
Available environments: `example`, `local`, `ci` (values from build-time environment variables) and `docker`
(values from container environment variables).

To start without MySQL or Redis (for example to export the Swagger JSON):
```
java -jar target/tbmja-api-1.0.0.war --spring.profiles.active=swagger
```

### Docker
```
docker build -t tbmja-api .
docker run -p 8080:8080 \
  -e DATABASE_URL=... -e DATABASE_USERNAME=... -e DATABASE_PASSWORD=... \
  -e REDIS_HOST=... -e JWT_SECRET_KEY=... -e CORS_ALLOWED_ORIGINS=... \
  -e TBMJA_API_LOGGING_FILE_NAME=logs/tbmja-api.log \
  tbmja-api
```

### Project structure
```
src/main/java/com/iemr/tbmja/
  TbmjaApplication.java   entry point
  config/                 Swagger, Redis
  controller/             REST endpoints (health, version, ...)
  service/                business logic
  data/iemr/              db_iemr entities
  repo/iemr/              db_iemr repositories
  utils/                  JWT filter and helpers, exceptions
src/main/environment/     per-environment properties
src/main/resources/       application.properties, swagger profile, logback
```

### Authentication
Users log in through Common-API, which issues the JWT. Every endpoint here except `/health`, `/version`,
Swagger and `/public/**` needs that JWT, sent in the `Jwttoken` cookie or the `Jwttoken` header. The token must be signed with `jwt.secret`, carry a `userId`
claim, not be revoked (Redis denylist) and belong to an active user in `m_user`.

## Setting Up Commit Hooks

This project uses Git hooks to enforce consistent code quality and commit message standards. Even though this
is a Java project, the hooks are powered by Node.js.

### Prerequisites
- Node.js (v18 or later)
- npm (comes with Node.js)

### Setup Steps
1. Install Node.js and npm from [nodejs.org](https://nodejs.org/).
2. From the project root, run `npm install`. This installs Husky and commitlint.
3. The hooks in `.husky` run automatically on commit.

### Commit Message Convention
- Format: `type(scope): subject`
- Example: `feat(worklist): add clinical assessment line listing`

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

## Filing Issues

If you encounter any issues, bugs, or have feature requests, please file them in the
[main AMRIT repository](https://github.com/PSMRI/AMRIT/issues).

## Join Our Community

Join our [Discord server](https://discord.gg/FVQWsf5ENS) to connect with contributors, ask questions, and stay
updated.

## License

AMRIT is released under the GNU General Public License v3.0. See [LICENSE](LICENSE).
