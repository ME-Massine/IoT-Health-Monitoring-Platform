import { useParams, Link } from "react-router-dom";
import { useState } from "react";
import { Wrench } from "lucide-react";
import { usePatientDetail } from "../hooks/usePatientDetail";
import { VitalCard } from "../components/detail/VitalCard";
import { VitalChart } from "../components/detail/VitalChart";
import { PatientAlerts } from "../components/detail/PatientAlerts";
import { PatientDetailSkeleton } from "../components/ui/Skeleton";
import {
  getHeartRateStatus,
  getTemperatureStatus,
  getSpo2Status,
  getPatientStatus,
} from "../utils/vitalStatus";

const HR_REFS = [
  { value: 110, color: "#f59e0b" },
  { value: 120, color: "#dc2626" },
  { value: 50,  color: "#dc2626" },
];
const TEMP_REFS = [
  { value: 37.8, color: "#f59e0b" },
  { value: 38.0, color: "#dc2626" },
  { value: 35.0, color: "#dc2626" },
];
const SPO2_REFS = [
  { value: 94, color: "#f59e0b" },
  { value: 92, color: "#dc2626" },
];

const STATUS_LABEL = {
  critical: "CRITICAL",
  warning: "WARNING",
  stable: "STABLE",
  unknown: "NO DATA",
};

const RANGE_OPTIONS = [
  { label: "Live", hours: null },
  { label: "1h",   hours: 1  },
  { label: "6h",   hours: 6  },
  { label: "24h",  hours: 24 },
];

export function PatientDetailPage() {
  const { patientId } = useParams();
  const [rangeHours, setRangeHours] = useState(null);
  const { patient, vitalsHistory, alerts, device, maintenanceWindows, loading, error, handleAlertResolved } =
    usePatientDetail(patientId, rangeHours);

  if (loading) return <PatientDetailSkeleton />;

  if (error) {
    return (
      <section>
        <p className="error">{error}</p>
        <Link to="/">← Back to dashboard</Link>
      </section>
    );
  }

  if (!patient) return null;

  const fullName = `${patient.firstName} ${patient.lastName}`;
  const latest = vitalsHistory[0] ?? null;
  const previous = vitalsHistory[1] ?? null;
  const status = getPatientStatus(latest);
  const unresolvedCount = alerts.filter((a) => !a.resolved).length;

  return (
    <section className="patient-detail">
      <div className="patient-detail__back">
        <Link to="/">← Back to dashboard</Link>
      </div>

      <div className="patient-detail__header">
        <div className="patient-detail__title-row">
          <h2 className="patient-detail__name">{fullName}</h2>
          <span className="patient-detail__room-tag">Room {patient.roomNumber}</span>
          {device && device.status !== "ACTIVE" ? (
            <span className="status-badge status-badge--offline">
              <Wrench size={11} /> {device.status}
            </span>
          ) : (
            <span className={`status-badge status-badge--${status}`}>
              {STATUS_LABEL[status]}
            </span>
          )}
        </div>
        <div className="patient-detail__meta">
          {patient.age} yrs · {patient.gender}
          {patient.medicalCondition && <span> · {patient.medicalCondition}</span>}
        </div>
      </div>

      <div className="vital-cards-row">
        <VitalCard
          label="Heart Rate"
          value={latest?.heartRate}
          unit="bpm"
          status={getHeartRateStatus(latest?.heartRate)}
          delta={previous?.heartRate}
        />
        <VitalCard
          label="Temperature"
          value={latest?.temperature != null ? parseFloat(latest.temperature) : null}
          unit="°C"
          status={getTemperatureStatus(latest?.temperature)}
          delta={previous?.temperature != null ? parseFloat(previous.temperature) : null}
        />
        <VitalCard
          label="SpO2"
          value={latest?.spo2}
          unit="%"
          status={getSpo2Status(latest?.spo2)}
          delta={previous?.spo2}
        />
      </div>

      <div className="patient-detail__body">
        <div className="patient-detail__charts">
          <div className="section-title-row">
            <h3 className="section-title">Vital Trends</h3>
            <div className="range-picker">
              {RANGE_OPTIONS.map((opt) => (
                <button
                  key={opt.label}
                  className={`range-btn ${rangeHours === opt.hours ? "range-btn--active" : ""}`}
                  onClick={() => setRangeHours(opt.hours)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          <div className="chart-block">
            <div className="chart-block__label">Heart Rate (bpm)</div>
            <VitalChart
              vitals={vitalsHistory}
              dataKey="heartRate"
              color="#ef4444"
              refLines={HR_REFS}
              yDomain={["auto", "auto"]}
              maintenanceWindows={maintenanceWindows}
            />
          </div>

          <div className="chart-block">
            <div className="chart-block__label">Temperature (°C)</div>
            <VitalChart
              vitals={vitalsHistory}
              dataKey="temperature"
              color="#f97316"
              refLines={TEMP_REFS}
              yDomain={[34, 40]}
              maintenanceWindows={maintenanceWindows}
            />
          </div>

          <div className="chart-block">
            <div className="chart-block__label">SpO2 (%)</div>
            <VitalChart
              vitals={vitalsHistory}
              dataKey="spo2"
              color="#3b82f6"
              refLines={SPO2_REFS}
              yDomain={[85, 100]}
              maintenanceWindows={maintenanceWindows}
            />
          </div>
        </div>

        <div className="patient-detail__alerts">
          <h3 className="section-title">
            Alert Timeline
            {unresolvedCount > 0 && (
              <span className="badge badge--critical" style={{ marginLeft: "0.5rem" }}>
                {unresolvedCount} active
              </span>
            )}
          </h3>
          <PatientAlerts alerts={alerts} onAlertResolved={handleAlertResolved} />
        </div>
      </div>
    </section>
  );
}
