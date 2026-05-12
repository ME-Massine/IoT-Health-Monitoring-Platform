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
                                        │
                                   PostgreSQL (5432)
```

| Layer | Technology | Port |
| --- | --- | --- |
| Frontend (UI) | React 19 + Vite | 5173 |
| Backend (API) | Spring Boot 3.5 · Java 17 | 8080 |
| Database | PostgreSQL | 5432 |
| Simulator | Python 3.12 | — (client only) |

---

## Layer-by-Layer Breakdown

### 1. Simulator — `simulator/`

**Role:** Pretends to be physical IoT devices attached to patients.

- Sends vital sign readings via `HTTP POST /api/v1/vitals` every few seconds
- Vitals **drift realistically** — they don't jump randomly, they gradually move toward normal ranges, mimicking real physiology
- Configurable **anomaly injection rate** (e.g. 10%) that fires one of 8 pre-defined scenarios: `high_hr_critical`, `spo2_warning`, `fever`, etc.
- Validates device codes against the API on startup and skips any that don't exist in the DB

> [!tip] Why Python?
> Fast to prototype, minimal boilerplate, and the `requests` library makes HTTP calls trivial. No need for Java or Node here — the simulator is purely a data producer.

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

#### Ingestion Pipeline (per reading)

```
POST /api/v1/vitals
  └─► VitalSignService.ingestVitalSign()
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

- **`Patient`** — name, room number
- **`Device`** — string device code, assigned patient (FK)
- **`VitalSign`** — HR, temp, SpO2, timestamp, device (FK), patient (FK)
- **`Alert`** — severity (`WARNING` / `CRITICAL`), type, message, resolved flag, patient (FK)

> [!note] Performance
> All relationships use `FetchType.LAZY` — data is only loaded from the DB when explicitly accessed, avoiding N+1 query problems. `VitalSign` and `Alert` tables carry composite indices for efficient time-range queries.

#### WebSocket Topics

| Topic | Content |
| --- | --- |
| `/topic/vitals` | Every new vital reading (all patients) |
| `/topic/patients/{id}/vitals` | Patient-scoped vitals |
| `/topic/alerts` | Every new alert |
| `/topic/patients/{id}/alerts` | Patient-scoped alerts |

---

### 3. Database — PostgreSQL

- Schema **auto-managed by Hibernate** (`ddl-auto: update`) — tables are created/updated on startup, no SQL migration scripts needed
- Stores the **full history** of all vital readings and alerts (including resolved ones)
- Composite indices on `VitalSign` and `Alert` for efficient paginated history queries

---

### 4. Frontend — `frontend/`

**Role:** Clinical dashboard with three pages, all updated live via WebSocket.

#### Pages

##### Dashboard `/`
- **KPI strip** — total patients · critical count · warning count · devices online
- **Patient cards** grid — current HR, Temp, SpO2 with color-coded left border
  - 🔴 Red = CRITICAL · 🟠 Orange = WARNING · 🟢 Green = STABLE
- **Filter tabs** — All / Critical / Warning / Stable (critical cards sort to top)
- Cards update **live** without refresh

##### Patient Detail `/patients/:id`
- **3 metric cards** — current value + status badge (Normal / High / Low) per vital
- **Line charts** (Recharts) — one per vital, showing recent history with reference lines at warning/critical thresholds
- **Alert timeline** — vertical timeline of all alerts for that patient, color-coded by severity

##### Alert Center `/alerts`
- **Summary bar** — critical count · warning count · resolved count
- **Filter tabs** — Unresolved / Critical / Warnings / All
- **Alert list** — severity badge, type, message, patient link, timestamp, Resolve button
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
- Resolve button click → `CustomEvent("alert-resolved")` → decrement immediately
- 30 s polling → drift correction (handles external resolves)

---

## End-to-End Data Flow

```
1.  Simulator sends:
      POST /api/v1/vitals
      { deviceCode, heartRate, temperature, spo2 }

2.  VitalSignController → VitalSignService.ingestVitalSign()

3.  VitalSignService:
      a. Lookup device by code → resolve patient
      b. Persist VitalSign to PostgreSQL
      c. AlertService: check thresholds → persist Alert if violated
      d. Publish WebSocket events (vital + alert if any)

4.  React frontend (STOMP connected):
      usePatients()      → patches patient card live
      useAlerts()        → prepends alert to list
      AppLayout          → increments sidebar badge
```

---

## Key Design Decisions

> [!question] Why WebSocket instead of polling?
> Polling (e.g. every 5 s) wastes bandwidth and adds latency. WebSocket keeps a persistent TCP connection — the server **pushes** data the instant it's available. Critical for a medical monitoring context where seconds matter.

> [!question] Why STOMP over raw WebSocket?
> Raw WebSocket is just a byte pipe — you'd have to invent your own message protocol. STOMP gives **pub/sub semantics** with named topics, which maps naturally to "subscribe to this patient's vitals." Spring has first-class STOMP support via `spring-boot-starter-websocket`.

> [!question] Why a separate simulator instead of hardcoded seed data?
> Realistic demo and testability. The simulator generates physiologically plausible drift with **controlled anomaly injection**. This lets you demo the alert system triggering in real time without physical hardware.

> [!question] Why PostgreSQL and not a time-series DB like InfluxDB?
> InfluxDB is more optimal for pure time-series queries, but this system also has relational data (patients, devices, alerts with foreign keys). PostgreSQL covers both with proper indexing, without adding operational complexity for a demo/small deployment.

> [!question] How does the frontend know thresholds without calling the backend?
> `AlertService` thresholds are **mirrored** in `frontend/src/utils/vitalStatus.js`. Both backend and frontend use the same numeric boundaries, so the UI color-coding always matches what actually generated an alert.

> [!question] What happens if the backend goes down?
> The frontend displays an error state caught inside the hooks' `try/catch` blocks. The STOMP client (`@stomp/stompjs`) has built-in reconnect logic and will re-subscribe automatically when the backend comes back.

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

# Terminal 3 — Simulator
cd simulator
.\venv\Scripts\Activate.ps1
python simulator.py
```

Open **http://localhost:5173** in the browser.

> [!tip] First-time setup
> Copy `backend/src/main/resources/application.example.yml` → `application.yml` and fill in your PostgreSQL password.
> Copy `frontend/.env.example` → `.env.local` (defaults work as-is for local dev).

---

## REST API Reference

Base path: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/vitals` | Ingest a vital sign reading |
| `GET` | `/vitals/patient/{id}/history` | Paginated history (`from`, `to`, `limit` params; max 500) |
| `GET` | `/patients` | List all patients |
| `GET` | `/alerts` | List all alerts |
| `GET` | `/alerts/unresolved` | Unresolved alerts only |
| `PUT` | `/alerts/{id}/resolve` | Mark alert resolved |
| `GET` | `/devices` | List all registered devices |
| `GET` | `/devices/code/{code}` | Look up device by string code |

---

## File Map (Key Files Only)

```
backend/src/main/java/com/iothealth/backend/
├── controller/          REST endpoints
├── service/
│   ├── VitalSignService.java    ingestion + WS publish
│   └── AlertService.java        threshold evaluation
├── entity/              JPA entities (Patient, Device, VitalSign, Alert)
├── websocket/           WS publishers
└── mqtt/                optional MQTT ingestion path

frontend/src/
├── api/                 Axios clients (patientApi, alertApi, deviceApi, wsClient)
├── hooks/               usePatients, useAlerts, usePatientDetail, useGlobalAlertsSocket
├── pages/               DashboardPage, PatientDetailPage, AlertCenterPage
├── components/
│   ├── layout/          AppLayout (sidebar + nav badge)
│   ├── dashboard/       PatientCard, KpiStrip
│   └── detail/          VitalCard, VitalChart, PatientAlerts
├── utils/vitalStatus.js threshold helpers (mirrors AlertService)
└── styles/global.css    all styling

simulator/
└── simulator.py         device simulator with anomaly injection
```

---

#iot #spring-boot #react #websocket #postgresql #health-monitoring #handoff
