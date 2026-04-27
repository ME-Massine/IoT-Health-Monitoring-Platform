# Setup Guide

This guide provides instructions for setting up the IoT Health Monitoring Platform. Please ensure all prerequisites are met before proceeding.

## 1. Prerequisites

To set up the platform, the following software must be installed:

- **Java Development Kit (JDK) 17**: Required for running the backend application.
- **Apache Maven**: Used for building and managing Java projects.
- **PostgreSQL**: The relational database system used by the platform.
- **Git**: For cloning the project repository and managing version control.
- **Integrated Development Environment (IDE)**: Recommended options include IntelliJ IDEA or VS Code for development.

## 2. Clone the Repository

To obtain the project source code, execute the following commands in your terminal:

```bash
git clone https://github.com/ME-Massine/IoT-Health-Monitoring-Platform.git
cd IoT-Health-Monitoring-Platform
```

## 3. Database Setup

### Create the PostgreSQL Database

First, create the required PostgreSQL database:

```sql
CREATE DATABASE iot_health_db;
```

### Configure the Backend

The repository includes an example configuration file:

`backend/src/main/resources/application.example.yml`

Create your local configuration file by copying it:

```bash
cp backend/src/main/resources/application.example.yml backend/src/main/resources/application.yml
```

On Windows PowerShell:

```powershell
Copy-Item backend/src/main/resources/application.example.yml backend/src/main/resources/application.yml
```

Then open:

`backend/src/main/resources/application.yml`

Update the PostgreSQL password:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: your_password
```

## 4. Run the Backend

Navigate to the `backend` directory and start the Spring Boot application:

```bash
cd backend
mvn spring-boot:run
```

For Windows users, use the following command:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, you can run the tests to verify the backend:

```powershell
.\mvnw.cmd clean test
```

## 5. Test the Health Check Endpoint

After the backend is running, verify its status by accessing the health check endpoints:

### API v1 Health Check

Open your web browser or use a tool like `curl` to access:

`http://localhost:8080/api/v1/health-check`

**Expected Response:**

```json
{
  "status": "UP",
  "service": "iot-health-backend"
}
```

### Actuator Health Check

Access the Spring Boot Actuator health endpoint:

`http://localhost:8080/actuator/health`

**Expected Response:**

```json
{
  "status": "UP"
}
```

## 6. Git Workflow

Follow these steps for a standard Git workflow when contributing to the project:

### Create a Feature Branch

Always create a new feature branch from the `main` branch:

```bash
git checkout main
git pull origin main
git checkout -b feature/your-feature-name
```

### Commit and Push Changes

After making your changes, commit and push them to your feature branch:

```bash
git add .
git commit -m "feat: describe your change"
git push -u origin feature/your-feature-name
```

Finally, open a pull request into the `main` branch.

## 7. Troubleshooting

### PostgreSQL Connection Error

If you encounter issues connecting to PostgreSQL, verify the following:

- PostgreSQL service is running.
- The database name (`iot_health_db`) is correct.
- The username (`postgres`) and password in `application.yml` are correct.
- The default PostgreSQL port (5432) is being used.

### Port 8080 Already in Use

If port 8080 is already in use by another application, you can change the backend's port in `application.yml`:

```yaml
server:
  port: 8081
```

### Maven Dependencies Not Loading

If Maven dependencies fail to load, try cleaning and reinstalling them:

```bash
mvn clean install
```
