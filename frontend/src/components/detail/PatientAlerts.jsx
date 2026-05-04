import { useState } from "react";
import { alertApi } from "../../api/alertApi";

export function PatientAlerts({ alerts, onAlertResolved }) {
  const [resolvingId, setResolvingId] = useState(null);

  async function handleResolve(alertId) {
    setResolvingId(alertId);
    try {
      const updated = await alertApi.resolve(alertId);
      onAlertResolved(updated);
    } catch {
      alert("Failed to resolve alert. Please try again.");
    } finally {
      setResolvingId(null);
    }
  }

  if (!alerts || alerts.length === 0) {
    return <p className="no-data">No alerts for this patient.</p>;
  }

  return (
    <div className="alerts-list">
      {alerts.map((a) => (
        <div
          key={a.id}
          className={`alert-item alert-item--${a.severity.toLowerCase()} ${a.resolved ? "alert-item--resolved" : ""}`}
        >
          <div className="alert-item__header">
            <span className="alert-item__type">{a.type}</span>
            <span className="alert-item__severity">{a.severity}</span>
            {a.resolved && <span className="alert-item__badge">Resolved</span>}
          </div>
          <p className="alert-item__message">{a.message}</p>
          <div className="alert-item__footer">
            <span className="alert-item__time">
              {new Date(a.createdAt).toLocaleString()}
            </span>
            {!a.resolved && (
              <button
                className="btn btn--small"
                onClick={() => handleResolve(a.id)}
                disabled={resolvingId === a.id}
              >
                {resolvingId === a.id ? "Resolving…" : "Resolve"}
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}