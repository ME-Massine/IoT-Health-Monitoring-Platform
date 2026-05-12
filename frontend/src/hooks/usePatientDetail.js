import { useState, useEffect } from "react";
import { patientApi } from "../api/patientApi";
import { vitalSignApi } from "../api/vitalSignApi";
import { alertApi } from "../api/alertApi";
import { deviceApi } from "../api/deviceApi";
import { usePatientVitalsSocket } from "./usePatientVitalsSocket";
import { usePatientAlertsSocket } from "./usePatientAlertsSocket";

export function usePatientDetail(patientId, rangeHours = null) {
  const [patient, setPatient] = useState(null);
  const [vitalsHistory, setVitalsHistory] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [device, setDevice] = useState(null);
  const [maintenanceWindows, setMaintenanceWindows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) return;
    let cancelled = false;

    async function fetchAll() {
      try {
        setLoading(true);
        setError(null);

        const historyParams = rangeHours
          ? { limit: 500, from: new Date(Date.now() - rangeHours * 3600_000), to: new Date() }
          : { limit: 20 };

        const [patientData, historyData, alertsData] = await Promise.all([
          patientApi.getById(patientId),
          vitalSignApi.getHistoryByPatientId(patientId, historyParams),
          alertApi.getByPatientId(patientId),
        ]);

        if (cancelled) return;
        setPatient(patientData);
        setVitalsHistory(historyData);
        setAlerts(alertsData);

        // Fetch device + maintenance windows independently (non-blocking)
        deviceApi.getByPatientId(patientId)
          .then((dev) => {
            if (cancelled) return;
            setDevice(dev);
            return deviceApi.getMaintenanceWindows(dev.id);
          })
          .then((windows) => {
            if (!cancelled && windows) setMaintenanceWindows(windows);
          })
          .catch(() => {});
      } catch {
        if (!cancelled) setError("Failed to load patient data.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchAll();
    return () => { cancelled = true; };
  }, [patientId, rangeHours]);

  // Prepend new vitals from WebSocket (cap at 500 to avoid unbounded growth)
  usePatientVitalsSocket(patientId, (vital) => {
    setVitalsHistory((prev) => [vital, ...prev.slice(0, 499)]);
  });

  // Prepend new alerts from WebSocket
  usePatientAlertsSocket(patientId, (alert) => {
    setAlerts((prev) => {
      const exists = prev.some((a) => a.id === alert.id);
      return exists ? prev : [alert, ...prev];
    });
  });

  function handleAlertResolved(updatedAlert) {
    setAlerts((prev) =>
      prev.map((a) => (a.id === updatedAlert.id ? updatedAlert : a))
    );
  }

  return { patient, vitalsHistory, alerts, device, maintenanceWindows, loading, error, handleAlertResolved };
}