import { Link } from "react-router-dom";
import { useState } from "react";
import { useAlerts } from "../hooks/useAlerts";
import { alertApi } from "../api/alertApi";
import { AlertCircle, AlertTriangle, CheckCircle, Clock, BellOff, Eye, Trash2 } from "lucide-react";
import { AlertRowSkeleton } from "../components/ui/Skeleton";
import { EmptyState } from "../components/ui/EmptyState";
import { formatTimeAgo } from "../utils/time";

const FILTERS = [
  { key: "unresolved", label: "Unresolved" },
  { key: "critical",   label: "Critical"   },
  { key: "warning",    label: "Warnings"   },
  { key: "all",        label: "All"        },
];

function formatType(type) {
  return type.replace(/_/g, " ");
}

export function AlertCenterPage() {
  const { alerts, allAlerts, filter, setFilter, loading, error, handleAlertResolved, handleAlertUpdated, handleAlertDismissed } = useAlerts();
  const [resolvingId, setResolvingId] = useState(null);
  const [acknowledgingId, setAcknowledgingId] = useState(null);
  const [dismissingId, setDismissingId] = useState(null);

  async function handleResolve(alertId) {
    setResolvingId(alertId);
    try {
      const updated = await alertApi.resolve(alertId);
      handleAlertResolved(updated);
      window.dispatchEvent(new CustomEvent("alert-resolved"));
    } finally {
      setResolvingId(null);
    }
  }

  async function handleAcknowledge(alertId) {
    setAcknowledgingId(alertId);
    try {
      const updated = await alertApi.acknowledge(alertId);
      handleAlertUpdated(updated);
    } finally {
      setAcknowledgingId(null);
    }
  }

  async function handleDismiss(alertId) {
    setDismissingId(alertId);
    try {
      await alertApi.dismiss(alertId);
      handleAlertDismissed(alertId);
    } finally {
      setDismissingId(null);
    }
  }

  // Always compute summary counts from the full list so they don't change with the filter
  const criticalCount = allAlerts.filter((a) => !a.resolved && a.severity === "CRITICAL").length;
  const warningCount  = allAlerts.filter((a) => !a.resolved && a.severity === "WARNING").length;
  const resolvedCount = allAlerts.filter((a) => a.resolved).length;
  const unresolvedCount = allAlerts.filter((a) => !a.resolved).length;

  const filterCounts = {
    unresolved: unresolvedCount,
    critical:   criticalCount,
    warning:    warningCount,
    all:        allAlerts.length,
  };

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
            <span className="filter-btn__count">{filterCounts[f.key] ?? 0}</span>
          </button>
        ))}
      </div>

      {loading && (
        <div className="alert-center__list">
          {Array.from({ length: 5 }).map((_, i) => <AlertRowSkeleton key={i} />)}
        </div>
      )}
      {!loading && error && <p className="error">{error}</p>}

      {!loading && !error && alerts.length === 0 && (
        <EmptyState
          icon={BellOff}
          title={filter === "unresolved" ? "All clear" : "No alerts in this view"}
          subtitle={
            filter === "unresolved"
              ? "No active alerts right now. The system will notify you when one arrives."
              : "Try switching to another filter to see more alerts."
          }
        />
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
                  <span className="alert-row__time">{formatTimeAgo(a.createdAt)}</span>
                  {a.patientFullName && (
                    <Link to={`/patients/${a.patientId}`} className="alert-row__patient-link">
                      {a.patientFullName}
                    </Link>
                  )}
                  {a.acknowledgedAt && !a.resolved && (
                    <span className="acknowledged-tag">
                      <Eye size={11} /> Acknowledged
                    </span>
                  )}
                </div>
              </div>

              <div className="alert-row__actions">
                {!a.resolved && !a.acknowledgedAt && (
                  <button
                    className="btn btn--acknowledge"
                    onClick={() => handleAcknowledge(a.id)}
                    disabled={acknowledgingId === a.id}
                    title="Acknowledge"
                  >
                    <Eye size={13} />
                    {acknowledgingId === a.id ? "…" : "Ack"}
                  </button>
                )}
                {!a.resolved && (
                  <button
                    className="btn btn--resolve"
                    onClick={() => handleResolve(a.id)}
                    disabled={resolvingId === a.id}
                  >
                    {resolvingId === a.id ? "…" : "Resolve"}
                  </button>
                )}
                <button
                  className="btn btn--dismiss"
                  onClick={() => handleDismiss(a.id)}
                  disabled={dismissingId === a.id}
                  title="Dismiss alert"
                >
                  <Trash2 size={13} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
