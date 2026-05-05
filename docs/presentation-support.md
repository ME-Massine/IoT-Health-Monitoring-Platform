# Presentation Support — IoT Health Monitoring Platform

This document is the reference material for the final project defense.
Use it to build slides, practice the oral presentation, or answer examiner questions.

---

## 1. Project Pitch (30 seconds)

> "We built a full-stack IoT health monitoring platform that simulates connected
> medical devices sending patient vital signs in real time. The backend detects
> abnormal readings and generates alerts automatically. A live React dashboard
> shows patient data, vital sign history, and alerts — all updating instantly
> via WebSocket without any page refresh. The whole system runs end-to-end
> with no physical hardware."

---

## 2. Problem Statement

Hospitals and care facilities need continuous monitoring of patient vital signs.
Manual checks are infrequent and miss sudden deteriorations. Connected IoT
devices can stream data continuously, but this creates new challenges:

- How do you ingest high-frequency sensor data reliably?
- How do you detect abnormal readings automatically and immediately?
- How do you deliver those alerts to medical staff in real time?
- How do you visualize live data without constant page refreshes?

This project addresses all four challenges in a single integrated system.

---

## 3. Project Goal

Build a demonstrable full-stack IoT health monitoring platform that:

- Accepts real-time vital sign data from simulated IoT devices
- Detects threshold violations and generates health alerts automatically
- Broadcasts live updates to a web dashboard via WebSocket
- Provides a clean UI for monitoring patients, reviewing vitals history,
  and resolving alerts
- Requires no physical hardware — a Python simulator drives the full flow

---

## 4. Architecture

```
┌─────────────────┐     POST /api/v1/vitals     ┌──────────────────────────┐
│ Python Simulator│ ──────────────────────────► │  Spring Boot Backend     │
│                 │                             │                          │
│ - Drift model   │                             │  - REST API              │
│ - Anomaly inject│                             │  - Alert detection       │
│ - Multi-device  │                             │  - WebSocket broker      │
└─────────────────┘                             └────────────┬─────────────┘
                                                             │  WS + REST
                                                             ▼
┌─────────────────┐                             ┌──────────────────────────┐
│ React Frontend  │ ◄───────────────────────────│  PostgreSQL              │
│                 │                             │                          │
│ - Dashboard     │                             │  patients / devices      │
│ - Detail page   │                             │  vital_signs / alerts    │
│ - Alert center  │                             └──────────────────────────┘
└─────────────────┘
```

**Talking points:**
- The simulator and backend are fully decoupled — they communicate only through
  the REST ingestion endpoint. Any real device that can send an HTTP POST could
  replace the simulator with zero backend changes.
- The backend is the single source of truth. The frontend is purely a consumer.
- WebSocket removes polling entirely. The frontend subscribes to topics and
  receives pushes the moment a vital sign or alert is created.

---

## 5. Main Features

| Feature | Description |
|---|---|
| Patient management | Create, update, and delete patient records |
| Device assignment | Each device is assigned to exactly one patient |
| Vital sign ingestion | `POST /api/v1/vitals` accepts readings and triggers alert detection |
| Alert detection | Automatic threshold evaluation for HR, temperature, SpO2 |
| Real-time WebSocket | STOMP topics push vitals and alerts live to the frontend |
| Patient dashboard | Cards showing all patients with latest vitals |
| Patient detail page | Vitals history table and per-patient alerts with resolve action |
| Alert center | Global unresolved/all alert view with filter toggle |
| Simulator | Python script with drift model, anomaly injection, multi-device support |
| Swagger UI | Full REST API documentation at `/swagger-ui/index.html` |
| Tests | Service-layer unit tests and controller slice tests |

---

## 6. Backend Modules

| Module | Responsibility |
|---|---|
| `controller` | REST endpoints — delegates to services, no business logic |
| `service` | Business logic — patient/device CRUD, vital ingestion, alert detection |
| `repository` | Spring Data JPA interfaces for database access |
| `entity` | JPA entities — Patient, Device, VitalSign, Alert |
| `dto` | Request/response records — decouples API contract from entities |
| `mapper` | Converts between entities and DTOs |
| `exception` | `ResourceNotFoundException`, `BadRequestException`, global handler |
| `websocket` | `VitalSignWebSocketPublisher`, `AlertWebSocketPublisher` |
| `config` | `WebSocketConfig` (STOMP), `WebConfig` (CORS), `OpenApiConfig` (Swagger) |

**Alert detection thresholds (AlertService.java):**

| Vital | Warning | Critical |
|---|---|---|
| Heart rate | ≥ 110 bpm | < 50 or > 120 bpm |
| Temperature | ≥ 37.8 °C | < 35.0 or > 38.0 °C |
| SpO2 | ≤ 94 % | ≤ 92 % |

**Talking points:**
- Controllers are thin by design. All logic lives in services, making it easy
  to test without loading the full HTTP stack.
- Alert detection is synchronous and runs inside the same transaction as
  vital sign ingestion, so a vital is never saved without its alerts.
- DTOs use Java records — immutable, concise, no boilerplate.

---

## 7. Frontend Modules

| Module | Responsibility |
|---|---|
| `api/httpClient.js` | Axios instance with base URL from env |
| `api/patientApi.js` | `getAll`, `getById` |
| `api/vitalSignApi.js` | `getLatestByPatientId`, `getHistoryByPatientId` |
| `api/alertApi.js` | `getAll`, `getUnresolved`, `getByPatientId`, `resolve` |
| `api/wsClient.js` | STOMP client factory over SockJS |
| `hooks/usePatients` | Fetches all patients + latest vitals, live-patches via WS |
| `hooks/usePatientDetail` | Fetches patient + vitals history + alerts, live via WS |
| `hooks/useAlerts` | Fetches alerts with filter state, live via WS |
| `hooks/usePatientVitalsSocket` | Subscribes to `/topic/patients/{id}/vitals` |
| `hooks/usePatientAlertsSocket` | Subscribes to `/topic/patients/{id}/alerts` |
| `hooks/useGlobalAlertsSocket` | Subscribes to `/topic/alerts` |
| `pages/DashboardPage` | Patient grid with latest vitals |
| `pages/PatientDetailPage` | Vitals history table + alerts with resolve button |
| `pages/AlertCenterPage` | Global alert list with unresolved/all filter |

**Talking points:**
- Custom hooks keep pages clean — each page just calls a hook and renders.
- WebSocket hooks use cleanup functions (`client.deactivate()`) so connections
  close properly when navigating away.
- `Promise.allSettled` is used when fetching latest vitals per patient so a
  single patient with no data does not crash the whole dashboard.

---

## 8. Simulator Role

The Python simulator (`simulator/simulator.py`) replaces physical IoT hardware.

**How it works:**
1. Reads device codes from `.env` (`DEVICE_CODES`)
2. Validates each code against the backend at startup
3. Maintains a `VitalState` per device — values drift gradually each tick
4. With probability `ANOMALY_RATE` (default 10%), injects a named anomaly
   scenario instead of a normal reading
5. POSTs each reading to `POST /api/v1/vitals` every `INTERVAL_SECONDS`

**Anomaly scenarios and the backend alerts they trigger:**

| Scenario | Alert triggered |
|---|---|
| `high_hr_warning` | HIGH_HEART_RATE WARNING |
| `high_hr_critical` | HIGH_HEART_RATE CRITICAL |
| `low_hr_critical` | LOW_HEART_RATE CRITICAL |
| `fever_warning` | HIGH_TEMPERATURE WARNING |
| `fever_critical` | HIGH_TEMPERATURE CRITICAL |
| `hypothermia` | LOW_TEMPERATURE CRITICAL |
| `spo2_warning` | LOW_SPO2 WARNING |
| `spo2_critical` | LOW_SPO2 CRITICAL |

**Talking points:**
- Anomaly injection is intentional and controlled — this is what makes the
  demo reliable. We can guarantee alerts will appear on screen without waiting.
- Each device drifts independently, so with multiple devices the dashboard
  shows different readings per patient, which looks realistic.
- Raising `ANOMALY_RATE=0.50` during the demo guarantees frequent alerts for
  demonstration purposes.

---

## 9. Demo Flow Summary

1. Start PostgreSQL → start backend → start frontend
2. Confirm dashboard loads at `http://localhost:5173`
3. Seed a patient and device if not already present
4. Start the simulator — watch the patient card update live
5. Wait for an anomaly — watch alerts appear in Alert Center in real time
6. Navigate to Patient Detail — show vitals history table updating
7. Resolve an alert from the UI
8. Point out the Swagger UI at `http://localhost:8080/swagger-ui/index.html`

Full script: [docs/demo-scenario.md](demo-scenario.md)

---

## 10. Key Technical Decisions

**Spring Boot + PostgreSQL for the backend**
Familiar, well-documented, strong JPA support, production-grade WebSocket
via STOMP. Fits the project scope without over-engineering.

**STOMP over SockJS for WebSocket**
STOMP provides a topic-subscription model that maps cleanly to our
patient-scoped and global topics. SockJS adds browser fallback support.
The frontend uses `@stomp/stompjs` which pairs directly with the Spring backend.

**React with custom hooks**
Separating data-fetching into hooks keeps pages readable and makes it easy
to add WebSocket live-patching without touching the UI components.

**Python for the simulator**
Lightweight, readable, easy to configure via `.env`. The `requests` library
makes HTTP calls trivial. No framework needed — a single file covers
the full simulation loop.

**No authentication — intentional**
Authentication is out of scope for this demo project. All endpoints and
WebSocket topics are open. This is documented as a known limitation and a
future improvement.

**DTOs as Java records**
Immutable by default, no Lombok needed for getters, compact syntax.
Keeps the API contract explicit and separate from the database model.

---

## 11. Challenges Encountered

**CORS between frontend and backend**
The browser blocked requests from `localhost:5173` to `localhost:8080` until
we added `WebConfig` with explicit allowed origins. The fix was straightforward
once identified — configurable via `application.yml`.

**WebSocket allowed origins**
Spring's STOMP endpoint rejected SockJS connections with a wildcard origin.
We made allowed origins configurable (`app.websocket.allowed-origins`)
to match the CORS fix pattern.

**SpO2 boundary condition**
A Sourcery review caught that `spo2 < 92` would incorrectly treat exactly 92
as a warning. The fix was changing to `spo2 <= LOW_SPO2_CRITICAL`. This
highlights the importance of boundary testing — we added an explicit test
case for this exact value.

**Device validation in the simulator**
The simulator could silently fail if a device code was wrong, sending readings
that the backend would reject with 404. We added a startup validation step
that probes each device code and gives a clear per-device error rather than
failing silently mid-run.

**Node.js version requirement**
Vite 8 requires Node 20.19+ or 22.12+. The initial PATH issue on Windows was
resolved using `nvm-windows`. This is now documented in the setup guide.

---

## 12. Future Improvements

| Improvement | Reason deferred |
|---|---|
| Authentication / JWT | Out of scope for demo project |
| Transactional WebSocket publishing (`@TransactionalEventListener`) | Architectural improvement, system works correctly without it |
| Externalized alert thresholds (config file) | Not needed for demo; constants are clear in `AlertService.java` |
| REST endpoint path cleanup (`/patients/{id}/vitals/latest`) | Deferred to avoid breaking the frontend mid-project |
| Pagination for patients and alerts | Low priority for demo scale |
| Python simulator as a proper CLI with arguments | Low priority, `.env` is sufficient |
| Physical device integration | Requires hardware; out of scope |
| Deployment / Docker | Not required for the school submission |

---

## 13. Suggested Presentation Structure

| # | Section | Suggested time |
|---|---|---|
| 1 | Introduction — project pitch and problem statement | 1 min |
| 2 | Architecture overview — diagram walkthrough | 2 min |
| 3 | Backend — modules, alert detection, API, tests | 3 min |
| 4 | Frontend — pages, hooks, WebSocket integration | 2 min |
| 5 | Simulator — drift model, anomaly injection | 1 min |
| 6 | Live demo — full end-to-end flow | 4 min |
| 7 | Technical decisions and challenges | 2 min |
| 8 | Future improvements | 1 min |
| 9 | Questions | open |

**Total spoken time: ~16 minutes** (adjust per your time limit)

---

## 14. Likely Examiner Questions and Suggested Answers

**"Why did you choose Spring Boot over a lighter framework?"**
Spring Boot gave us production-grade WebSocket (STOMP), JPA, validation, and
actuator out of the box. For a project this size it was the right tradeoff
between familiarity and features.

**"How does the alert detection work?"**
Every time a vital sign is ingested, `AlertService.detectAndCreateAlerts()` runs
synchronously inside the same transaction. It checks each vital value against
hardcoded thresholds and creates alert records for any violations. Those alerts
are then published to WebSocket topics so the frontend receives them instantly.

**"What happens if the WebSocket connection drops?"**
The STOMP client is configured with `reconnectDelay: 5000`. It will
automatically attempt to reconnect every 5 seconds without any user action.

**"How do you prevent the simulator from sending data for a non-existent device?"**
At startup, the simulator sends a test reading for each configured device code.
If the backend returns anything other than 201, that device is skipped with a
warning. If all devices fail, the simulator exits with a clear message.

**"Why is there no authentication?"**
Authentication is intentionally out of scope for this demo. The project focuses
on IoT ingestion, alert detection, and real-time delivery. We documented it as
a known limitation and outlined how it would be added in the future improvements
section.

**"How did you test the alert thresholds?"**
`AlertServiceTest` has a dedicated test for every threshold boundary, including
edge cases like SpO2 exactly at 92 (which should be CRITICAL, not WARNING —
a bug we caught during a code review).

**"Could you connect a real device to this system?"**
Yes. Any device that can send an HTTP POST to `/api/v1/vitals` with the correct
JSON payload would work. The `deviceCode` just needs to be registered in the
database first.