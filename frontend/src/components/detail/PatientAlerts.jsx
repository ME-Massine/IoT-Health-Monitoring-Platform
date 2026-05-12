import { useState } from "react";
import { alertApi } from "../../api/alertApi";
import { CheckCircle, AlertCircle, AlertTriangle, Clock } from "lucide-react";

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

function SeverityIcon({ severity }) {
  if (severity === "CRITICAL")
    return <AlertCircle size={15} className="tl-icon tl-icon--critical" />;
  return <AlertTriangle size={15} className="tl-icon tl-icon--warning" />;
}

const PAGE_SIZE = 4;

export function PatientAlerts({ alerts, onAlertResolved }) {
  const [resolvingId, setResolvingId] = useState(null);
  const [page, setPage] = useState(0);

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

  const totalPages = Math.ceil(alerts.length / PAGE_SIZE);
  const safePage = Math.min(page, totalPages - 1);
  const pageAlerts = alerts.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  return (
    <div className="alert-timeline">
      {pageAlerts.map((a, i) => (
        <div
          key={a.id}
          className={`tl-item ${a.resolved ? "tl-item--resolved" : ""}`}
        >
          <div className="tl-item__track">
            <SeverityIcon severity={a.severity} />
            {i < pageAlerts.length - 1 && <div className="tl-line" />}
          </div>

          <div className={`tl-card tl-card--${a.severity.toLowerCase()}`}>
            <div className="tl-card__header">
              <span className={`severity-badge severity-badge--${a.severity.toLowerCase()}`}>
                {a.severity}
              </span>
              <span className="tl-card__type">{formatType(a.type)}</span>
              <span className="tl-card__time">
                <Clock size={10} /> {timeAgo(a.createdAt)}
              </span>
            </div>

            <p className="tl-card__message">{a.message}</p>

            <div className="tl-card__footer">
              {a.resolved ? (
                <span className="resolved-tag">
                  <CheckCircle size={11} /> Resolved
                </span>
              ) : (
                <button
                  className="btn btn--resolve"
                  onClick={() => handleResolve(a.id)}
                  disabled={resolvingId === a.id}
                >
                  {resolvingId === a.id ? "Resolving…" : "Resolve"}
                </button>
              )}
            </div>
          </div>
        </div>
      ))}

      {totalPages > 1 && (
        <div className="tl-pagination">
          <button
            className="tl-page-btn"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={safePage === 0}
          >
            ← Prev
          </button>
          <span className="tl-page-info">{safePage + 1} / {totalPages}</span>
          <button
            className="tl-page-btn"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={safePage === totalPages - 1}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
