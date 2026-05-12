import { useState, useEffect, useMemo } from "react";
import { Search, Users, SearchX } from "lucide-react";
import { usePatients } from "../hooks/usePatients";
import { PatientCard } from "../components/dashboard/PatientCard";
import { KpiStrip } from "../components/dashboard/KpiStrip";
import { PatientCardSkeleton } from "../components/ui/Skeleton";
import { EmptyState } from "../components/ui/EmptyState";
import { getPatientStatus, STATUS_ORDER } from "../utils/vitalStatus";
import { alertApi } from "../api/alertApi";
import { deviceApi } from "../api/deviceApi";

const FILTERS = ["all", "critical", "warning", "stable"];

export function DashboardPage() {
  const { patients, vitals, loading, error } = usePatients();
  const [statusFilter, setStatusFilter] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
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

  const sorted = useMemo(() => {
    return [...patients].sort((a, b) => {
      const sa = STATUS_ORDER[getPatientStatus(vitals[a.id])] ?? 3;
      const sb = STATUS_ORDER[getPatientStatus(vitals[b.id])] ?? 3;
      return sa - sb;
    });
  }, [patients, vitals]);

  const filterCounts = useMemo(() => {
    const counts = { all: patients.length, critical: 0, warning: 0, stable: 0 };
    patients.forEach((p) => {
      const s = getPatientStatus(vitals[p.id]);
      if (counts[s] !== undefined) counts[s] += 1;
    });
    return counts;
  }, [patients, vitals]);

  if (loading) {
    return (
      <section className="dashboard">
        <div className="dashboard__header">
          <h2 className="page-title">Patient Dashboard</h2>
        </div>
        <div className="patient-grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <PatientCardSkeleton key={i} />
          ))}
        </div>
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

  const search = searchTerm.trim().toLowerCase();
  const filtered = sorted.filter((p) => {
    const matchesStatus =
      statusFilter === "all" || getPatientStatus(vitals[p.id]) === statusFilter;
    if (!matchesStatus) return false;
    if (!search) return true;
    const fullName = `${p.firstName} ${p.lastName}`.toLowerCase();
    return (
      fullName.includes(search) ||
      String(p.roomNumber ?? "").toLowerCase().includes(search) ||
      (p.medicalCondition ?? "").toLowerCase().includes(search)
    );
  });

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

      <div className="dashboard__controls">
        <div className="dashboard__filters">
          {FILTERS.map((f) => (
            <button
              key={f}
              className={`filter-btn ${statusFilter === f ? "filter-btn--active" : ""}`}
              onClick={() => setStatusFilter(f)}
            >
              {f.charAt(0).toUpperCase() + f.slice(1)}
              <span className="filter-btn__count">{filterCounts[f]}</span>
            </button>
          ))}
        </div>

        <div className="search-input">
          <Search size={14} className="search-input__icon" />
          <input
            type="text"
            placeholder="Search patients, rooms, conditions…"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button className="search-input__clear" onClick={() => setSearchTerm("")} aria-label="Clear search">
              ×
            </button>
          )}
        </div>
      </div>

      {filtered.length === 0 ? (
        search ? (
          <EmptyState
            icon={SearchX}
            title="No patients match your search"
            subtitle={`Nothing found for "${searchTerm}" in the ${statusFilter} filter.`}
          />
        ) : (
          <EmptyState
            icon={Users}
            title="No patients in this filter"
            subtitle="Try switching to All to see everyone."
          />
        )
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
