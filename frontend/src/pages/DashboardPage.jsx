import { useState, useEffect } from "react";
import { usePatients } from "../hooks/usePatients";
import { PatientCard } from "../components/dashboard/PatientCard";
import { KpiStrip } from "../components/dashboard/KpiStrip";
import { getPatientStatus, STATUS_ORDER } from "../utils/vitalStatus";
import { alertApi } from "../api/alertApi";
import { deviceApi } from "../api/deviceApi";

const FILTERS = ["all", "critical", "warning", "stable"];

export function DashboardPage() {
  const { patients, vitals, loading, error } = usePatients();
  const [statusFilter, setStatusFilter] = useState("all");
  const [criticalCount, setCriticalCount] = useState(0);
  const [warningCount, setWarningCount] = useState(0);
  const [devicesOnline, setDevicesOnline] = useState(0);
  const [deviceStatusMap, setDeviceStatusMap] = useState({});

  useEffect(() => {
    alertApi.getUnresolved().then((alerts) => {
      setCriticalCount(alerts.filter((a) => a.severity === "CRITICAL").length);
      setWarningCount(alerts.filter((a) => a.severity === "WARNING").length);
    }).catch(() => {});

    deviceApi.getAll().then((devices) => {
      setDevicesOnline(devices.filter((d) => d.status === "ACTIVE").length);
      const map = {};
      devices.forEach((d) => { if (d.patientId) map[d.patientId] = d.status; });
      setDeviceStatusMap(map);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    function onDeviceStatusChanged(e) {
      const device = e.detail;
      if (device.patientId != null) {
        setDeviceStatusMap((prev) => ({ ...prev, [device.patientId]: device.status }));
      }
    }
    window.addEventListener("device-status-changed", onDeviceStatusChanged);
    return () => window.removeEventListener("device-status-changed", onDeviceStatusChanged);
  }, []);

  if (loading) {
    return (
      <section className="dashboard">
        <h2 className="page-title">Patient Dashboard</h2>
        <p className="loading-text">Loading patients…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="dashboard">
        <h2 className="page-title">Patient Dashboard</h2>
        <p className="error">{error}</p>
      </section>
    );
  }

  const sorted = [...patients].sort((a, b) => {
    const sa = STATUS_ORDER[getPatientStatus(vitals[a.id])] ?? 3;
    const sb = STATUS_ORDER[getPatientStatus(vitals[b.id])] ?? 3;
    return sa - sb;
  });

  const filtered =
    statusFilter === "all"
      ? sorted
      : sorted.filter((p) => getPatientStatus(vitals[p.id]) === statusFilter);

  return (
    <section className="dashboard">
      <div className="dashboard__header">
        <h2 className="page-title">Patient Dashboard</h2>
        <span className="dashboard__subtitle">
          Live monitoring · {patients.length} patient{patients.length !== 1 ? "s" : ""}
        </span>
      </div>

      <KpiStrip
        patientCount={patients.length}
        criticalCount={criticalCount}
        warningCount={warningCount}
        devicesOnline={devicesOnline}
      />

      <div className="dashboard__filters">
        {FILTERS.map((f) => (
          <button
            key={f}
            className={`filter-btn ${statusFilter === f ? "filter-btn--active" : ""}`}
            onClick={() => setStatusFilter(f)}
          >
            {f.charAt(0).toUpperCase() + f.slice(1)}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <p className="no-data">No patients match the selected filter.</p>
      ) : (
        <div className="patient-grid">
          {filtered.map((patient) => (
            <PatientCard
              key={patient.id}
              patient={patient}
              vitals={vitals[patient.id]}
              deviceStatus={deviceStatusMap[patient.id]}
            />
          ))}
        </div>
      )}
    </section>
  );
}
