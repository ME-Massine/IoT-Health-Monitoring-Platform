import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Cpu, Wrench, Wifi, WifiOff, PowerOff } from "lucide-react";
import { deviceApi } from "../api/deviceApi";

const STATUS_META = {
  ACTIVE:      { label: "Active",      cls: "device-status--active",      Icon: Wifi },
  MAINTENANCE: { label: "Maintenance", cls: "device-status--maintenance",  Icon: Wrench },
  INACTIVE:    { label: "Inactive",    cls: "device-status--inactive",     Icon: PowerOff },
};

function StatusBadge({ status }) {
  const { label, cls, Icon } = STATUS_META[status] ?? STATUS_META.INACTIVE;
  return (
    <span className={`device-status-badge ${cls}`}>
      <Icon size={11} />
      {label}
    </span>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString([], {
    month: "short", day: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

export function DevicesPage() {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [togglingId, setTogglingId] = useState(null);

  useEffect(() => {
    deviceApi.getAll()
      .then(setDevices)
      .catch(() => setError("Failed to load devices."))
      .finally(() => setLoading(false));
  }, []);

  async function handleToggle(device) {
    const next = device.status === "ACTIVE" ? "MAINTENANCE"
      : device.status === "MAINTENANCE" ? "ACTIVE"
      : "ACTIVE";

    setTogglingId(device.id);
    try {
      const updated = await deviceApi.setStatus(device.id, next);
      setDevices((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      window.dispatchEvent(new CustomEvent("device-status-changed", { detail: updated }));
    } catch {
      alert("Failed to update device status.");
    } finally {
      setTogglingId(null);
    }
  }

  const maintenanceCount = devices.filter((d) => d.status === "MAINTENANCE").length;
  const inactiveCount    = devices.filter((d) => d.status === "INACTIVE").length;
  const activeCount      = devices.filter((d) => d.status === "ACTIVE").length;

  return (
    <section className="devices-page">
      <h2 className="page-title">Devices</h2>

      <div className="devices-kpi-bar">
        <div className="devices-kpi devices-kpi--active">
          <Wifi size={16} />
          <span className="devices-kpi__count">{activeCount}</span>
          <span className="devices-kpi__label">Active</span>
        </div>
        <div className="devices-kpi devices-kpi--maintenance">
          <Wrench size={16} />
          <span className="devices-kpi__count">{maintenanceCount}</span>
          <span className="devices-kpi__label">Maintenance</span>
        </div>
        <div className="devices-kpi devices-kpi--inactive">
          <WifiOff size={16} />
          <span className="devices-kpi__count">{inactiveCount}</span>
          <span className="devices-kpi__label">Inactive</span>
        </div>
      </div>

      {loading && <p className="loading-text">Loading devices…</p>}
      {error   && <p className="error">{error}</p>}

      {!loading && !error && (
        <div className="devices-table-wrap">
          <table className="devices-table">
            <thead>
              <tr>
                <th>Device Code</th>
                <th>Type</th>
                <th>Patient</th>
                <th>Status</th>
                <th>Last Updated</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {devices.map((d) => (
                <tr
                  key={d.id}
                  className={`devices-table__row ${d.status === "MAINTENANCE" ? "devices-table__row--maintenance" : ""} ${d.status === "INACTIVE" ? "devices-table__row--inactive" : ""}`}
                >
                  <td className="devices-table__code">
                    <Cpu size={13} className="devices-table__icon" />
                    {d.deviceCode}
                  </td>
                  <td>{d.type}</td>
                  <td>
                    {d.patientId ? (
                      <Link to={`/patients/${d.patientId}`} className="devices-table__patient-link">
                        {d.patientFullName ?? `Patient #${d.patientId}`}
                      </Link>
                    ) : "—"}
                  </td>
                  <td><StatusBadge status={d.status} /></td>
                  <td className="devices-table__time">{formatDate(d.updatedAt)}</td>
                  <td>
                    {d.status !== "INACTIVE" && (
                      <button
                        className={`btn devices-table__toggle-btn ${d.status === "MAINTENANCE" ? "btn--activate" : "btn--maintenance"}`}
                        onClick={() => handleToggle(d)}
                        disabled={togglingId === d.id}
                      >
                        {togglingId === d.id
                          ? "…"
                          : d.status === "MAINTENANCE"
                          ? "Set Active"
                          : "Set Maintenance"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
