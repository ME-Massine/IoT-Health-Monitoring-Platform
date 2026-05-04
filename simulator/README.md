# Sensor Simulator

A Python script that simulates an IoT device sending vital signs to the backend ingestion API.

## Requirements

- Python 3.12+
- Backend running at `http://localhost:8080`
- At least one device registered in the database

## Setup

```powershell
cd simulator
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env
```

Edit `.env` to match your device code and desired interval.

## Configuration

| Variable           | Default                        | Description                              |
|--------------------|--------------------------------|------------------------------------------|
| `API_BASE_URL`     | `http://localhost:8080/api/v1` | Backend API base URL                     |
| `DEVICE_CODE`      | `DEV-001`                      | Device code registered in the DB         |
| `INTERVAL_SECONDS` | `5`                            | Seconds between each reading             |
| `ANOMALY_RATE`     | `0.10`                         | Fraction of readings that are anomalous (0.0–1.0) |

## Anomaly types

When an anomaly fires, one of the following scenarios is injected and will trigger the corresponding backend alert:

| Scenario           | What fires                          |
|--------------------|-------------------------------------|
| `high_hr_warning`  | HR warning + slight temp rise       |
| `high_hr_critical` | HR critical high                    |
| `low_hr_critical`  | HR critical low                     |
| `fever_warning`    | Temp warning + elevated HR          |
| `fever_critical`   | Temp critical high + elevated HR    |
| `hypothermia`      | Temp critical low                   |
| `spo2_warning`     | SpO2 warning + elevated HR          |
| `spo2_critical`    | SpO2 critical + elevated HR         |

## Run

```powershell
python simulator.py
```

Stop with `Ctrl+C`.