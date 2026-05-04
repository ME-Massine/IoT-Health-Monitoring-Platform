import { useState, useEffect } from "react";
import { patientApi } from "../api/patientApi";
import { vitalSignApi } from "../api/vitalSignApi";

export function usePatients() {
  const [patients, setPatients] = useState([]);
  const [vitals, setVitals] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchData() {
      try {
        setLoading(true);
        setError(null);

        const patientList = await patientApi.getAll();
        if (cancelled) return;
        setPatients(patientList);

        const vitalsResults = await Promise.allSettled(
          patientList.map((p) => vitalSignApi.getLatestByPatientId(p.id))
        );

        if (cancelled) return;

        const vitalsMap = {};
        patientList.forEach((p, i) => {
          const result = vitalsResults[i];
          vitalsMap[p.id] = result.status === "fulfilled" ? result.value : null;
        });
        setVitals(vitalsMap);
      } catch (err) {
        if (!cancelled) setError("Failed to load patients. Is the backend running?");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchData();
    return () => { cancelled = true; };
  }, []);

  return { patients, vitals, loading, error };
}