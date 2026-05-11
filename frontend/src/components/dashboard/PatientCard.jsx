import { Link } from "react-router-dom";
import { Heart, Thermometer, Activity } from "lucide-react";
import {
  getPatientStatus,
  getHeartRateStatus,
  getTemperatureStatus,
  getSpo2Status,
} from "../../utils/vitalStatus";

const STATUS_LABEL = {
  critical: "CRITICAL",
  warning: "WARNING",
  stable: "STABLE",
  unknown: "NO DATA",
};

export function PatientCard({ patient, vitals }) {
  const fullName = `${patient.firstName} ${patient.lastName}`;
  const status = getPatientStatus(vitals);
  const hrStatus = vitals ? getHeartRateStatus(vitals.heartRate) : "unknown";
  const tempStatus = vitals ? getTemperatureStatus(vitals.temperature) : "unknown";
  const spo2Status = vitals ? getSpo2Status(vitals.spo2) : "unknown";

  return (
    <div className={`patient-card patient-card--${status}`}>
      <div className="patient-card__header">
        <span className="patient-card__name">{fullName}</span>
        <span className={`status-badge status-badge--${status}`}>
          {STATUS_LABEL[status]}
        </span>
      </div>

      <div className="patient-card__room">Room {patient.roomNumber}</div>

      <div className="patient-card__meta">
        <span>{patient.age} yrs · {patient.gender}</span>
        {patient.medicalCondition && (
          <span className="patient-card__condition">{patient.medicalCondition}</span>
        )}
      </div>

      <div className="patient-card__vitals">
        {vitals ? (
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
          </>
        ) : (
          <span className="patient-card__no-vitals">No vitals recorded</span>
        )}
      </div>

      <Link to={`/patients/${patient.id}`} className="patient-card__link">
        View details →
      </Link>
    </div>
  );
}
