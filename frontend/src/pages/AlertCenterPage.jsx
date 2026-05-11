import { Link } from "react-router-dom";
import { useState } from "react";
import { useAlerts } from "../hooks/useAlerts";
import { alertApi } from "../api/alertApi";
import { AlertCircle, AlertTriangle, CheckCircle, Clock } from "lucide-react";

const FILTERS = [
  { key: "unresolved", label: "Unresolved" },
  { key: "critical",   label: "Critical"   },
  { key: "warning",    label: "Warnings"   },
  { key: "all",        label: "All"        },
];

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function formatType(type) {
  return type.replace(/_/g, " ");
}

export function AlertCenterPage() {
  const { alerts, filter, setFilter, loading, error, handleAlertResolved } = useAlerts();
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

  const criticalCount = alerts.filter((a) => !a.resolved && a.severity === "CRITICAL").length;
  const warningCount  = alerts.filter((a) => !a.resolved && a.severity === "WARNING").length;
  const resolvedCount = alerts.filter((a) => a.resolved).length;

  return (
    <section className="alert-center">
      <h2 className="page-title">Alert Center</h2>

      <div className="alert-summary-bar">
        <div className="alert-summary-card alert-summary-card--critical">
          <AlertCircle size={18} />
          <span className="alert-summary-card__count">{criticalCount}</span>
          <span className="alert-summary-card__label">Critical</span>
        </div>
        <div className="alert-summary-card alert-summary-card--warning">
          <AlertTriangle size={18} />
          <span className="alert-summary-card__count">{warningCount}</span>
          <span className="alert-summary-card__label">Warnings</span>
        </div>
        <div className="alert-summary-card alert-summary-card--resolved">
          <CheckCircle size={18} />
          <span className="alert-summary-card__count">{resolvedCount}</span>
          <span className="alert-summary-card__label">Resolved</span>
        </div>
      </div>

      <div className="alert-center__filters">
        {FILTERS.map((f) => (
          <button
            key={f.key}
            className={`filter-btn ${filter === f.key ? "filter-btn--active" : ""}`}
            onClick={() => setFilter(f.key)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {loading && <p className="loading-text">Loading alerts…</p>}
      {error   && <p className="error">{error}</p>}

      {!loading && !error && alerts.length === 0 && (
        <p className="no-data">No alerts found for this filter.</p>
      )}

      {!loading && !error && alerts.length > 0 && (
        <div className="alert-center__list">
          {alerts.map((a) => (
            <div
              key={a.id}
              className={`alert-row alert-row--${a.severity.toLowerCase()} ${a.resolved ? "alert-row--resolved" : ""}`}
            >
              <div className="alert-row__icon">
                {a.severity === "CRITICAL"
                  ? <AlertCircle size={16} className="icon--critical" />
                  : <AlertTriangle size={16} className="icon--warning" />}
              </div>

              <div className="alert-row__body">
                <div className="alert-row__top">
                  <span className={`severity-badge severity-badge--${a.severity.toLowerCase()}`}>
                    {a.severity}
                  </span>
                  <span className="alert-row__type">{formatType(a.type)}</span>
                  {a.resolved && (
                    <span className="resolved-tag">
                      <CheckCircle size={11} /> Resolved
                    </span>
                  )}
                </div>

                <p className="alert-row__message">{a.message}</p>

                <div className="alert-row__meta">
                  <Clock size={11} />
                  <span className="alert-row__time">{timeAgo(a.createdAt)}</span>
                  {a.patientFullName && (
                    <Link to={`/patients/${a.patientId}`} className="alert-row__patient-link">
                      {a.patientFullName}
                    </Link>
                  )}
                </div>
              </div>

              {!a.resolved && (
                <button
                  className="btn btn--resolve"
                  onClick={() => handleResolve(a.id)}
                  disabled={resolvingId === a.id}
                >
                  {resolvingId === a.id ? "…" : "Resolve"}
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
