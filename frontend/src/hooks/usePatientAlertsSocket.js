import { useEffect } from "react";
import { createStompClient } from "../api/wsClient";

export function usePatientAlertsSocket(patientId, onAlert) {
  useEffect(() => {
    if (!patientId) return;

    const client = createStompClient();

    client.onConnect = () => {
      client.subscribe(`/topic/patients/${patientId}/alerts`, (message) => {
        try {
          const alert = JSON.parse(message.body);
          onAlert(alert);
        } catch {
          console.warn("Failed to parse alerts WS message");
        }
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [patientId]); // eslint-disable-line react-hooks/exhaustive-deps
}