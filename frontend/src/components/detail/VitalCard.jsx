export function VitalCard({ label, value, unit, status }) {
  const statusLabel = status === 'normal' ? 'Normal' : status.charAt(0).toUpperCase() + status.slice(1);

  return (
    <div className={`vital-card vital-card--${status}`}>
      <div className="vital-card__label">{label}</div>
      <div className="vital-card__value">
        {value != null ? value : "—"}
        {value != null && <span className="vital-card__unit"> {unit}</span>}
      </div>
      <span className={`vital-card__status-tag vital-card__status-tag--${status}`}>
        {statusLabel}
      </span>
    </div>
  );
}
