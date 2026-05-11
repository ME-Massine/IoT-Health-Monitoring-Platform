"""
MQTT test publisher for the IoT Health Monitoring Platform.

Publishes sample vital sign payloads to:
  iot-health/devices/{deviceCode}/vitals

The backend MQTT listener (when enabled) picks these up and runs them
through the same ingestion pipeline as the HTTP simulator.

Usage:
  python mqtt_publisher.py              # publish one round of readings and exit
  python mqtt_publisher.py --loop       # publish continuously (same interval as .env)
  python mqtt_publisher.py --anomaly    # include an anomalous reading in the first batch
"""

import argparse
import json
import os
import random
import sys
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt
from dotenv import load_dotenv

load_dotenv()

BROKER_HOST = os.getenv("MQTT_BROKER_HOST", "localhost")
BROKER_PORT = int(os.getenv("MQTT_BROKER_PORT", "1883"))
DEVICE_CODES = [c.strip() for c in os.getenv("DEVICE_CODES", "DEV-001").split(",") if c.strip()]
INTERVAL_SECONDS = int(os.getenv("INTERVAL_SECONDS", "5"))
TOPIC_PREFIX = "iot-health/devices"


def normal_reading(device_code: str) -> dict:
    return {
        "deviceCode": device_code,
        "heartRate": random.randint(65, 95),
        "temperature": round(random.uniform(36.2, 37.4), 1),
        "spo2": random.randint(96, 99),
        "recordedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }


def anomalous_reading(device_code: str) -> dict:
    scenarios = [
        {"heartRate": 125, "temperature": 38.5, "spo2": 97},   # high HR + fever
        {"heartRate": 45,  "temperature": 36.5, "spo2": 97},   # low HR critical
        {"heartRate": 80,  "temperature": 36.8, "spo2": 91},   # low SpO2 critical
        {"heartRate": 112, "temperature": 37.9, "spo2": 94},   # high HR + temp warning
    ]
    overrides = random.choice(scenarios)
    reading = normal_reading(device_code)
    reading.update(overrides)
    return reading


def publish_readings(client: mqtt.Client, use_anomaly: bool = False) -> None:
    for device_code in DEVICE_CODES:
        reading = anomalous_reading(device_code) if use_anomaly else normal_reading(device_code)
        topic = f"{TOPIC_PREFIX}/{device_code}/vitals"
        payload = json.dumps(reading)
        result = client.publish(topic, payload, qos=1)
        result.wait_for_publish()
        print(f"  [{device_code}] -> {topic}")
        print(f"           {payload}")


def main() -> None:
    parser = argparse.ArgumentParser(description="MQTT vital sign test publisher")
    parser.add_argument("--loop", action="store_true", help="publish continuously")
    parser.add_argument("--anomaly", action="store_true", help="inject anomalous readings")
    args = parser.parse_args()

    client = mqtt.Client(client_id="iot-health-mqtt-publisher", clean_session=True)

    print(f"Connecting to MQTT broker at {BROKER_HOST}:{BROKER_PORT} ...")
    try:
        client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    except Exception as e:
        print(f"ERROR: Could not connect to broker: {e}")
        print("Make sure Mosquitto is running (see docs/mqtt-guide.md).")
        sys.exit(1)

    client.loop_start()

    print(f"Publishing to topic prefix: {TOPIC_PREFIX}/{{deviceCode}}/vitals")
    print(f"Devices: {', '.join(DEVICE_CODES)}")
    print()

    try:
        if args.loop:
            print(f"Loop mode — publishing every {INTERVAL_SECONDS}s. Press Ctrl+C to stop.\n")
            first = True
            while True:
                publish_readings(client, use_anomaly=args.anomaly and first)
                first = False
                time.sleep(INTERVAL_SECONDS)
        else:
            publish_readings(client, use_anomaly=args.anomaly)
            print("\nDone.")
    except KeyboardInterrupt:
        print("\nStopped.")
    finally:
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    main()
