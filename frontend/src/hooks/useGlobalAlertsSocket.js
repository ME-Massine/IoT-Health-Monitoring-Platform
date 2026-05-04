import { useEffect } from "react";
import { createStompClient } from "../api/wsClient";

export function useGlobalAlertsSocket(onAlert) {
  useEffect(() => {
    const client = createStompClient();

    client.onConnect = () => {
      client.subscribe("/topic/alerts", (message) => {
        try {
          const alert = JSON.parse(message.body);
          onAlert(alert);
        } catch {
          console.warn("Failed to parse global alerts WS message");
        }
      });
    };

    client.activate();
    return () => client.deactivate();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}