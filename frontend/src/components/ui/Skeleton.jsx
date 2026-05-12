function Skel({ className = "", style = {} }) {
  return <div className={`skeleton ${className}`} style={style} />;
}

/* ── Dashboard — patient card grid ───────────────────── */
export function PatientCardSkeleton() {
  return (
    <div className="patient-card patient-card--unknown" style={{ gap: "0.6rem" }}>
      <div className="patient-card__header">
        <Skel style={{ width: "55%", height: "1rem" }} />
        <Skel style={{ width: "4.5rem", height: "1.1rem", borderRadius: "4px" }} />
      </div>
      <Skel style={{ width: "35%", height: "0.75rem" }} />
      <Skel style={{ width: "60%", height: "0.75rem" }} />
      <div className="patient-card__vitals" style={{ borderTop: "1px solid #f1f5f9" }}>
        <Skel style={{ width: "5rem", height: "1.5rem", borderRadius: "5px" }} />
        <Skel style={{ width: "5rem", height: "1.5rem", borderRadius: "5px" }} />
        <Skel style={{ width: "4.5rem", height: "1.5rem", borderRadius: "5px" }} />
      </div>
      <Skel style={{ width: "5rem", height: "0.75rem", alignSelf: "flex-end" }} />
    </div>
  );
}

/* ── Patient detail page ──────────────────────────────── */
export function PatientDetailSkeleton() {
  return (
    <section className="patient-detail">
      <Skel style={{ width: "8rem", height: "0.85rem", marginBottom: "1.25rem" }} />

      <div className="patient-detail__header" style={{ marginBottom: "1.25rem" }}>
        <div className="patient-detail__title-row">
          <Skel style={{ width: "14rem", height: "1.4rem" }} />
          <Skel style={{ width: "4.5rem", height: "1.2rem", borderRadius: "4px" }} />
          <Skel style={{ width: "5rem", height: "1.2rem", borderRadius: "4px" }} />
        </div>
        <Skel style={{ width: "12rem", height: "0.85rem", marginTop: "0.4rem" }} />
      </div>

      <div className="vital-cards-row">
        {[0, 1, 2].map((i) => (
          <div key={i} className="vital-card vital-card--unknown">
            <Skel style={{ width: "5rem", height: "0.7rem", marginBottom: "0.4rem" }} />
            <Skel style={{ width: "60%", height: "2rem", marginBottom: "0.4rem" }} />
            <Skel style={{ width: "3.5rem", height: "1rem", borderRadius: "3px" }} />
          </div>
        ))}
      </div>

      <div className="patient-detail__body">
        <div className="patient-detail__charts">
          <Skel style={{ width: "7rem", height: "0.9rem", marginBottom: "1rem" }} />
          {[0, 1, 2].map((i) => (
            <div key={i} className="chart-block">
              <Skel style={{ width: "9rem", height: "0.7rem", marginBottom: "0.4rem" }} />
              <Skel style={{ width: "100%", height: "120px", borderRadius: "6px" }} />
            </div>
          ))}
        </div>

        <div className="patient-detail__alerts">
          <Skel style={{ width: "8rem", height: "0.9rem", marginBottom: "1rem" }} />
          {[0, 1, 2, 3].map((i) => (
            <div key={i} style={{ marginBottom: "0.65rem" }}>
              <Skel style={{ width: "100%", height: "4rem", borderRadius: "8px" }} />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ── Alert center — alert rows ────────────────────────── */
export function AlertRowSkeleton() {
  return (
    <div className="alert-row" style={{ borderLeftColor: "#e2e8f0" }}>
      <Skel style={{ width: "1rem", height: "1rem", borderRadius: "50%", flexShrink: 0 }} />
      <div className="alert-row__body">
        <div className="alert-row__top" style={{ marginBottom: "0.4rem" }}>
          <Skel style={{ width: "4rem", height: "1rem", borderRadius: "3px" }} />
          <Skel style={{ width: "8rem", height: "0.85rem" }} />
        </div>
        <Skel style={{ width: "90%", height: "0.85rem", marginBottom: "0.4rem" }} />
        <div style={{ display: "flex", gap: "0.4rem" }}>
          <Skel style={{ width: "3rem", height: "0.7rem" }} />
          <Skel style={{ width: "6rem", height: "0.7rem" }} />
        </div>
      </div>
      <Skel style={{ width: "4rem", height: "1.8rem", borderRadius: "6px", flexShrink: 0 }} />
    </div>
  );
}

/* ── Devices page — table rows ────────────────────────── */
export function DeviceRowSkeleton() {
  return (
    <tr>
      {[
        "8rem", "5rem", "7rem", "6rem", "7rem", "4.5rem",
      ].map((w, i) => (
        <td key={i} style={{ padding: "0.75rem 1rem" }}>
          <Skel style={{ width: w, height: "0.85rem" }} />
        </td>
      ))}
    </tr>
  );
}
