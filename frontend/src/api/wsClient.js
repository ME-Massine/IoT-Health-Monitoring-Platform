import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const WS_URL =
  import.meta.env.VITE_WS_URL || "http://localhost:8080/ws";

export function createStompClient() {
  return new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 5000,
  });
}