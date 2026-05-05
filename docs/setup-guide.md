# Setup Guide

Detailed local setup instructions for the IoT Health Monitoring Platform.
For a quick start, see the [README](../README.md).

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java JDK | 17+ | Required for the backend |
| Apache Maven | 3.8+ | Or use the included `mvnw` wrapper |
| PostgreSQL | 14+ | Must be running before starting the backend |
| Node.js | 20.19+ or 22.12+ | Required for the frontend |
| Python | 3.12 (3.11 acceptable) | Required for the simulator |
| Git | Any recent version | |

---

## 1. Clone the repository

```powershell
git clone https://github.com/ME-Massine/IoT-Health-Monitoring-Platform.git
cd IoT-Health-Monitoring-Platform
```

---

## 2. Database setup

### Create the database

```sql
CREATE DATABASE iot_health_db;
```

### Configure the backend

Copy the example config and fill in your local values:

```powershell
Copy-Item backend/src/main/resources/application.example.yml `
           backend/src/main/resources/application.yml
```

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: iot-health-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: your_password        # ← change this

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info

app:
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000
  websocket:
    allowed-origins: http://localhost:5173,http://localhost:3000
```

> `application.yml` is git-ignored. Never commit it.

---

## 3. Backend

### Run

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend starts on **http://localhost:8080**.

### Verify

```
GET http://localhost:8080/actuator/health
→ { "status": "UP" }
```

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

### Run tests

```powershell
.\mvnw.cmd test
```

---

## 4. Frontend

### Install and configure

```powershell
cd frontend
npm install
```

Create `frontend/.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=http://localhost:8080/ws
```

### Run

```powershell
npm run dev
```

Frontend starts on **http://localhost:5173**.

### Build for production

```powershell
npm run build
```

---

## 5. Simulator

### Setup

```powershell
cd simulator
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Create `simulator/.env`:

```env
API_BASE_URL=http://localhost:8080/api/v1
DEVICE_CODES=DEV-001
INTERVAL_SECONDS=5
ANOMALY_RATE=0.10
```

### Run

```powershell
python simulator.py
```

The simulator validates every device code against the backend at startup.
Codes that are not registered are skipped. If all codes fail, the simulator exits.

To use multiple devices:

```env
DEVICE_CODES=DEV-001,DEV-002,DEV-003
```

Each device must be registered via `POST /api/v1/devices` before starting.

---

## 6. Seed demo data

Before the simulator can send data, at least one patient and one device must exist.

### Create a patient

```http
POST http://localhost:8080/api/v1/patients
Content-Type: application/json

{
  "firstName": "Sara",
  "lastName": "El Amrani",
  "age": 42,
  "gender": "FEMALE",
  "roomNumber": "A-102",
  "medicalCondition": "Cardiac observation"
}
```

### Register a device

```http
POST http://localhost:8080/api/v1/devices
Content-Type: application/json

{
  "deviceCode": "DEV-001",
  "type": "Multi-parameter monitor",
  "status": "ACTIVE",
  "patientId": 1
}
```

Use the `id` returned from the patient creation as `patientId`.

---

## 7. Troubleshooting

**PostgreSQL connection error**
Verify the service is running, `iot_health_db` exists, and the password in `application.yml` is correct.

**Port 8080 already in use**
Change `server.port` in `application.yml` to `8081` and update `VITE_API_BASE_URL` and `VITE_WS_URL` in the frontend `.env.local`.

**Frontend shows "Failed to load patients"**
Confirm `app.cors.allowed-origins` in `application.yml` includes `http://localhost:5173`.

**Simulator exits immediately**
Ensure device codes in `DEVICE_CODES` are registered in the backend. Check the simulator log for the specific error per device code.

**Maven dependencies not loading**
```powershell
.\mvnw.cmd clean install
```

**Node version issues**
Use `nvm` or `nvm-windows` to switch to Node 20.19+ or 22.12+.