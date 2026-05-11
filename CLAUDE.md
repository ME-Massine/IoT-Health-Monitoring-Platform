# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IoT Health Monitoring Platform — a full-stack system that simulates connected medical devices sending real-time vital signs (heart rate, temperature, SpO2) to a Spring Boot backend. Abnormal readings trigger alerts that are pushed live to a React frontend via STOMP/WebSocket. No physical hardware required; a Python simulator drives the entire data flow.

## Architecture

```
Python Simulator ──HTTP POST──► Spring Boot (8080) ◄──REST + WS──► React Frontend (5173)
Python MQTT pub  ──MQTT pub──►  Mosquitto (1883)                        │
                                        │  ◄──MQTT sub──────────────────┘ (optional)
                                   PostgreSQL (5432)
```

**Services:**
- `backend/` — Java 17, Spring Boot 3.5, Spring Data JPA, Spring WebSocket (STOMP over SockJS)
- `frontend/` — React 19, Vite, React Router, Axios, @stomp/stompjs
- `simulator/` — Python 3.12 script that generates realistic vital drift with configurable anomaly injection
- `mosquitto/` — optional Mosquitto broker config for MQTT ingestion (see `docker-compose.mqtt.yml`)

**WebSocket topics published by the backend:**
- `/topic/vitals` — all incoming vital sign readings
- `/topic/patients/{id}/vitals` — patient-scoped vitals
- `/topic/alerts` — all new alerts
- `/topic/patients/{id}/alerts` — patient-scoped alerts

## Commands

### Backend (Java/Maven)
```powershell
cd backend
.\mvnw.cmd spring-boot:run          # start API on :8080
.\mvnw.cmd test                     # run all unit tests
.\mvnw.cmd test -Dtest=AlertServiceTest   # run a single test class
.\mvnw.cmd package -DskipTests      # build jar
```

### Frontend (Node/Vite)
```powershell
cd frontend
npm install
npm run dev        # dev server on :5173
npm run build      # production build
npm run lint       # ESLint
npm run preview    # preview production build
```

### Simulator (Python)
```powershell
cd simulator
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env               # then edit device codes / API URL
python simulator.py
```

### MQTT (optional)
```powershell
# Start Mosquitto broker via Docker
docker-compose -f docker-compose.mqtt.yml up -d

# Publish one batch of test readings
cd simulator && python mqtt_publisher.py

# Publish continuously with anomalies
python mqtt_publisher.py --loop --anomaly
```

## Configuration

### Backend
Copy `backend/src/main/resources/application.example.yml` → `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/iot_health_db
    username: postgres
    password: YOUR_PASSWORD
app:
  cors.allowed-origins: http://localhost:5173
  websocket.allowed-origins: http://localhost:5173
```
Hibernate manages schema automatically (`ddl-auto: update`).

### Frontend
Copy `frontend/.env.example` → `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=http://localhost:8080/ws
```

### Simulator
Edit `simulator/.env`:
```
API_BASE_URL=http://localhost:8080/api/v1
DEVICE_CODES=DEV-001,DEV-002       # must exist in DB
INTERVAL_SECONDS=5
ANOMALY_RATE=0.10                  # 0.0–1.0
# MQTT publisher also reads these, plus:
MQTT_BROKER_HOST=localhost
MQTT_BROKER_PORT=1883
```

### MQTT (backend)
Add to `application.yml` to enable:
```yaml
app:
  mqtt:
    enabled: true
    broker-url: tcp://localhost:1883
    client-id: iot-health-backend
    topic-pattern: iot-health/devices/+/vitals
    qos: 1
```
When `enabled: false` (the default), nothing MQTT-related starts and no broker is needed.

## Key Packages / Patterns

### Backend (`backend/src/main/java/com/iothealth/backend/`)
- **`controller/`** → thin REST layer, delegates to services
- **`service/`** → business logic; `VitalSignService` ingests readings, calls `AlertService`, then fires WebSocket events
- **`entity/`** → JPA entities; `VitalSign`, `Alert` (with 5 composite indices), `Device`, `Patient`; all relationships are `FetchType.LAZY`
- **`websocket/`** → `VitalSignWebSocketPublisher`, `AlertWebSocketPublisher` — called from services, not controllers
- **`dto/` + `mapper/`** → separate request/response shapes from entities

Alert thresholds (in `AlertService`):

| Vital | Warning | Critical |
|-------|---------|----------|
| Heart rate | ≥ 110 bpm | < 50 or > 120 bpm |
| Temperature | ≥ 37.8 °C | < 35.0 or > 38.0 °C |
| SpO2 | ≤ 94% | ≤ 92% |

### Frontend (`frontend/src/`)
- **`api/`** — Axios instance (`httpClient.js`) + domain clients; STOMP factory (`wsClient.js`)
- **`hooks/`** — custom hooks combine initial fetch with WebSocket subscription; e.g. `usePatients()` fetches the patient list then keeps vitals live via `/topic/vitals`
- **`pages/`** — `DashboardPage`, `PatientDetailPage`, `AlertCenterPage`

### Simulator (`simulator/simulator.py`)
Each device tracks independent vital state that drifts toward a normal range. At each tick there is an `ANOMALY_RATE` chance of injecting one of 8 pre-defined anomaly scenarios (e.g., `high_hr_critical`, `spo2_warning`). Validates all device codes against the API on startup and skips any that don't exist.

### MQTT ingestion (`backend/src/main/java/com/iothealth/backend/mqtt/`)
Four classes implement the optional second ingestion path:
- `MqttProperties` — `@ConfigurationProperties(prefix = "app.mqtt")` record
- `MqttConfig` — `@ConditionalOnProperty(enabled=true)` activates the whole subsystem
- `MqttPayload` — JSON deserialization record for incoming MQTT messages
- `MqttVitalSignListener` — `MqttCallback` that connects on `@PostConstruct`, subscribes to `topicPattern`, parses each message into a `VitalSignRequest`, and calls `VitalSignService.ingestVitalSign()` — identical to the HTTP path

Topic convention: `iot-health/devices/{deviceCode}/vitals` (`+` wildcard in subscription).

## REST API Base Path

All REST endpoints are under `/api/v1`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Key endpoints:
- `POST /vitals` — ingest a vital sign reading (used by simulator)
- `GET /vitals/patient/{id}/history?from=&to=&limit=` — paginated history (max 500)
- `PUT /alerts/{id}/resolve` — mark alert resolved
- `GET /devices/code/{code}` — look up device by its string code

## Database Setup

```sql
CREATE DATABASE iot_health_db;
```

Hibernate auto-creates all tables on first run.

## Docs

- `docs/setup-guide.md` — step-by-step local setup and troubleshooting
- `docs/websocket-topics.md` — WebSocket subscription reference
- `docs/demo-scenario.md` — presentation walkthrough script
- `docs/mqtt-guide.md` — MQTT broker setup, publisher usage, payload reference
