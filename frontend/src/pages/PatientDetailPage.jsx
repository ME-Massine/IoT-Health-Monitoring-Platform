import { useParams } from "react-router-dom";

export function PatientDetailPage() {
  const { patientId } = useParams();

  return (
    <section>
      <h2>Patient Details</h2>
      <p>Patient ID: {patientId}</p>
    </section>
  );
}