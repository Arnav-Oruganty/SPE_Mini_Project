# Calculator REST API — CI/CD Mini Project

A Spring Boot calculator API packaged with Maven and deployed as an app-plus-Nginx Docker Compose stack. A GitHub push can trigger Jenkins to build, test, publish, and deploy the image automatically!

## Architecture

```
Developer → GitHub → webhook → Jenkins → Docker Hub
                                  │
                                  └── Docker Compose
                                         │
                              Nginx : APP_PORT
                                         │
                              Calculator API : 8080
```

## Prerequisites

| Tool | Required version |
| --- | --- |
| Java | 17+ |
| Maven | 3.9+ |
| Docker Engine | 24+ recommended |
| Docker Compose | v2 |
| Jenkins | Required only for CI/CD |

## API

All arithmetic endpoints require numeric `a` and `b` query parameters.

| Operation | Endpoint example |
| --- | --- |
| Add | `GET /api/v1/add?a=2&b=3` |
| Subtract | `GET /api/v1/subtract?a=2&b=3` |
| Multiply | `GET /api/v1/multiply?a=2&b=3` |
| Divide | `GET /api/v1/divide?a=10&b=4` |
| Health | `GET /actuator/health` |

Successful responses are JSON, e.g. `{"operation":"add","a":2.0,"b":3.0,"result":5.0}`. Missing/non-numeric parameters and division by zero return HTTP 400 with a structured error JSON body.

## Local build and test

```bash
mvn clean package
mvn test
java -jar target/calculator-api-1.0.0.jar
```

In another terminal:

```bash
curl 'http://localhost:8080/api/v1/add?a=2&b=3'
curl http://localhost:8080/actuator/health
```

JUnit XML reports are written to `target/surefire-reports/` for Jenkins to publish.

## Configuration

Copy the template, then replace the two placeholder values:

```bash
cp .env.example .env
```

| Variable | Purpose | Example |
| --- | --- | --- |
| `DOCKER_IMAGE` | Docker Hub repository to publish | `arnavoruganty/calculator-api` |
| `NOTIFICATION_EMAIL` | Jenkins build-notification recipient | `you@example.com` |
| `APP_PORT` | Host port exposed by Nginx | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |

The `.env` file is excluded from Git. Do not add a Docker Hub password to it.

## Docker Compose deployment

Requires Docker Engine plus the Docker Compose v2 plugin.

```bash
docker compose up --build -d
docker compose ps
curl 'http://localhost:8080/api/v1/divide?a=10&b=4'
docker compose logs -f
docker compose down
```

Nginx is the public endpoint at port 8080; the Spring Boot container stays on the private Compose network. Set `APP_PORT=9090` before `docker compose up` to use another host port.

## Docker image

Build and run only the API container if you do not need Nginx:

```bash
docker build -t calculator-api:local .
docker run --rm -p 8080:8080 calculator-api:local
```

## Jenkins setup

1. Install Pipeline, Git, GitHub, JUnit, Mailer, and Timestamper Jenkins plugins.
2. Create a Pipeline job using Pipeline script from SCM, pointing to this repository. Enable GitHub hook trigger for GITScm polling.
3. Under Manage Jenkins → Credentials, add:Credential IDTypeContentsdockerhub-credentialsUsername with passwordDocker Hub username and access token/passwordcalculator-envSecret fileYour completed .env file
4. Configure SMTP under Manage Jenkins → System → E-mail Notification.
5. Ensure the Jenkins agent has Docker, Docker Compose, Maven, curl, and permission to access Docker. On Linux this commonly requires sudo usermod -aG docker jenkins, followed by a Jenkins restart.
6. Add a GitHub repository webhook to http://YOUR_JENKINS_HOST/github-webhook/ when Jenkins has a public URL. When Jenkins is only local, this project uses Poll SCM every five minutes as the allowed automatic-trigger fallback.

The pipeline restores `.env` from the Jenkins Secret File credential, builds and tests the project, publishes JUnit results, pushes both a build-number tag and `latest` to Docker Hub, deploys with Compose, runs a health/smoke check, and emails the result. Concurrent builds are disabled so deployments cannot conflict.

## Project structure

```
src/main/java/...       Spring Boot application
src/test/java/...       JUnit and MockMvc tests
nginx/default.conf      Nginx reverse-proxy configuration
Dockerfile              Multi-stage, non-root container image
docker-compose.yml      Calculator API + Nginx deployment
Jenkinsfile             Declarative CI/CD pipeline
.env.example            Configuration template
```