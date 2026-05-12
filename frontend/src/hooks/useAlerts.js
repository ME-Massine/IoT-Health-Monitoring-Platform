import { useState, useEffect } from "react";
import { alertApi } from "../api/alertApi";
import { useGlobalAlertsSocket } from "./useGlobalAlertsSocket";

export function useAlerts() {
  const [allAlerts, setAllAlerts] = useState([]);
  const [filter, setFilter] = useState("unresolved");
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
        if (!cancelled) setAllAlerts(data);
      } catch {
        if (!cancelled) setError("Failed to load alerts.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchAlerts();
    return () => { cancelled = true; };
  }, [filter]);

  // Prepend live alerts from WebSocket when showing active filters
  useGlobalAlertsSocket((incoming) => {
    const isActiveFilter = filter === "unresolved" || filter === "critical" || filter === "warning";
    if (isActiveFilter) {
      setAllAlerts((prev) => {
        const exists = prev.some((a) => a.id === incoming.id);
        return exists ? prev : [incoming, ...prev];
      });
    }
  });

  function handleAlertResolved(updatedAlert) {
    setAllAlerts((prev) =>
      prev.map((a) => (a.id === updatedAlert.id ? updatedAlert : a))
    );
  }

  // Client-side filter for severity-specific views
  const alerts = (() => {
    if (filter === "critical")
      return allAlerts.filter((a) => !a.resolved && a.severity === "CRITICAL");
    if (filter === "warning")
      return allAlerts.filter((a) => !a.resolved && a.severity === "WARNING");
    return allAlerts;
  })();

  return { alerts, allAlerts, filter, setFilter, loading, error, handleAlertResolved };
}
