import os
import time
import random
import logging
from datetime import datetime, timezone

import requests
from dotenv import load_dotenv

load_dotenv()

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080/api/v1")
DEVICE_CODE = os.getenv("DEVICE_CODE", "DEV-001")
INTERVAL_SECONDS = int(os.getenv("INTERVAL_SECONDS", "5"))
ANOMALY_RATE = float(os.getenv("ANOMALY_RATE", "0.10"))

VITALS_ENDPOINT = f"{API_BASE_URL}/vitals"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Backend alert thresholds (must match AlertService.java constants)
# ---------------------------------------------------------------------------
HR_CRITICAL_LOW  = 50
HR_NORMAL_LOW    = 60
HR_NORMAL_HIGH   = 100
HR_WARNING_HIGH  = 110
HR_CRITICAL_HIGH = 120

TEMP_CRITICAL_LOW  = 35.0
TEMP_NORMAL_LOW    = 36.1
TEMP_NORMAL_HIGH   = 37.5
TEMP_WARNING_HIGH  = 37.8
TEMP_CRITICAL_HIGH = 38.0

SPO2_NORMAL_LOW   = 95
SPO2_WARNING_LOW  = 94   # <= triggers WARNING
SPO2_CRITICAL_LOW = 92   # <= triggers CRITICAL


# ---------------------------------------------------------------------------
# State — tracks current "baseline" so values drift gradually
# ---------------------------------------------------------------------------
class VitalState:
    def __init__(self):
        self.heart_rate  = float(random.randint(65, 85))
        self.temperature = round(random.uniform(36.3, 37.0), 1)
        self.spo2        = float(random.randint(96, 99))

    def drift(self):
        """Apply a small random walk to each vital sign."""
        self.heart_rate  = self._clamp(self.heart_rate  + random.uniform(-2, 2),  HR_NORMAL_LOW,   HR_NORMAL_HIGH)
        self.temperature = self._clamp(self.temperature + random.uniform(-0.1, 0.1), TEMP_NORMAL_LOW, TEMP_NORMAL_HIGH)
        self.spo2        = self._clamp(self.spo2        + random.uniform(-0.5, 0.5), SPO2_NORMAL_LOW, 100.0)

    @staticmethod
    def _clamp(value, lo, hi):
        return max(lo, min(hi, value))


def pick_anomaly() -> dict:
    """
    Return a set of vital signs that will trigger at least one backend alert.
    Anomaly types and their correlations:
      - high_hr_warning  : HR warning, slight temp rise
      - high_hr_critical : HR critical, slight temp rise
      - low_hr_critical  : HR critical low
      - fever_warning    : Temp warning, HR slightly elevated
      - fever_critical   : Temp critical, HR elevated
      - hypothermia      : Temp critical low
      - spo2_warning     : SpO2 warning, HR slightly elevated
      - spo2_critical    : SpO2 critical, HR elevated
    """
    anomaly = random.choice([
        "high_hr_warning",
        "high_hr_critical",
        "low_hr_critical",
        "fever_warning",
        "fever_critical",
        "hypothermia",
        "spo2_warning",
        "spo2_critical",
    ])

    # Defaults — will be overridden per anomaly
    hr   = random.randint(65, 85)
    temp = round(random.uniform(36.3, 37.0), 1)
    spo2 = random.randint(96, 99)

    if anomaly == "high_hr_warning":
        hr   = random.randint(HR_WARNING_HIGH, HR_CRITICAL_HIGH)      # 110–120
        temp = round(random.uniform(37.2, 37.7), 1)                   # slightly warm

    elif anomaly == "high_hr_critical":
        hr   = random.randint(HR_CRITICAL_HIGH + 1, 140)              # > 120
        temp = round(random.uniform(37.3, 37.8), 1)

    elif anomaly == "low_hr_critical":
        hr   = random.randint(30, HR_CRITICAL_LOW - 1)                # < 50

    elif anomaly == "fever_warning":
        temp = round(random.uniform(TEMP_WARNING_HIGH, TEMP_CRITICAL_HIGH), 1)  # 37.8–38.0
        hr   = random.randint(95, HR_WARNING_HIGH - 1)                # slightly elevated

    elif anomaly == "fever_critical":
        temp = round(random.uniform(TEMP_CRITICAL_HIGH + 0.1, 40.0), 1)        # > 38.0
        hr   = random.randint(100, 115)

    elif anomaly == "hypothermia":
        temp = round(random.uniform(32.0, TEMP_CRITICAL_LOW - 0.1), 1)         # < 35.0
        hr   = random.randint(45, 65)

    elif anomaly == "spo2_warning":
        spo2 = random.randint(SPO2_WARNING_LOW - 1, SPO2_WARNING_LOW)          # 93–94
        hr   = random.randint(95, 110)

    elif anomaly == "spo2_critical":
        spo2 = random.randint(85, SPO2_CRITICAL_LOW)                           # 85–92
        hr   = random.randint(100, 120)

    return {"heart_rate": hr, "temperature": temp, "spo2": spo2, "anomaly": anomaly}


def build_payload(state: VitalState) -> dict:
    """
    Return a payload dict. With probability ANOMALY_RATE, inject an anomaly
    instead of drifting normally.
    """
    is_anomaly = random.random() < ANOMALY_RATE

    if is_anomaly:
        values = pick_anomaly()
        label = f"ANOMALY({values['anomaly']})"
    else:
        state.drift()
        values = {
            "heart_rate":  int(round(state.heart_rate)),
            "temperature": round(state.temperature, 1),
            "spo2":        int(round(state.spo2)),
        }
        label = "normal"

    return {
        "payload": {
            "deviceCode": DEVICE_CODE,
            "heartRate":  values["heart_rate"],
            "temperature": values["temperature"],
            "spo2":        values["spo2"],
            "recordedAt":  datetime.now(timezone.utc).isoformat(),
        },
        "label": label,
    }


def send_vital_signs(payload: dict, label: str) -> bool:
    """POST vital signs to the backend ingestion API. Returns True on success."""
    try:
        response = requests.post(VITALS_ENDPOINT, json=payload, timeout=5)
        response.raise_for_status()
        log.info(
            "%-22s | device=%-10s HR=%3d bpm  Temp=%4.1f°C  SpO2=%3d%%",
            label,
            payload["deviceCode"],
            payload["heartRate"],
            payload["temperature"],
            payload["spo2"],
        )
        return True
    except requests.exceptions.ConnectionError:
        log.error("Connection failed — is the backend running at %s?", API_BASE_URL)
    except requests.exceptions.Timeout:
        log.error("Request timed out after 5s")
    except requests.exceptions.HTTPError as e:
        log.error("HTTP %s — %s", e.response.status_code, e.response.text)
    return False


def run():
    log.info(
        "Simulator starting — device=%s  interval=%ds  anomaly_rate=%.0f%%",
        DEVICE_CODE, INTERVAL_SECONDS, ANOMALY_RATE * 100,
    )
    log.info("Target: %s", VITALS_ENDPOINT)

    state = VitalState()

    while True:
        result = build_payload(state)
        send_vital_signs(result["payload"], result["label"])
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        log.info("Simulator stopped.")