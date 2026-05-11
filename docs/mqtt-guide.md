# MQTT Ingestion Guide

MQTT is an **optional, additive** ingestion path. The existing HTTP `POST /api/v1/vitals` endpoint is unchanged. When MQTT is enabled the backend subscribes to:

```
iot-health/devices/{deviceCode}/vitals
```

Any message arriving on that topic is processed by the same pipeline as an HTTP request: vital sign saved → alert detection → WebSocket broadcast → visible in the frontend.

---

## Prerequisites

- Docker Desktop (to run Mosquitto), **or** Mosquitto installed natively
- Backend already running with a valid `application.yml`
- `paho-mqtt` installed in the simulator venv (already in `requirements.txt`)

---

## 1. Start the Mosquitto Broker

### Option A — Docker (recommended)

```powershell
docker-compose -f docker-compose.mqtt.yml up -d
```

Mosquitto starts on `localhost:1883`. To stop it:

```powershell
docker-compose -f docker-compose.mqtt.yml down
```

### Option B — Native Mosquitto

Install from [mosquitto.org/download](https://mosquitto.org/download/), then run with the provided config:

```powershell
mosquitto -c mosquitto/mosquitto.conf
```

---

## 2. Enable MQTT in the Backend

In `backend/src/main/resources/application.yml`, set:

```yaml
app:
  mqtt:
    enabled: true
    broker-url: tcp://localhost:1883
    client-id: iot-health-backend
    topic-pattern: iot-health/devices/+/vitals
    qos: 1
```

The `+` wildcard matches any single device code segment, so one subscription covers all devices.

Restart the backend. On startup you should see:

```
MQTT listener connected to tcp://localhost:1883 — subscribed to 'iot-health/devices/+/vitals'
```

---

## 3. Publish Test Messages

### Option A — Python publisher

```powershell
cd simulator
# activate your venv first, then:
python mqtt_publisher.py                # one round of normal readings, then exit
python mqtt_publisher.py --loop         # continuous publishing (every INTERVAL_SECONDS)
python mqtt_publisher.py --anomaly      # inject an anomalous reading in the first batch
python mqtt_publisher.py --loop --anomaly
```

The publisher reads `DEVICE_CODES` and `INTERVAL_SECONDS` from the same `.env` used by the HTTP simulator. Add `MQTT_BROKER_HOST` and `MQTT_BROKER_PORT` to `.env` if the broker is not on localhost:

```env
MQTT_BROKER_HOST=localhost
MQTT_BROKER_PORT=1883
```

### Option B — Mosquitto CLI (`mosquitto_pub`)

Publish a single reading for DEV-001:

```bash
mosquitto_pub -h localhost -p 1883 \
  -t "iot-health/devices/DEV-001/vitals" \
  -m '{"deviceCode":"DEV-001","heartRate":88,"temperature":36.9,"spo2":97,"recordedAt":"2026-05-05T10:00:00Z"}'
```

Trigger a critical SpO2 alert:

```bash
mosquitto_pub -h localhost -p 1883 \
  -t "iot-health/devices/DEV-001/vitals" \
  -m '{"deviceCode":"DEV-001","heartRate":80,"temperature":36.8,"spo2":91,"recordedAt":"2026-05-05T10:00:01Z"}'
```

---

## 4. Verify End-to-End

1. Open the frontend at `http://localhost:5173`.
2. Publish a message via the Python publisher or `mosquitto_pub`.
3. The patient card for the device's patient should update in real time — the same as when the HTTP simulator sends data.
4. If the reading crosses an alert threshold, an alert badge appears immediately.

You can also confirm via the REST API:

```powershell
# Replace 1 with the actual patient ID
Invoke-RestMethod http://localhost:8080/api/v1/vitals/patient/1/latest
```

---

## MQTT Payload Format

```json
{
  "deviceCode": "DEV-001",
  "heartRate": 88,
  "temperature": 36.9,
  "spo2": 97,
  "recordedAt": "2026-05-05T10:00:00Z"
}
```

| Field         | Type            | Constraints                  |
|---------------|-----------------|------------------------------|
| `deviceCode`  | string          | must exist in the database   |
| `heartRate`   | integer         | 30–220 bpm                   |
| `temperature` | decimal         | 30.0–45.0 °C                 |
| `spo2`        | integer         | 50–100 %                     |
| `recordedAt`  | ISO-8601 string | optional — defaults to now   |

---

## Topic Convention

```
iot-health/devices/{deviceCode}/vitals
```

Examples:

```
iot-health/devices/DEV-001/vitals
iot-health/devices/DEV-002/vitals
```

The backend subscribes to `iot-health/devices/+/vitals`. The `+` is an MQTT single-level wildcard that matches exactly one path segment, so all device topics are covered by a single subscription.

---

## Configuration Reference

All properties live under `app.mqtt` in `application.yml`:

| Property        | Default                            | Description                              |
|-----------------|------------------------------------|------------------------------------------|
| `enabled`       | `false`                            | Set to `true` to activate MQTT ingestion |
| `broker-url`    | `tcp://localhost:1883`             | Paho broker URL                          |
| `client-id`     | `iot-health-backend`               | MQTT client identifier                   |
| `topic-pattern` | `iot-health/devices/+/vitals`      | Topic to subscribe to                    |
| `qos`           | `1`                                | QoS level (0, 1, or 2)                   |

---

## Future: Wokwi ESP32 Simulation

Once the MQTT pipeline is stable locally, a [Wokwi](https://wokwi.com/) ESP32 sketch can replace or complement the Python publisher. The ESP32 connects to the same Mosquitto broker and publishes to the same topic convention. No backend changes are needed — the broker URL just needs to be reachable from the Wokwi simulation network.
