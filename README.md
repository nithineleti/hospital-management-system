# Hospital Database Management System

A Spring Boot based hospital management application for handling patients, doctors, staff, appointments, inventory, prescriptions, and related operations.

## Project Status
- Status: Running locally and actively maintained.
- Last updated: March 12, 2026.
- Security: High-severity CVE remediation has been applied.

## What Has Been Completed
- Core modules for patient, doctor, staff, prescription, inventory, and supplier management.
- Authentication and authorization with Spring Security.
- Web UI with Thymeleaf templates.
- REST API controllers for key entities.
- H2 in-memory database setup for local development.
- Dependency security update: `com.mysql:mysql-connector-j` updated to `9.1.0`.
- Spring Boot baseline upgraded to `3.4.3`.
- Application start/run verified at `http://localhost:8080/hospital/`.

## Tech Stack
- Java 17+
- Spring Boot 3.4.3
- Spring Security
- Spring Data JPA + Hibernate
- Thymeleaf
- H2 Database (default local profile)
- Maven

## Project Structure
```text
src/main/java/com/hospital/
  config/
  controller/
  model/
  repository/
  service/
src/main/resources/
  application.properties
  templates/
  static/
```

## Quick Run
1. Ensure Java and Maven are installed.
2. From project root:
```bash
mvn clean install
mvn spring-boot:run
```
3. Open:
- `http://localhost:8080/hospital/`
- `http://localhost:8080/hospital/auth/login`

For full platform-specific instructions, see `RUN_PROJECT.md`.

## Configuration
Main config file: `src/main/resources/application.properties`
- Port: `8080`
- Context path: `/hospital`
- Database URL: `jdbc:h2:mem:hospital_db`

## Added Documentation Files
- `FUTURE_ENHANCEMENTS.md`: roadmap and improvement backlog.
- `RUN_PROJECT.md`: complete run instructions and troubleshooting.
- `SKILLS_USED_IN_PROJECT.md`: technologies and engineering skills used.
- `PROJECT_OVERVIEW_SIMPLE.md`: easy-to-understand summary for quick onboarding.

## Notes
- The repository includes many archived documents under `archived_md_files/`.
- The files listed above are the current concise guides for day-to-day development.

## Docker & Cloud Deployment

This project can be containerized and deployed to Google Cloud Platform (GCP) using Cloud Run or Compute Engine. A `Dockerfile` and `cloudbuild.yaml` have been added to support building the image.

### Building the Docker image locally

```bash
# build the Maven package and create container image
docker build -t hospital-management-system:latest .

# run the container on your machine (port is forwarded automatically)
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  hospital-management-system:latest
```

After the container starts you can access the app at `http://localhost:8080/hospital/`.

### Publishing to Google Container Registry and deploying to Cloud Run

1. Authenticate with GCP and set your project ID:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID
   ```
2. Build and push the image:
   ```bash
   docker build -t gcr.io/$GOOGLE_CLOUD_PROJECT/hospital-management-system:latest .
   docker push gcr.io/$GOOGLE_CLOUD_PROJECT/hospital-management-system:latest
   ```
   Alternatively use Cloud Build:
   ```bash
   gcloud builds submit --tag gcr.io/$GOOGLE_CLOUD_PROJECT/hospital-management-system
   ```
3. Deploy to Cloud Run:
   ```bash
   gcloud run deploy hospital-management-system \
     --image gcr.io/$GOOGLE_CLOUD_PROJECT/hospital-management-system:latest \
     --platform managed --region us-central1 \
     --allow-unauthenticated --port 8080
   ```

The service URL will be printed after deployment; open it in your browser.

> **Note:** `server.port` in `application.properties` is now configured as `\${PORT:8080}` so Cloud Run can assign the port dynamically.

### Using Cloud Build

A `cloudbuild.yaml` file is provided for automated builds:

```yaml
steps:
  - name: 'gcr.io/cloud-builders/mvn'
    args: ['clean','package','-DskipTests']
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build','-t','gcr.io/$PROJECT_ID/hospital-management-system','.',]
images:
  - 'gcr.io/$PROJECT_ID/hospital-management-system'
```

To trigger a build manually:
```bash
gcloud builds submit --config cloudbuild.yaml .
```

### Connecting to a Cloud SQL database

If you deploy on Google Cloud Run (or another container service) you'll likely want a persistent database rather than the in‑memory H2 instance.
A common pattern is to use **Cloud SQL (MySQL)** and connect via the Cloud SQL Auth proxy or the native socket support.

1. **Create a Cloud SQL instance** (MySQL 8 or later) in your GCP project.
2. **Enable the Cloud SQL Admin API** and grant the service account used by Cloud Run access to the instance.
3. **Add the Cloud SQL Auth proxy** to your container or use the built‑in connector.  For example, with the proxy:
   ```dockerfile
   # in your Dockerfile add before ENTRYPOINT
   COPY cloud_sql_proxy /cloud_sql_proxy
   ENTRYPOINT ["/cloud_sql_proxy","-instances=$CLOUD_SQL_CONNECTION_NAME=tcp:3306","&&","java","-jar","app.war"]
   ```
   or run the proxy alongside the container with `docker run --network`.
4. **Set environment variables** when you deploy:
   ```bash
   gcloud run deploy hospital-management-system \
     --image gcr.io/$GOOGLE_CLOUD_PROJECT/hospital-management-system:latest \
     --update-env-vars \
       SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/hospital_db,\
       SPRING_DATASOURCE_USERNAME=root,\
       SPRING_DATASOURCE_PASSWORD=secret,\
       CLOUD_SQL_CONNECTION_NAME=your-project:region:instance
   ```
   adjust the URL if using the native socket (e.g. `jdbc:mysql:///<DB_NAME>?socket=/cloudsql/$CLOUD_SQL_CONNECTION_NAME`).

The application properties default to H2 but will honour the `SPRING_DATASOURCE_*` environment variables above.

### Automating build & deploy

You can trigger builds automatically on commits by hooking the repository to Cloud Build (via the GCP console or `gcloud beta builds triggers create`).  A simple trigger configuration would run the `cloudbuild.yaml` in this repo on every push to `main` and then deploy the resulting image to Cloud Run using a `gcloud run deploy` step.

Example `cloudbuild.yaml` with a deploy step:
```yaml
steps:
  - name: 'gcr.io/cloud-builders/mvn'
    args: ['clean','package','-DskipTests']
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build','-t','gcr.io/$PROJECT_ID/hospital-management-system','.',]
  - name: 'gcr.io/cloud-builders/docker'
    args: ['push','gcr.io/$PROJECT_ID/hospital-management-system']
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'bash'
    args:
      - '-c'
      - |
        gcloud run deploy hospital-management-system \
          --image gcr.io/$PROJECT_ID/hospital-management-system:latest \
          --region us-central1 --platform managed \
          --allow-unauthenticated
images:
  - 'gcr.io/$PROJECT_ID/hospital-management-system'
```

Once the trigger is configured, every push to `main` will build the image and roll out a new revision of the Cloud Run service.

### Notes

- Ensure the appropriate database (MySQL/Cloud SQL) is accessible from the container. For Cloud Run, you can connect to Cloud SQL using the Cloud SQL Auth proxy or the native connector.
- Adjust any environment-specific configuration via environment variables or a Spring profile.

For further details on Google Cloud deployment, consult the GCP documentation.

> After cloning you can make the helper script executable:
> ```bash
> chmod +x deploy.sh
> ```
