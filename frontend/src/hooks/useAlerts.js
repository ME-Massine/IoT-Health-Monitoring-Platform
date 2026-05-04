import { useState, useEffect } from "react";
import { alertApi } from "../api/alertApi";

export function useAlerts() {
  const [alerts, setAlerts] = useState([]);
  const [filter, setFilter] = useState("unresolved"); // "unresolved" | "all"
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchAlerts() {
      try {
        setLoading(true);
        setError(null);

        const data =
          filter === "unresolved"
            ? await alertApi.getUnresolved()
            : await alertApi.getAll();

        if (!cancelled) setAlerts(data);
      } catch {
        if (!cancelled) setError("Failed to load alerts.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchAlerts();
    return () => { cancelled = true; };
  }, [filter]);

  function handleAlertResolved(updatedAlert) {
    setAlerts((prev) =>
      prev.map((a) => (a.id === updatedAlert.id ? updatedAlert : a))
    );
  }

  return { alerts, filter, setFilter, loading, error, handleAlertResolved };
}