import { useState, useEffect, useCallback } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Activity, LayoutDashboard, Bell, Cpu } from "lucide-react";
import { alertApi } from "../../api/alertApi";
import { useGlobalAlertsSocket } from "../../hooks/useGlobalAlertsSocket";

const WS_BASE = import.meta.env.VITE_WS_URL ?? "http://localhost:8080";
const HEALTH_URL = WS_BASE.replace(/\/ws$/, "") + "/actuator/health";

export function AppLayout() {
  const [unresolvedCount, setUnresolvedCount] = useState(0);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [systemStatus, setSystemStatus] = useState("UP");

  const refreshCount = useCallback(() => {
    alertApi
      .getUnresolved()
      .then((alerts) => {
        setUnresolvedCount(alerts.length);
        setLastUpdate(new Date());
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    refreshCount();
    const interval = setInterval(refreshCount, 30000);
    return () => clearInterval(interval);
  }, [refreshCount]);

  useEffect(() => {
    function onResolved() {
      setUnresolvedCount((prev) => Math.max(0, prev - 1));
      setLastUpdate(new Date());
    }
    window.addEventListener("alert-resolved", onResolved);
    return () => window.removeEventListener("alert-resolved", onResolved);
  }, []);

  useEffect(() => {
    function checkHealth() {
      fetch(HEALTH_URL)
        .then((r) => r.json())
        .then((data) => setSystemStatus(data.status ?? "UNKNOWN"))
        .catch(() => setSystemStatus("DOWN"));
    }
    checkHealth();
    const id = setInterval(checkHealth, 30_000);
    return () => clearInterval(id);
  }, []);

  // Increment badge immediately when a new alert arrives via WebSocket
  useGlobalAlertsSocket((incoming) => {
    if (!incoming.resolved) {
      setUnresolvedCount((prev) => prev + 1);
      setLastUpdate(new Date());
    }
  });

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <Activity size={20} className="brand-icon" />
          <span className="logo">IoT Health</span>
        </div>

        <nav className="nav">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
          >
            <LayoutDashboard size={15} />
            Dashboard
          </NavLink>
          <NavLink
            to="/alerts"
            className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
          >
            <Bell size={15} />
            Alerts
            {unresolvedCount > 0 && (
              <span className="nav-badge">{unresolvedCount}</span>
            )}
          </NavLink>
          <NavLink
            to="/devices"
            className={({ isActive }) => `nav-link ${isActive ? "nav-link--active" : ""}`}
          >
            <Cpu size={15} />
            Devices
          </NavLink>
        </nav>

        <div className="sidebar__footer">
          <div className={`system-status ${systemStatus !== "UP" ? "system-status--down" : ""}`}>
            <span className="system-status__dot" />
            {systemStatus === "UP" ? "System Online" : systemStatus === "DOWN" ? "System Offline" : "System Degraded"}
          </div>
          <div className="system-status__time">
            Updated {lastUpdate.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
          </div>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
