export function VitalCard({ label, value, unit, status, delta }) {
  const statusLabel = status === 'normal' ? 'Normal' : status.charAt(0).toUpperCase() + status.slice(1);

  const deltaDisplay = (() => {
    if (delta == null || value == null) return null;
    const diff = parseFloat((value - delta).toFixed(1));
    if (diff === 0) return { symbol: "↔", cls: "neutral", text: "±0" };
    return diff > 0
      ? { symbol: "▲", cls: "up", text: `+${diff}` }
      : { symbol: "▼", cls: "down", text: `${diff}` };
  })();

  return (
    <div className={`vital-card vital-card--${status}`}>
      <div className="vital-card__label">{label}</div>
      <div className="vital-card__value">
        {value != null ? value : "—"}
        {value != null && <span className="vital-card__unit"> {unit}</span>}
        {deltaDisplay && (
          <span className={`vital-card__delta vital-card__delta--${deltaDisplay.cls}`}>
            {deltaDisplay.symbol} {deltaDisplay.text}
          </span>
        )}
      </div>
      <span className={`vital-card__status-tag vital-card__status-tag--${status}`}>
        {statusLabel}
      </span>
    </div>
  );
}
