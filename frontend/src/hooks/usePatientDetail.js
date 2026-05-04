import { useState, useEffect } from "react";
import { patientApi } from "../api/patientApi";
import { vitalSignApi } from "../api/vitalSignApi";
import { alertApi } from "../api/alertApi";

export function usePatientDetail(patientId) {
  const [patient, setPatient] = useState(null);
  const [vitalsHistory, setVitalsHistory] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!patientId) return;
    let cancelled = false;

    async function fetchAll() {
      try {
        setLoading(true);
        setError(null);

        const [patientData, historyData, alertsData] = await Promise.all([
          patientApi.getById(patientId),
          vitalSignApi.getHistoryByPatientId(patientId, 20),
          alertApi.getByPatientId(patientId),
        ]);

        if (cancelled) return;
        setPatient(patientData);
        setVitalsHistory(historyData);
        setAlerts(alertsData);
      } catch (err) {
        if (!cancelled) setError("Failed to load patient data.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchAll();
    return () => { cancelled = true; };
  }, [patientId]);

  function handleAlertResolved(updatedAlert) {
    setAlerts((prev) =>
      prev.map((a) => (a.id === updatedAlert.id ? updatedAlert : a))
    );
  }

  return { patient, vitalsHistory, alerts, loading, error, handleAlertResolved };
}