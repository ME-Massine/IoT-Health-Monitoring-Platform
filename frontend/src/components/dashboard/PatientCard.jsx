import { Link } from "react-router-dom";

export function PatientCard({ patient, vitals }) {
  const fullName = `${patient.firstName} ${patient.lastName}`;

  return (
    <div className="patient-card">
      <div className="patient-card__header">
        <span className="patient-card__name">{fullName}</span>
        <span className="patient-card__room">Room {patient.roomNumber}</span>
      </div>

      <div className="patient-card__meta">
        <span>{patient.age} yrs</span>
        <span>{patient.gender}</span>
        {patient.medicalCondition && (
          <span className="patient-card__condition">{patient.medicalCondition}</span>
        )}
      </div>

      <div className="patient-card__vitals">
        {vitals ? (
          <>
            <span>❤️ {vitals.heartRate ?? "—"} bpm</span>
            <span>🌡️ {vitals.temperature ?? "—"} °C</span>
            <span>🩸 {vitals.spo2 ?? "—"} %</span>
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