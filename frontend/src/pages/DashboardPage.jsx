import { usePatients } from "../hooks/usePatients";
import { PatientCard } from "../components/dashboard/PatientCard";

export function DashboardPage() {
  const { patients, vitals, loading, error } = usePatients();

  if (loading) {
    return (
      <section>
        <h2>Patient Dashboard</h2>
        <p>Loading patients…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section>
        <h2>Patient Dashboard</h2>
        <p className="error">{error}</p>
      </section>
    );
  }

  if (patients.length === 0) {
    return (
      <section>
        <h2>Patient Dashboard</h2>
        <p>No patients found.</p>
      </section>
    );
  }

  return (
    <section>
      <h2>Patient Dashboard</h2>
      <p>{patients.length} patient{patients.length !== 1 ? "s" : ""} monitored</p>
      <div className="patient-grid">
        {patients.map((patient) => (
          <PatientCard
            key={patient.id}
            patient={patient}
            vitals={vitals[patient.id]}
          />
        ))}
      </div>
    </section>
  );
}