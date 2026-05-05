# IoT Health Monitoring Platform

A full-stack IoT health monitoring system that simulates connected medical devices sending real-time vital signs to a backend API. The backend detects abnormal readings, generates alerts, and broadcasts live updates via WebSocket. A React dashboard displays patient data, vital sign history, and alerts in real time.

Built as a school/portfolio project. No physical hardware required — a Python simulator drives the full demo flow.

---

## Features

- Real-time vital sign ingestion from simulated IoT devices
- Automatic alert detection for abnormal heart rate, temperature, and SpO2 readings
- Live WebSocket push to the frontend dashboard (no polling)
- Patient management with assigned devices
- Alert resolution from the UI or API
- Configurable Python simulator with realistic drift and anomaly injection
- REST API documented via Swagger UI
- Service and controller-layer unit tests

---

## Architecture

```
┌─────────────────┐        HTTP POST         ┌──────────────────────────┐
│ Python Simulator│ ───────────────────────► │  Spring Boot Backend     │
│  (simulator/)   │                          │  Port 8080               │
└─────────────────┘                          │                          │
                                             │  - Patient API           │
┌─────────────────┐     REST + WebSocket     │  - Device API            │
│ React Frontend  │ ◄──────────────────────► │  - Vital Sign Ingestion  │
│  (frontend/)    │     Port 5173            │  - Alert Detection       │
└─────────────────┘                          │  - WebSocket Broker      │
                                             └────────────┬─────────────┘
                                                          │
                                                          ▼
                                             ┌──────────────────────────┐
                                             │  PostgreSQL              │
                                             │  Port 5432               │
                                             └──────────────────────────┘
```

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, Spring WebSocket (STOMP) |
| Frontend | React 19, Vite, React Router, Axios, @stomp/stompjs, SockJS |
| Simulator | Python 3.12, requests, python-dotenv |
| Database | PostgreSQL |
| API Docs | SpringDoc OpenAPI (Swagger UI) |

---

## Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 17+ |
| Apache Maven | 3.8+ (or use the included `mvnw`) |
| PostgreSQL | 14+ |
| Node.js | 20.19+ or 22.12+ |
| Python | 3.12 (3.11 acceptable) |
| Git | Any recent version |

---

## Project Structure

```
IoT-Health-Monitoring-Platform/
├── backend/                  # Spring Boot application
│   ├── src/main/java/        # Application source
│   ├── src/test/java/        # Service and controller tests
│   └── src/main/resources/
│       ├── application.example.yml   # Committed config template
│       └── application.yml           # Local config (git-ignored)
├── frontend/                 # React + Vite application
│   ├── src/
│   │   ├── api/              # Axios clients and WS factory
│   │   ├── components/       # Reusable UI components
│   │   ├── hooks/            # Data-fetching and WS hooks
│   │   ├── pages/            # Dashboard, Detail, Alert Center
│   │   └── routes/           # React Router config
│   └── .env.example
├── simulator/                # Python sensor simulator
│   ├── simulator.py
│   ├── requirements.txt
│   └── .env.example
└── docs/
    ├── setup-guide.md
    ├── websocket-topics.md
    └── demo-scenario.md
```

---

## Backend Setup

### 1. Create the database

```sql
CREATE DATABASE iot_health_db;
```

### 2. Configure the application

```powershell
Copy-Item backend/src/main/resources/application.example.yml `
           backend/src/main/resources/application.yml
```

Edit `application.yml` and set your PostgreSQL password:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: your_password

app:
  cors:
    allowed-origins: http://localhost:5173,http://localhost:3000
  websocket:
    allowed-origins: http://localhost:5173,http://localhost:3000
```

### 3. Run the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend starts on **http://localhost:8080**.

Verify:
```
GET http://localhost:8080/actuator/health  →  { "status": "UP" }
```

### 4. Run backend tests

```powershell
.\mvnw.cmd test
```

### 5. Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## Frontend Setup

```powershell
cd frontend
npm install
cp .env.example .env.local   # or copy manually
npm run dev
```

Frontend starts on **http://localhost:5173**.

### Frontend environment (`.env.local`)

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=http://localhost:8080/ws
```

---

## Simulator Setup

```powershell
cd simulator
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env          # or copy manually
python simulator.py
```

### Simulator environment (`.env`)

```env
API_BASE_URL=http://localhost:8080/api/v1
DEVICE_CODES=DEV-001
INTERVAL_SECONDS=5
ANOMALY_RATE=0.10
```

`DEVICE_CODES` accepts a comma-separated list. Each code must be registered in the database before starting the simulator. The simulator validates all codes at startup and skips any that are rejected by the backend.

---

## REST API Overview

Full interactive documentation: **http://localhost:8080/swagger-ui/index.html**

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/patients` | Create a patient |
| `GET` | `/api/v1/patients` | List all patients |
| `GET` | `/api/v1/patients/{id}` | Get patient by ID |
| `PUT` | `/api/v1/patients/{id}` | Update a patient |
| `DELETE` | `/api/v1/patients/{id}` | Delete a patient |
| `POST` | `/api/v1/devices` | Register a device |
| `GET` | `/api/v1/devices` | List all devices |
| `GET` | `/api/v1/devices/{id}` | Get device by ID |
| `GET` | `/api/v1/devices/code/{code}` | Get device by code |
| `GET` | `/api/v1/devices/patient/{id}` | Get device by patient |
| `PUT` | `/api/v1/devices/{id}` | Update a device |
| `DELETE` | `/api/v1/devices/{id}` | Delete a device |
| `POST` | `/api/v1/vitals` | Ingest a vital sign reading |
| `GET` | `/api/v1/vitals/patient/{id}/latest` | Latest reading for a patient |
| `GET` | `/api/v1/vitals/patient/{id}/history` | Vital sign history |
| `GET` | `/api/v1/alerts` | All alerts |
| `GET` | `/api/v1/alerts/unresolved` | Unresolved alerts |
| `GET` | `/api/v1/alerts/patient/{id}` | Alerts for a patient |
| `PUT` | `/api/v1/alerts/{id}/resolve` | Resolve an alert |

---

## WebSocket Topics

WebSocket endpoint: `ws://localhost:8080/ws` (STOMP over SockJS)

| Topic | Description |
|---|---|
| `/topic/vitals` | All incoming vital sign readings |
| `/topic/patients/{id}/vitals` | Vital signs for a specific patient |
| `/topic/alerts` | All new alerts |
| `/topic/patients/{id}/alerts` | Alerts for a specific patient |

See [docs/websocket-topics.md](docs/websocket-topics.md) for full details.

---

## Alert Thresholds

| Vital | Warning | Critical |
|---|---|---|
| Heart rate | ≥ 110 bpm | < 50 or > 120 bpm |
| Temperature | ≥ 37.8 °C | < 35.0 or > 38.0 °C |
| SpO2 | ≤ 94 % | ≤ 92 % |

---

## Demo Flow

1. Start PostgreSQL
2. Start the backend (`mvnw spring-boot:run`)
3. Start the frontend (`npm run dev`)
4. Seed at least one patient and one device via the API or Swagger UI
5. Start the simulator (`python simulator.py`)
6. Watch live vitals update on the dashboard
7. Wait for an anomaly (or raise `ANOMALY_RATE` temporarily) to see alerts appear in real time
8. Resolve an alert from the Alert Center or via `PUT /api/v1/alerts/{id}/resolve`

Full walkthrough: [docs/demo-scenario.md](docs/demo-scenario.md)

---

## Git Workflow

```
main          ← stable, always deployable
feature/*     ← all development work
```

1. Branch off `main`: `git checkout -b feature/your-feature`
2. Commit with conventional prefixes: `feat:`, `fix:`, `docs:`, `test:`, `chore:`
3. Push and open a PR targeting `main`
4. Merge after review

---

## Documentation

| Doc | Description |
|---|---|
| [docs/setup-guide.md](docs/setup-guide.md) | Detailed local setup instructions |
| [docs/websocket-topics.md](docs/websocket-topics.md) | WebSocket topic reference |
| [docs/demo-scenario.md](docs/demo-scenario.md) | Step-by-step presentation script |

---

## Troubleshooting

**PostgreSQL connection error** — verify the service is running, the database `iot_health_db` exists, and the password in `application.yml` is correct.

**Port 8080 already in use** — change `server.port` in `application.yml`.

**Frontend shows "Failed to load patients"** — confirm the backend is running and `app.cors.allowed-origins` in `application.yml` includes `http://localhost:5173`.

**Simulator rejects all devices** — ensure the device codes in `DEVICE_CODES` are registered in the backend via `POST /api/v1/devices` before starting the simulator.

**Maven dependencies not loading** — run `.\mvnw.cmd clean install`.