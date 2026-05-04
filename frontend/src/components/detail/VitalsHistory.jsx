export function VitalsHistory({ vitals }) {
  if (!vitals || vitals.length === 0) {
    return <p className="no-data">No vitals history recorded.</p>;
  }

  return (
    <div className="vitals-history">
      <table className="vitals-table">
        <thead>
          <tr>
            <th>Recorded At</th>
            <th>Heart Rate (bpm)</th>
            <th>Temperature (°C)</th>
            <th>SpO2 (%)</th>
          </tr>
        </thead>
        <tbody>
          {vitals.map((v) => (
            <tr key={v.id}>
              <td>{new Date(v.recordedAt).toLocaleString()}</td>
              <td>{v.heartRate ?? "—"}</td>
              <td>{v.temperature ?? "—"}</td>
              <td>{v.spo2 ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}