import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Heart, Thermometer, Activity, Wrench, Clock, ChevronRight } from "lucide-react";
import {
  getPatientStatus,
  getHeartRateStatus,
  getTemperatureStatus,
  getSpo2Status,
} from "../../utils/vitalStatus";
import { formatTimeAgo } from "../../utils/time";

const STATUS_LABEL = {
  critical: "CRITICAL",
  warning: "WARNING",
  stable: "STABLE",
  unknown: "NO DATA",
};

const STALE_AFTER_MS = 2 * 60 * 1000;

export function PatientCard({ patient, vitals, deviceStatus }) {
  const navigate = useNavigate();
  const [now, setNow] = useState(Date.now());
  const [pulse, setPulse] = useState(false);
  const prevRecordedAt = useRef(vitals?.recordedAt);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 30_000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (
      vitals?.recordedAt &&
      prevRecordedAt.current &&
      vitals.recordedAt !== prevRecordedAt.current
    ) {
      setPulse(true);
      const t = setTimeout(() => setPulse(false), 700);
      prevRecordedAt.current = vitals.recordedAt;
      return () => clearTimeout(t);
    }
    prevRecordedAt.current = vitals?.recordedAt;
  }, [vitals?.recordedAt]);

  const fullName = `${patient.firstName} ${patient.lastName}`;
  const isOffline = deviceStatus && deviceStatus !== "ACTIVE";
  const isStale =
    !isOffline &&
    vitals?.recordedAt &&
    now - new Date(vitals.recordedAt).getTime() > STALE_AFTER_MS;
  const status = isOffline ? "unknown" : getPatientStatus(vitals);
  const hrStatus   = vitals ? getHeartRateStatus(vitals.heartRate)     : "unknown";
  const tempStatus = vitals ? getTemperatureStatus(vitals.temperature) : "unknown";
  const spo2Status = vitals ? getSpo2Status(vitals.spo2)               : "unknown";

  function handleClick() {
    navigate(`/patients/${patient.id}`);
  }

  function handleKeyDown(e) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      handleClick();
    }
  }

  return (
    <div
      className={`patient-card patient-card--${status} patient-card--clickable ${pulse ? "patient-card--pulse" : ""}`}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role="button"
      tabIndex={0}
      aria-label={`Open details for ${fullName}`}
    >
      <div className="patient-card__header">
        <span className="patient-card__name">{fullName}</span>
        {isOffline ? (
          <span className="status-badge status-badge--offline">
            <Wrench size={11} />
            {deviceStatus === "MAINTENANCE" ? "MAINTENANCE" : "OFFLINE"}
          </span>
        ) : (
          <span className={`status-badge status-badge--${status}`}>
            {STATUS_LABEL[status]}
          </span>
        )}
      </div>

      <div className="patient-card__room">Room {patient.roomNumber}</div>

      <div className="patient-card__meta">
        <span>{patient.age} yrs · {patient.gender}</span>
        {patient.medicalCondition && (
          <span className="patient-card__condition">{patient.medicalCondition}</span>
        )}
      </div>

      <div className="patient-card__vitals">
        {isOffline ? (
          <span className="patient-card__no-vitals device-offline-chip">
            <Wrench size={11} /> Device offline — no live data
          </span>
        ) : vitals ? (
          <>
            <div className={`vital-chip vital-chip--${hrStatus}`}>
              <Heart size={11} />
              <span>{vitals.heartRate ?? "—"} bpm</span>
            </div>
            <div className={`vital-chip vital-chip--${tempStatus}`}>
              <Thermometer size={11} />
              <span>{vitals.temperature ?? "—"} °C</span>
            </div>
            <div className={`vital-chip vital-chip--${spo2Status}`}>
              <Activity size={11} />
              <span>{vitals.spo2 ?? "—"}%</span>
            </div>
            {isStale && (
              <div className="stale-chip">
                <Clock size={10} />
                <span>{formatTimeAgo(vitals.recordedAt)}</span>
              </div>
            )}
          </>
        ) : (
          <span className="patient-card__no-vitals">No vitals recorded</span>
        )}
      </div>

      <div className="patient-card__footer">
        <ChevronRight size={14} className="patient-card__chevron" />
      </div>
    </div>
  );
}
