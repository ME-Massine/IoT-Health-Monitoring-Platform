---
title: IoT Health Monitoring Platform — Project Handoff
date: 2026-05-12
tags:
  - iot
  - spring-boot
  - react
  - websocket
  - health-monitoring
  - handoff
status: complete
---

# IoT Health Monitoring Platform

> [!abstract] What This Project Does
> A **real-time health monitoring system** that simulates connected medical IoT devices sending patient vital signs (heart rate, temperature, SpO2) to a central platform. The system continuously monitors incoming readings, automatically detects dangerous values, fires clinical alerts, and pushes everything live to a web dashboard — no page refresh required.

---

## Architecture

```
Python Simulator ──HTTP POST──► Spring Boot (8080) ◄──REST + WebSocket──► React Frontend (5173)
Python MQTT pub  ──MQTT pub──►  MQTT Broker (1883)         │
                                        │  ◄──MQTT sub─────┘
                                   PostgreSQL (5432)
```

| Layer | Technology | Port |
| --- | --- | --- |
| Frontend (UI) | React 19 + Vite | 5173 |
| Backend (API) | Spring Boot 3.5 · Java 17 | 8080 |
| Database | PostgreSQL | 5432 |
| Simulator | Python 3.12 | — (client only) |
| MQTT Broker | HiveMQ public / local Mosquitto | 1883 |

---

## Layer-by-Layer Breakdown

### 1. Simulator — `simulator/`

**Role:** Pretends to be physical IoT devices attached to patients.

- Sends vital sign readings via `HTTP POST /api/v1/vitals` every few seconds
- Vitals **drift realistically** — they don't jump randomly, they gradually move toward normal ranges, mimicking real physiology
- Configurable **anomaly injection rate** (e.g. 10%) that fires one of 8 pre-defined scenarios: `high_hr_critical`, `spo2_warning`, `fever`, etc.
- Validates device codes against the API on startup and skips any that don't exist in the DB
- Includes a separate `mqtt_publisher.py` script that publishes readings to an MQTT broker instead of HTTP

> [!tip] Why Python?
> Fast to prototype, minimal boilerplate, and the `requests` and `paho-mqtt` libraries make HTTP/MQTT trivial. The simulator is purely a data producer.

---

### 2. Backend — `backend/`

**Role:** The brain of the system. Handles ingestion, business logic, persistence, REST API, and real-time push.

#### Package Structure

| Package | Responsibility |
| --- | --- |
| `controller/` | Thin REST layer — receives requests, delegates to services immediately |
| `service/` | Business logic — ingestion pipeline, alert evaluation |
| `entity/` | JPA entities mapped to PostgreSQL tables |
| `websocket/` | Publishers that push events to connected frontend clients |
| `dto/` + `mapper/` | Separates API shapes from internal entities |
| `mqtt/` | Optional MQTT ingestion path (conditional on `app.mqtt.enabled`) |

#### Ingestion Pipeline (per reading)

Two ingestion paths exist — HTTP and MQTT — but they converge at the same service method:

```
HTTP path:
  POST /api/v1/vitals
    └─► VitalSignService.ingestVitalSign()

MQTT path:
  MQTT broker topic: iot-health/devices/{deviceCode}/vitals
    └─► MqttVitalSignListener.messageArrived()
          └─► VitalSignService.ingestVitalSign()   ← same pipeline

VitalSignService.ingestVitalSign():
  ├─ Resolve device code → find patient
  ├─ Save VitalSign to PostgreSQL
  ├─ Call AlertService → evaluate thresholds → save Alert if violated
  └─ Publish via WebSocket:
        /topic/vitals
        /topic/patients/{id}/vitals
        /topic/alerts          (if alert created)
        /topic/patients/{id}/alerts
```

#### Alert Thresholds

> [!warning] Clinical Logic (mirrored in frontend `vitalStatus.js`)

| Vital | Warning | Critical |
| --- | --- | --- |
| Heart Rate | ≥ 110 bpm | < 50 or > 120 bpm |
| Temperature | ≥ 37.8 °C | < 35.0 or > 38.0 °C |
| SpO2 | ≤ 94 % | ≤ 92 % |

#### Entities

- **`Patient`** — first name, last name, age, gender, room number, medical condition
- **`Device`** — string device code, assigned patient (FK), status, type
- **`VitalSign`** — HR, temp, SpO2, timestamp, device (FK), patient (FK)
- **`Alert`** — severity (`WARNING` / `CRITICAL`), type, message, resolved flag, `resolvedAt`, `acknowledgedAt`, patient (FK)

> [!note] Alert lifecycle
> An alert now has three states beyond creation:
> - **Acknowledged** — a clinician has seen it (`PATCH /alerts/{id}/acknowledge` sets `acknowledgedAt`)
> - **Resolved** — the condition is cleared (`PUT /alerts/{id}/resolve` sets `resolvedAt`, `resolved = true`)
> - **Dismissed** — a false positive or noise, removed from the system (`DELETE /alerts/{id}`)

> [!note] Performance
> All relationships use `FetchType.LAZY` — data is only loaded from the DB when explicitly accessed, avoiding N+1 query problems. `VitalSign` and `Alert` tables carry composite indices for efficient time-range queries.

#### WebSocket Topics

| Topic | Content |
| --- | --- |
| `/topic/vitals` | Every new vital reading (all patients) |
| `/topic/patients/{id}/vitals` | Patient-scoped vitals |
| `/topic/alerts` | Every new alert |
| `/topic/patients/{id}/alerts` | Patient-scoped alerts |

#### MQTT Ingestion (`mqtt/`)

Four classes implement the optional second ingestion path:

| Class | Role |
| --- | --- |
| `MqttProperties` | `@ConfigurationProperties(prefix = "app.mqtt")` — broker URL, client ID, topic pattern, QoS |
| `MqttConfig` | `@ConditionalOnProperty(enabled=true)` — activates the whole subsystem |
| `MqttPayload` | JSON deserialization record for incoming MQTT messages |
| `MqttVitalSignListener` | Connects on `@PostConstruct`, subscribes, parses each message into a `VitalSignRequest`, calls `VitalSignService.ingestVitalSign()` |

Topic convention: `iot-health/devices/{deviceCode}/vitals` (`+` wildcard in subscription).

Enable in `application.yml`:
```yaml
app:
  mqtt:
    enabled: true
    broker-url: tcp://broker.hivemq.com:1883
    client-id: iot-health-backend
    topic-pattern: iot-health/devices/+/vitals
    qos: 1
```

To see MQTT activity in the logs, set:
```yaml
logging:
  level:
    com.iothealth.backend.mqtt: DEBUG
```

---

### 3. Database — PostgreSQL

- Schema **auto-managed by Hibernate** (`ddl-auto: update`) — tables are created/updated on startup, no SQL migration scripts needed
- Stores the **full history** of all vital readings and alerts (including resolved and dismissed ones are purged via delete)
- Composite indices on `VitalSign` and `Alert` for efficient paginated history queries
- SQL logging is disabled (`show-sql: false`) to keep the console clean during normal operation

---

### 4. Frontend — `frontend/`

**Role:** Clinical dashboard with three pages, all updated live via WebSocket.

#### Pages

##### Dashboard `/`
- **KPI strip** — total patients · critical count · warning count · devices online · **24 h alert trend sparkline** (area chart, warning + critical series)
- **Patient cards** grid — current HR, Temp, SpO2 with color-coded left border
  - 🔴 Red = CRITICAL · 🟠 Orange = WARNING · 🟢 Green = STABLE
- **Filter tabs** — All / Critical / Warning / Stable (critical cards sort to top)
- **Add Patient button** — opens the patient creation modal
- **Edit icon per card** — hover to reveal a pencil button that opens the patient edit/delete modal
- Cards update **live** without refresh

##### Patient Detail `/patients/:id`
- **3 metric cards** — current value + status badge (Normal / High / Low) per vital, plus a **delta indicator** (▲ red / ▼ blue / ↔ grey) showing the change from the previous reading
- **Time range picker** — Live · 1 h · 6 h · 24 h presets above the charts; selecting a preset re-fetches the full history for that window
- **Line charts** (Recharts) — one per vital, showing history with reference lines at warning/critical thresholds; maintenance windows are shaded
- **Alert timeline** — vertical timeline of all alerts for that patient, color-coded by severity

##### Alert Center `/alerts`
- **Summary bar** — critical count · warning count · resolved count
- **Filter tabs** — Unresolved / Critical / Warnings / All
- **Alert list** — severity badge, type, message, patient link, timestamp
- **Per-alert action buttons:**
  - **Acknowledge** (eye icon) — marks the alert as seen; the "Acknowledged" tag appears; button hides after use
  - **Resolve** (check icon) — closes the alert; hidden after resolved
  - **Dismiss** (trash icon) — permanently removes the alert (false positives / noise)
- New alerts appear at the top **instantly** via WebSocket

#### Real-Time Architecture (Frontend)

Each page uses a custom React hook that does two things on mount:

1. **Initial fetch** — REST API call to populate state
2. **WebSocket subscription** — STOMP subscription that patches state when new data arrives

```
usePatients()       → GET /patients  +  subscribe /topic/vitals
useAlerts()         → GET /alerts    +  subscribe /topic/alerts
usePatientDetail()  → GET /vitals/patient/{id}/history  +  subscribe /topic/patients/{id}/vitals
                                                         +  subscribe /topic/patients/{id}/alerts
```

The **sidebar alert badge** updates via three paths:
- WebSocket new-alert event → increment immediately
- Resolve/Dismiss button click → `CustomEvent("alert-resolved")` → decrement immediately
- 30 s polling → drift correction (handles external resolves)

#### Global Error Handling

An Axios response interceptor in `httpClient.js` catches all non-404 API errors and dispatches a `CustomEvent("api-error", { detail: message })`. `AppLayout` listens for this event and pushes an `ERROR` toast, giving the user feedback for any failed backend call without needing per-component error handling.

#### Patient CRUD

A `PatientForm.jsx` modal handles create, edit, and delete:
- **Create** — "Add Patient" button in the dashboard header opens a blank form
- **Edit** — pencil icon on each patient card opens the form pre-filled
- **Delete** — two-step confirmation inside the edit modal (click once → "Confirm delete?" → click again)
- On success the hook's `addPatient` / `updatePatient` / `removePatient` functions update state optimistically without a full re-fetch

---

## End-to-End Data Flow

```
HTTP path:
  1.  Simulator sends:
        POST /api/v1/vitals
        { deviceCode, heartRate, temperature, spo2 }

MQTT path:
  1.  mqtt_publisher.py publishes:
        topic: iot-health/devices/{deviceCode}/vitals
        payload: { deviceCode, heartRate, temperature, spo2, recordedAt }

Both paths converge at:
  2.  VitalSignService.ingestVitalSign()
        a. Lookup device by code → resolve patient
        b. Persist VitalSign to PostgreSQL
        c. AlertService: check thresholds → persist Alert if violated
        d. Publish WebSocket events (vital + alert if any)

  3.  React frontend (STOMP connected):
        usePatients()      → patches patient card live (vital values + delta)
        useAlerts()        → prepends alert to list
        AppLayout          → increments sidebar badge
```

---

## Key Design Decisions

> [!question] Why WebSocket instead of polling?
> Polling (e.g. every 5 s) wastes bandwidth and adds latency. WebSocket keeps a persistent TCP connection — the server **pushes** data the instant it's available. Critical for a medical monitoring context where seconds matter.

> [!question] Why STOMP over raw WebSocket?
> Raw WebSocket is just a byte pipe — you'd have to invent your own message protocol. STOMP gives **pub/sub semantics** with named topics, which maps naturally to "subscribe to this patient's vitals." Spring has first-class STOMP support via `spring-boot-starter-websocket`.

> [!question] Why two ingestion paths (HTTP and MQTT)?
> HTTP is simpler and works everywhere. MQTT is the standard protocol for real IoT hardware — it's lightweight, designed for unreliable networks, and supports QoS guarantees. Both paths run through the same `VitalSignService`, so the business logic is never duplicated.

> [!question] Why a separate simulator instead of hardcoded seed data?
> Realistic demo and testability. The simulator generates physiologically plausible drift with **controlled anomaly injection**. This lets you demo the alert system triggering in real time without physical hardware.

> [!question] Why PostgreSQL and not a time-series DB like InfluxDB?
> InfluxDB is more optimal for pure time-series queries, but this system also has relational data (patients, devices, alerts with foreign keys). PostgreSQL covers both with proper indexing, without adding operational complexity for a demo/small deployment.

> [!question] How does the frontend know thresholds without calling the backend?
> `AlertService` thresholds are **mirrored** in `frontend/src/utils/vitalStatus.js`. Both backend and frontend use the same numeric boundaries, so the UI color-coding always matches what actually generated an alert.

> [!question] What happens if the backend goes down?
> The frontend displays an error state caught inside the hooks' `try/catch` blocks. The STOMP client (`@stomp/stompjs`) has built-in reconnect logic and will re-subscribe automatically when the backend comes back.

> [!question] Why is CORS configured with PATCH in allowedMethods?
> The alert acknowledge endpoint uses `PATCH`. Browsers send a preflight `OPTIONS` request before any non-simple method. If `PATCH` is absent from `allowedMethods` in `WebConfig.java`, the preflight fails silently and the request never reaches the backend. Always include every HTTP method your API uses.

---

## What This Is NOT (Intentional Scope Limits)

> [!warning] Not Production-Ready — by Design

| Missing | Why acceptable for demo |
| --- | --- |
| Authentication / JWT | Demo only — no user accounts needed |
| HTTPS / TLS | Local network only |
| Alert deduplication | A persistent high HR would generate one alert per reading — acceptable for demo volume |
| Horizontal scaling | Single backend instance |
| Audit logs | Out of scope |
| Role-based access | Single operator view |

A production clinical system would require all of the above plus compliance (HIPAA/GDPR), redundancy, and formal device certification.

---

## How to Run Locally

```powershell
# Terminal 1 — Backend
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2 — Frontend
cd frontend
npm install
npm run dev

# Terminal 3 — HTTP Simulator
cd simulator
.\venv\Scripts\Activate.ps1
python simulator.py

# Terminal 4 (optional) — MQTT Publisher
cd simulator
.\venv\Scripts\Activate.ps1
python mqtt_publisher.py --loop --anomaly
```

Open **http://localhost:5173** in the browser.

> [!tip] First-time setup
> Copy `backend/src/main/resources/application.example.yml` → `application.yml` and fill in your PostgreSQL password.
> Copy `frontend/.env.example` → `.env.local` (defaults work as-is for local dev).
> Install simulator dependencies: `pip install -r requirements.txt` (includes `paho-mqtt`).

> [!tip] MQTT without Docker
> The project is pre-configured to use the free public HiveMQ broker (`broker.hivemq.com:1883`). No local broker installation needed. Set `app.mqtt.enabled: true` in `application.yml` and restart the backend.

---

## REST API Reference

Base path: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Vital Signs

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/vitals` | Ingest a vital sign reading |
| `GET` | `/vitals/patient/{id}/history` | Paginated history (`from`, `to`, `limit` params; max 500) |

### Patients

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/patients` | List all patients |
| `GET` | `/patients/{id}` | Get a single patient |
| `POST` | `/patients` | Create a new patient |
| `PUT` | `/patients/{id}` | Update a patient |
| `DELETE` | `/patients/{id}` | Delete a patient |

### Alerts

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/alerts` | List all alerts (ordered by creation date desc) |
| `GET` | `/alerts/unresolved` | Unresolved alerts only |
| `GET` | `/alerts/patient/{patientId}` | All alerts for a specific patient |
| `GET` | `/alerts/summary?from=&to=` | Hourly alert counts (critical + warning) between two timestamps |
| `PUT` | `/alerts/{id}/resolve` | Mark alert resolved |
| `PATCH` | `/alerts/{id}/acknowledge` | Acknowledge an alert (sets `acknowledgedAt`) |
| `DELETE` | `/alerts/{id}` | Dismiss (permanently delete) an alert |

### Devices

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/devices` | List all registered devices |
| `GET` | `/devices/code/{code}` | Look up device by string code |

---

## File Map (Key Files Only)

```
backend/src/main/java/com/iothealth/backend/
├── controller/
│   ├── AlertController.java       resolve, acknowledge, dismiss, summary
│   ├── PatientController.java     full CRUD
│   └── VitalSignController.java   ingestion + history
├── service/
│   ├── VitalSignService.java      ingestion + WS publish
│   └── AlertService.java          threshold evaluation + lifecycle methods
├── entity/
│   ├── Alert.java                 includes acknowledgedAt field + acknowledge()
│   ├── Patient.java
│   ├── Device.java
│   └── VitalSign.java
├── dto/alert/
│   ├── AlertResponse.java         includes acknowledgedAt
│   └── AlertSummaryPoint.java     hourly bucket (hour, critical, warning)
├── config/
│   └── WebConfig.java             CORS — includes PATCH in allowedMethods
├── websocket/                     WS publishers
└── mqtt/
    ├── MqttConfig.java            conditional bean activation
    ├── MqttProperties.java        broker URL, client ID, topic, QoS
    ├── MqttPayload.java           JSON deserialization record
    └── MqttVitalSignListener.java connect → subscribe → ingest

frontend/src/
├── api/
│   ├── httpClient.js              Axios instance + error interceptor (CustomEvent)
│   ├── patientApi.js              getAll, getById, create, update, delete
│   ├── alertApi.js                getAll, resolve, acknowledge, dismiss, getSummary
│   ├── vitalSignApi.js            getHistoryByPatientId (supports from/to/limit)
│   └── wsClient.js                STOMP factory
├── hooks/
│   ├── usePatients.js             fetch + WS + addPatient/updatePatient/removePatient
│   ├── useAlerts.js               fetch + WS + handleAlertUpdated/handleAlertDismissed
│   └── usePatientDetail.js        fetch(rangeHours) + WS vitals + WS alerts
├── pages/
│   ├── DashboardPage.jsx          patient grid + Add Patient button + PatientForm
│   ├── PatientDetailPage.jsx      vitals + range picker + charts + alert timeline
│   └── AlertCenterPage.jsx        alert list + acknowledge/resolve/dismiss actions
├── components/
│   ├── layout/
│   │   ├── AppLayout.jsx          sidebar + api-error event → toast
│   │   └── ToastContainer.jsx     ERROR / CRITICAL / WARNING / INFO severities
│   ├── dashboard/
│   │   ├── PatientCard.jsx        edit button on hover
│   │   └── KpiStrip.jsx           4 KPI cards + AlertSparkline (24 h area chart)
│   ├── detail/
│   │   ├── VitalCard.jsx          value + delta indicator (▲/▼/↔)
│   │   ├── VitalChart.jsx         Recharts line chart + ref lines + maintenance windows
│   │   └── PatientAlerts.jsx      alert timeline
│   └── forms/
│       └── PatientForm.jsx        create/edit/delete modal with validation
├── utils/vitalStatus.js           threshold helpers (mirrors AlertService)
└── styles/global.css              all styling

simulator/
├── simulator.py                   HTTP device simulator with anomaly injection
└── mqtt_publisher.py              MQTT publisher (single batch or --loop --anomaly)
```

---

#iot #spring-boot #react #websocket #postgresql #mqtt #health-monitoring #handoff
