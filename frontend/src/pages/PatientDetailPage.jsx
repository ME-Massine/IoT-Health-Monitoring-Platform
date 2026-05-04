import { useParams, Link } from "react-router-dom";
import { usePatientDetail } from "../hooks/usePatientDetail";
import { VitalsHistory } from "../components/detail/VitalsHistory";
import { PatientAlerts } from "../components/detail/PatientAlerts";

export function PatientDetailPage() {
  const { patientId } = useParams();
  const { patient, vitalsHistory, alerts, loading, error, handleAlertResolved } =
    usePatientDetail(patientId);

  if (loading) {
    return (
      <section>
        <p>Loading patient…</p>
      </section>
    );
  }

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
  const unresolvedCount = alerts.filter((a) => !a.resolved).length;

  return (
    <section className="patient-detail">
      <div className="patient-detail__back">
        <Link to="/">← Back to dashboard</Link>
      </div>

      <div className="patient-detail__header">
        <h2>{fullName}</h2>
        <span className="patient-detail__room">Room {patient.roomNumber}</span>
      </div>

      <div className="patient-detail__info">
        <span>{patient.age} yrs</span>
        <span>{patient.gender}</span>
        {patient.medicalCondition && <span>{patient.medicalCondition}</span>}
      </div>

      <div className="patient-detail__sections">
        <div className="patient-detail__section">
          <h3>Vitals History <span className="section-count">(last 20)</span></h3>
          <VitalsHistory vitals={vitalsHistory} />
        </div>

        <div className="patient-detail__section">
          <h3>
            Alerts{" "}
            {unresolvedCount > 0 && (
              <span className="badge badge--warning">{unresolvedCount} unresolved</span>
            )}
          </h3>
          <PatientAlerts alerts={alerts} onAlertResolved={handleAlertResolved} />
        </div>
      </div>
    </section>
  );
}