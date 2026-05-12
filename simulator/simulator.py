import os
import time
import random
import logging
from datetime import datetime, timezone

import requests
from dotenv import load_dotenv

load_dotenv()

API_BASE_URL      = os.getenv("API_BASE_URL", "http://localhost:8080/api/v1")
DEVICE_CODES_RAW  = os.getenv("DEVICE_CODES", os.getenv("DEVICE_CODE", "DEV-001"))
INTERVAL_SECONDS  = int(os.getenv("INTERVAL_SECONDS", "5"))
ANOMALY_RATE      = float(os.getenv("ANOMALY_RATE", "0.10"))

VITALS_ENDPOINT        = f"{API_BASE_URL}/vitals"
DEVICE_LOOKUP_ENDPOINT = f"{API_BASE_URL}/devices/code"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Backend alert thresholds (must match AlertService.java constants)
# ---------------------------------------------------------------------------
HR_CRITICAL_LOW   = 50
HR_NORMAL_LOW     = 60
HR_NORMAL_HIGH    = 100
HR_WARNING_HIGH   = 110
HR_CRITICAL_HIGH  = 120

TEMP_CRITICAL_LOW  = 35.0
TEMP_NORMAL_LOW    = 36.1
TEMP_NORMAL_HIGH   = 37.5
TEMP_WARNING_HIGH  = 37.8
TEMP_CRITICAL_HIGH = 38.0

SPO2_NORMAL_LOW   = 95
SPO2_WARNING_LOW  = 94
SPO2_CRITICAL_LOW = 92


# ---------------------------------------------------------------------------
# Per-device vital state — each device drifts independently
# ---------------------------------------------------------------------------
class VitalState:
    def __init__(self, device_code: str):
        self.device_code = device_code
        self.heart_rate  = float(random.randint(65, 85))
        self.temperature = round(random.uniform(36.3, 37.0), 1)
        self.spo2        = float(random.randint(96, 99))

    def drift(self):
        self.heart_rate  = self._clamp(self.heart_rate  + random.uniform(-2, 2),    HR_NORMAL_LOW,   HR_NORMAL_HIGH)
        self.temperature = self._clamp(self.temperature + random.uniform(-0.1, 0.1), TEMP_NORMAL_LOW, TEMP_NORMAL_HIGH)
        self.spo2        = self._clamp(self.spo2        + random.uniform(-0.5, 0.5), SPO2_NORMAL_LOW, 100.0)

    @staticmethod
    def _clamp(value, lo, hi):
        return max(lo, min(hi, value))


# ---------------------------------------------------------------------------
# Anomaly generation
# ---------------------------------------------------------------------------
def pick_anomaly() -> dict:
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

    hr   = random.randint(65, 85)
    temp = round(random.uniform(36.3, 37.0), 1)
    spo2 = random.randint(96, 99)

    if anomaly == "high_hr_warning":
        hr   = random.randint(HR_WARNING_HIGH, HR_CRITICAL_HIGH)
        temp = round(random.uniform(37.2, 37.7), 1)
    elif anomaly == "high_hr_critical":
        hr   = random.randint(HR_CRITICAL_HIGH + 1, 140)
        temp = round(random.uniform(37.3, 37.8), 1)
    elif anomaly == "low_hr_critical":
        hr   = random.randint(30, HR_CRITICAL_LOW - 1)
    elif anomaly == "fever_warning":
        temp = round(random.uniform(TEMP_WARNING_HIGH, TEMP_CRITICAL_HIGH), 1)
        hr   = random.randint(95, HR_WARNING_HIGH - 1)
    elif anomaly == "fever_critical":
        temp = round(random.uniform(TEMP_CRITICAL_HIGH + 0.1, 40.0), 1)
        hr   = random.randint(100, 115)
    elif anomaly == "hypothermia":
        temp = round(random.uniform(32.0, TEMP_CRITICAL_LOW - 0.1), 1)
        hr   = random.randint(45, 65)
    elif anomaly == "spo2_warning":
        spo2 = random.randint(SPO2_WARNING_LOW - 1, SPO2_WARNING_LOW)
        hr   = random.randint(95, 110)
    elif anomaly == "spo2_critical":
        spo2 = random.randint(85, SPO2_CRITICAL_LOW)
        hr   = random.randint(100, 120)

    return {"heart_rate": hr, "temperature": temp, "spo2": spo2, "anomaly": anomaly}


# ---------------------------------------------------------------------------
# Payload builder
# ---------------------------------------------------------------------------
def build_payload(state: VitalState) -> dict:
    is_anomaly = random.random() < ANOMALY_RATE

    if is_anomaly:
        values = pick_anomaly()
        label  = f"ANOMALY({values['anomaly']})"
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
            "deviceCode":  state.device_code,
            "heartRate":   values["heart_rate"],
            "temperature": values["temperature"],
            "spo2":        values["spo2"],
            "recordedAt":  datetime.now(timezone.utc).isoformat(),
        },
        "label": label,
    }


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------
def send_vital_signs(payload: dict, label: str) -> bool:
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
        log.error("[%s] Connection failed — is the backend running at %s?",
                  payload["deviceCode"], API_BASE_URL)
    except requests.exceptions.Timeout:
        log.error("[%s] Request timed out after 5s", payload["deviceCode"])
    except requests.exceptions.HTTPError as e:
        body = e.response.text
        if e.response.status_code == 400 and "cannot accept readings" in body:
            # Device is MAINTENANCE or INACTIVE — not an error, just skip quietly
            status_word = "MAINTENANCE" if "MAINTENANCE" in body else "INACTIVE"
            log.info("%-22s | device=%-10s skipped — device is %s",
                     "SKIPPED", payload["deviceCode"], status_word)
        else:
            log.error("[%s] HTTP %s — %s",
                      payload["deviceCode"], e.response.status_code, body)
    return False


# ---------------------------------------------------------------------------
# Startup validation — verify each device code exists in the backend
# ---------------------------------------------------------------------------
def validate_device(device_code: str) -> bool:
    """
    Verify the device code exists in the backend using a GET lookup.
    Accepts devices in any status — MAINTENANCE/INACTIVE are handled gracefully
    at send time, not filtered out at startup.
    """
    try:
        response = requests.get(f"{DEVICE_LOOKUP_ENDPOINT}/{device_code}", timeout=5)
        if response.status_code == 200:
            data = response.json()
            status = data.get("status", "UNKNOWN")
            if status != "ACTIVE":
                log.warning("  ✓ %s — found (status: %s, readings will be skipped until ACTIVE)",
                            device_code, status)
            return True
        log.error(
            "Device '%s' not found in backend — HTTP %s: %s",
            device_code, response.status_code, response.text,
        )
        return False
    except requests.exceptions.ConnectionError:
        log.error("Cannot reach backend at %s — is it running?", API_BASE_URL)
        return False
    except requests.exceptions.Timeout:
        log.error("Validation request timed out for device '%s'", device_code)
        return False


def parse_device_codes(raw: str) -> list[str]:
    codes = [c.strip() for c in raw.split(",") if c.strip()]
    if not codes:
        log.error("DEVICE_CODES is empty. Set at least one device code in .env")
        raise SystemExit(1)
    return codes


# ---------------------------------------------------------------------------
# Main loop
# ---------------------------------------------------------------------------
def run():
    device_codes = parse_device_codes(DEVICE_CODES_RAW)

    log.info("Configured devices: %s", device_codes)
    log.info("Validating device codes against backend...")

    valid_states: list[VitalState] = []

    for code in device_codes:
        if validate_device(code):
            valid_states.append(VitalState(code))
        else:
            log.warning("  ✗ %s — not found in backend, skipping", code)

    if not valid_states:
        log.error("No valid devices found. Check your DEVICE_CODES and ensure "
                  "they are registered in the backend. Exiting.")
        raise SystemExit(1)

    log.info(
        "Simulator running — %d device(s)  interval=%ds  anomaly_rate=%.0f%%",
        len(valid_states), INTERVAL_SECONDS, ANOMALY_RATE * 100,
    )

    while True:
        for state in valid_states:
            result = build_payload(state)
            send_vital_signs(result["payload"], result["label"])
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        log.info("Simulator stopped.")