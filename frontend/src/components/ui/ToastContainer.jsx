import { useEffect } from "react";
import { X, AlertCircle, AlertTriangle, XCircle } from "lucide-react";
import { Link } from "react-router-dom";

const AUTO_DISMISS_MS = { CRITICAL: 8000, WARNING: 5000, ERROR: 6000 };

const ICON_MAP = {
  CRITICAL: AlertCircle,
  ERROR: XCircle,
};

export function ToastContainer({ toasts, onDismiss }) {
  if (toasts.length === 0) return null;
  return (
    <div className="toast-stack">
      {toasts.map((t) => (
        <Toast key={t.id} toast={t} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

function Toast({ toast, onDismiss }) {
  useEffect(() => {
    const ms = AUTO_DISMISS_MS[toast.severity] ?? 5000;
    const timer = setTimeout(() => onDismiss(toast.id), ms);
    return () => clearTimeout(timer);
  }, [toast.id, toast.severity, onDismiss]);

  const Icon = ICON_MAP[toast.severity] ?? AlertTriangle;
  const cls = toast.severity.toLowerCase();

  return (
    <div className={`toast toast--${cls}`}>
      <Icon size={15} className="toast__icon" />
      <div className="toast__body">
        <span className="toast__message">{toast.message}</span>
        {toast.patientName && (
          <Link to={`/patients/${toast.patientId}`} className="toast__patient">
            {toast.patientName}
          </Link>
        )}
      </div>
      <button className="toast__close" onClick={() => onDismiss(toast.id)} aria-label="Dismiss">
        <X size={13} />
      </button>
    </div>
  );
}
