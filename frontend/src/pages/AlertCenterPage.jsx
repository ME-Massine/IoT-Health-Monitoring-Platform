import { Link } from "react-router-dom";
import { useAlerts } from "../hooks/useAlerts";
import { alertApi } from "../api/alertApi";
import { useState } from "react";

export function AlertCenterPage() {
  const { alerts, filter, setFilter, loading, error, handleAlertResolved } =
    useAlerts();
  const [resolvingId, setResolvingId] = useState(null);

  async function handleResolve(alertId) {
    setResolvingId(alertId);
    try {
      const updated = await alertApi.resolve(alertId);
      handleAlertResolved(updated);
    } catch {
      alert("Failed to resolve alert. Please try again.");
    } finally {
      setResolvingId(null);
    }
  }

  const unresolvedCount = alerts.filter((a) => !a.resolved).length;

  return (
    <section className="alert-center">
      <div className="alert-center__header">
        <h2>
          Alert Center{" "}
          {!loading && unresolvedCount > 0 && (
            <span className="badge badge--warning">{unresolvedCount} unresolved</span>
          )}
        </h2>

        <div className="alert-center__filters">
          <button
            className={`filter-btn ${filter === "unresolved" ? "filter-btn--active" : ""}`}
            onClick={() => setFilter("unresolved")}
          >
            Unresolved
          </button>
          <button
            className={`filter-btn ${filter === "all" ? "filter-btn--active" : ""}`}
            onClick={() => setFilter("all")}
          >
            All
          </button>
        </div>
      </div>

      {loading && <p>Loading alerts…</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && alerts.length === 0 && (
        <p className="no-data">
          {filter === "unresolved" ? "No unresolved alerts." : "No alerts found."}
        </p>
      )}

      {!loading && !error && alerts.length > 0 && (
        <div className="alert-center__list">
          {alerts.map((a) => (
            <div
              key={a.id}
              className={`alert-item alert-item--${a.severity.toLowerCase()} ${a.resolved ? "alert-item--resolved" : ""}`}
            >
              <div className="alert-item__header">
                <span className="alert-item__type">{a.type}</span>
                <span className="alert-item__severity">{a.severity}</span>
                {a.resolved && (
                  <span className="alert-item__badge">Resolved</span>
                )}
              </div>

              <p className="alert-item__message">{a.message}</p>

              <div className="alert-item__footer">
                <div className="alert-item__meta">
                  <span className="alert-item__time">
                    {new Date(a.createdAt).toLocaleString()}
                  </span>
                  {a.patientFullName && (
                    <Link
                      to={`/patients/${a.patientId}`}
                      className="alert-item__patient-link"
                    >
                      {a.patientFullName}
                    </Link>
                  )}
                </div>

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
      )}
    </section>
  );
}