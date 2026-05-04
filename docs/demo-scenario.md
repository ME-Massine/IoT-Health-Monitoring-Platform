# Demo Scenario — IoT Health Monitoring Platform

This document is the step-by-step script for the final project presentation.
Follow it in order. Every step has an expected outcome so you know what to look
for on screen.

---

## Prerequisites checklist

Before starting the demo, confirm all of these are true:

- [ ] PostgreSQL is running on port 5432
- [ ] `iot_health_db` database exists
- [ ] `backend/src/main/resources/application.yml` is configured with the correct DB password
- [ ] Node.js 20.19+ or 22.12+ is installed
- [ ] Python 3.12 is installed and the simulator `venv` is set up
- [ ] `simulator/.env` exists and contains at least `DEV-001` in `DEVICE_CODES`
- [ ] A patient is registered in the database and assigned to `DEV-001`

> If the patient/device data is missing, complete **Step 3** before the demo.
> All other steps assume the data already exists.

---

## Step 1 — Start the backend

Open a dedicated terminal and run:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Expected:** Spring Boot starts on port 8080. Wait until you see:

```
Started BackendApplication in X seconds
```

Verify it is healthy:

```
GET http://localhost:8080/actuator/health
→ { "status": "UP" }
```

---

## Step 2 — Start the frontend

Open a second terminal and run:

```powershell
cd frontend
npm run dev
```

**Expected:** Vite starts on `http://localhost:5173`.

Open the browser at `http://localhost:5173`. You should see the Patient
Dashboard. If no patients are registered yet the page shows "No patients found"
— that is correct at this stage.

---

## Step 3 — Seed demo patient and device data

> Skip this step if the patient and device already exist in the database.

Use Postman, curl, or the Swagger UI at `http://localhost:8080/swagger-ui.html`.

### 3a — Create a patient

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

Note the `id` returned in the response (e.g. `2`).

### 3b — Register a device and assign it to the patient

```http
POST http://localhost:8080/api/v1/devices
Content-Type: application/json

{
  "deviceCode": "DEV-001",
  "type": "Multi-parameter monitor",
  "status": "ACTIVE",
  "patientId": 2
}
```

**Expected:** 201 Created. The device `DEV-001` is now linked to the patient.

Refresh the browser — the patient card should appear on the dashboard.

> To add more devices for a richer demo, repeat this step with additional
> patient records and device codes (e.g. `DEV-002`), then add those codes to
> `simulator/.env` under `DEVICE_CODES`.

---

## Step 4 — Configure and start the simulator

Open a third terminal.

Confirm `simulator/.env` contains:

```env
API_BASE_URL=http://localhost:8080/api/v1
DEVICE_CODES=DEV-001
INTERVAL_SECONDS=5
ANOMALY_RATE=0.10
```

Activate the virtual environment and start the simulator:

```powershell
cd simulator
.\venv\Scripts\Activate.ps1
python simulator.py
```

**Expected startup output:**

```
Configured devices: ['DEV-001']
Validating device codes against backend...
  ✓ DEV-001 — accepted
Simulator running — 1 device(s)  interval=5s  anomaly_rate=10%
```

The simulator then sends a reading every 5 seconds:

```
normal                 | device=DEV-001     HR= 74 bpm  Temp=36.8°C  SpO2= 97%
normal                 | device=DEV-001     HR= 76 bpm  Temp=36.9°C  SpO2= 97%
```

---

## Step 5 — Show live vital sign updates on the dashboard

Switch to the browser at `http://localhost:5173`.

**What to show:**

- The patient card updates its latest vitals automatically every time the
  simulator sends a reading (no page refresh needed — WebSocket is live).
- Point out the heart rate, temperature, and SpO2 values changing on the card.

---

## Step 6 — Trigger abnormal readings and show alert creation

The simulator fires an anomaly roughly every 10 readings at the default 10%
rate. To guarantee an anomaly fires quickly during the demo, temporarily raise
the rate before starting:

```env
ANOMALY_RATE=0.50
```

Restart the simulator. Within a few seconds you will see lines like:

```
ANOMALY(spo2_critical) | device=DEV-001     HR=108 bpm  Temp=36.7°C  SpO2= 88%
ANOMALY(fever_critical)| device=DEV-001     HR=104 bpm  Temp=38.4°C  SpO2= 97%
```

**What to show in the browser:**

- Navigate to **Alert Center** (`/alerts`). New alerts appear in real time
  without refreshing — the WebSocket pushes them immediately.
- Point out the severity badge (WARNING / CRITICAL) and the alert message
  matching the anomaly type.
- Navigate to the **Patient Detail** page for the affected patient. The alerts
  section also updates live.

> Remember to reset `ANOMALY_RATE=0.10` after the demo.

---

## Step 7 — Show alert resolution

### Option A — Resolve from the UI

1. On the Patient Detail page or Alert Center, find an unresolved alert.
2. Click the **Resolve** button next to it.
3. The alert badge changes to "Resolved" immediately.

### Option B — Resolve via API

```http
PUT http://localhost:8080/api/v1/alerts/{alertId}/resolve
```

Replace `{alertId}` with the numeric ID visible in the alert list.

**Expected:** The alert `resolved` field becomes `true` and `resolvedAt` is
set. The UI reflects this without a page refresh.

---

## Step 8 — Wrap-up talking points

| Layer | Technology | What it does in the demo |
|---|---|---|
| Simulator | Python 3.12 | Generates realistic vitals and anomalies |
| Ingestion API | Spring Boot | Receives vitals, persists, detects alerts |
| Alert engine | Spring Boot | Evaluates thresholds, creates alert records |
| WebSocket broker | STOMP / SockJS | Pushes vitals and alerts to the frontend live |
| Frontend | React 19 + Vite | Displays dashboard, detail page, alert center |
| Database | PostgreSQL | Stores patients, devices, vitals, alerts |

---

## Known limitations / out of scope

- No authentication — all data is visible to any browser session. This is
  intentional for the demo.
- The simulator sends readings sequentially per device per tick. With many
  devices and a short interval, readings may queue slightly.
- WebSocket publishing is not transaction-after-commit safe (deferred to
  issue #78). Alerts are always persisted correctly; the WebSocket event
  may fire slightly before the transaction commits in rare cases.