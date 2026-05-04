import os
import time
import random
import logging
from datetime import datetime, timezone
from decimal import Decimal

import requests
from dotenv import load_dotenv

load_dotenv()

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080/api/v1")
DEVICE_CODE = os.getenv("DEVICE_CODE", "DEV-001")
INTERVAL_SECONDS = int(os.getenv("INTERVAL_SECONDS", "5"))

VITALS_ENDPOINT = f"{API_BASE_URL}/vitals"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


def generate_vital_signs() -> dict:
    """Generate a single set of normal-range vital signs."""
    heart_rate = random.randint(60, 100)
    temperature = round(random.uniform(36.1, 37.5), 1)
    spo2 = random.randint(95, 100)
    recorded_at = datetime.now(timezone.utc).isoformat()

    return {
        "deviceCode": DEVICE_CODE,
        "heartRate": heart_rate,
        "temperature": temperature,
        "spo2": spo2,
        "recordedAt": recorded_at,
    }


def send_vital_signs(payload: dict) -> bool:
    """POST vital signs to the backend ingestion API. Returns True on success."""
    try:
        response = requests.post(VITALS_ENDPOINT, json=payload, timeout=5)
        response.raise_for_status()
        log.info(
            "Sent | device=%-10s HR=%3d bpm  Temp=%4.1f°C  SpO2=%3d%%",
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
    log.info("Simulator starting — device=%s  interval=%ds", DEVICE_CODE, INTERVAL_SECONDS)
    log.info("Target: %s", VITALS_ENDPOINT)

    while True:
        payload = generate_vital_signs()
        send_vital_signs(payload)
        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        log.info("Simulator stopped.")