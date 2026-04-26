# IoT Health Monitoring Platform Guide

This document provides a comprehensive guide for setting up, developing, and contributing to the IoT Health Monitoring Platform project.

## 1. Setup Guide

This section outlines the steps required to get the IoT Health Monitoring Platform running locally.

### 1.1. Prerequisites

To set up the platform, the following software must be installed:

- **Java Development Kit (JDK) 17**: Required for running the backend application.
- **Apache Maven**: Used for building and managing Java projects.
- **PostgreSQL**: The relational database system used by the platform.
- **Git**: For cloning the project repository and managing version control.
- **Integrated Development Environment (IDE)**: Recommended options include IntelliJ IDEA or VS Code for development.

### 1.2. Clone the Repository

To obtain the project source code, execute the following commands in your terminal:

```bash
git clone https://github.com/ME-Massine/IoT-Health-Monitoring-Platform.git
cd IoT-Health-Monitoring-Platform
```

### 1.3. Database Setup

#### Create the PostgreSQL Database

First, create the necessary database in PostgreSQL:

```sql
CREATE DATABASE iot_health_db;
```

#### Default Backend Configuration

The backend application's database connection details are configured as follows:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: your_password
```

**Action Required**: Update the `backend/src/main/resources/application.yml` file with your local PostgreSQL password.

### 1.4. Run the Backend

Navigate to the `backend` directory and start the Spring Boot application:

```bash
cd backend
mvn spring-boot:run
```

For Windows users, use the following command:

```bash
.\mvnw.cmd spring-boot:run
```

### 1.5. Test the Health Check Endpoint

After the backend is running, verify its status by accessing the health check endpoints:

#### API v1 Health Check

Open your web browser or use a tool like `curl` to access:

`http://localhost:8080/api/v1/health-check`

**Expected Response:**

```json
{
  "status": "UP",
  "service": "iot-health-backend"
}
```

#### Actuator Health Check

Access the Spring Boot Actuator health endpoint:

`http://localhost:8080/actuator/health`

**Expected Response:**

```json
{
  "status": "UP"
}
```

### 1.6. Troubleshooting

#### PostgreSQL Connection Error

If you encounter issues connecting to PostgreSQL, verify the following:

- PostgreSQL service is running.
- The database name (`iot_health_db`) is correct.
- The username (`postgres`) and password in `application.yml` are correct.
- The default PostgreSQL port (5432) is being used.

#### Port 8080 Already in Use

If port 8080 is already in use by another application, you can change the backend's port in `application.yml`:

```yaml
server:
  port: 8081
```

#### Maven Dependencies Not Loading

If Maven dependencies fail to load, try cleaning and reinstalling them:

```bash
mvn clean install
```

## 2. Branching Strategy

This project utilizes a simple two-person workflow with the following branch types:

- `main`: The stable branch, representing the latest production-ready code.
- `feature/*`: Development branches for implementing new features, bug fixes, or improvements.

All development work **must** be conducted within feature branches and integrated into `main` via pull requests.

**Example Workflow for a Feature Branch:**

```bash
git checkout main
git pull origin main
git checkout -b feature/patient-module
```

## 3. Team Workflow

To ensure a consistent and collaborative development process, follow these steps:

1.  **Pick or Create an Issue**: Select an existing GitHub issue to work on, or create a new one if necessary.
2.  **Create a Feature Branch**: Branch off from `main` for your development work.
3.  **Implement Changes**: Develop the required functionality or fix the bug.
4.  **Commit with Clear Messages**: Use descriptive commit messages following the established conventions.
5.  **Push Branch**: Push your feature branch to the remote repository.
6.  **Open a Pull Request**: Create a pull request targeting the `main` branch.
7.  **Request Review**: Ask a teammate to review your changes.
8.  **Merge After Review**: Only merge the pull request into `main` after it has been reviewed and approved.

## 4. Commit Message Examples

Adhere to the following conventions for clear and informative commit messages:

- `feat`: For new features (e.g., `feat: add patient entity and repository`)
- `fix`: For bug fixes (e.g., `fix: complete global exception handling classes`)
- `docs`: For documentation updates (e.g., `docs: update setup guide`)
- `chore`: For routine tasks or maintenance (e.g., `chore: configure backend project`)
- `test`: For adding or modifying tests (e.g., `test: add patient service tests`)

## 5. Current Development Status

The project is currently in the **backend foundation phase**.

### Completed:

- Repository setup
- GitHub Project planning
- Spring Boot backend initialization
- Global exception handling
- Initial setup guide

### In Progress:

- Patient and device management
- Vital signs ingestion
- Alert detection system

## 6. `application.example.yml`

This file provides an example configuration for the backend application. Create the file at `backend/src/main/resources/application.example.yml` with the following content:

```yaml
spring:
  application:
    name: iot-health-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: your_password

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
```
