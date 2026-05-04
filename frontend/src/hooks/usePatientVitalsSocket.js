import { useEffect } from "react";
import { createStompClient } from "../api/wsClient";

export function usePatientVitalsSocket(patientId, onVital) {
  useEffect(() => {
    if (!patientId) return;

    const client = createStompClient();

    client.onConnect = () => {
      client.subscribe(`/topic/patients/${patientId}/vitals`, (message) => {
        try {
          const vital = JSON.parse(message.body);
          onVital(vital);
        } catch {
          console.warn("Failed to parse vitals WS message");
        }
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [patientId]); // eslint-disable-line react-hooks/exhaustive-deps
}