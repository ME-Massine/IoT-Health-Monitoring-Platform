import { useState, useEffect } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Activity, LayoutDashboard, Bell } from "lucide-react";
import { alertApi } from "../../api/alertApi";

export function AppLayout() {
  const [unresolvedCount, setUnresolvedCount] = useState(0);
  const [lastUpdate, setLastUpdate] = useState(new Date());

  useEffect(() => {
    function fetchCount() {
      alertApi
        .getUnresolved()
        .then((alerts) => {
          setUnresolvedCount(alerts.length);
          setLastUpdate(new Date());
        })
        .catch(() => {});
    }

    fetchCount();
    const interval = setInterval(fetchCount, 30000);
    return () => clearInterval(interval);
  }, []);

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
        </nav>

        <div className="sidebar__footer">
          <div className="system-status">
            <span className="system-status__dot" />
            System Online
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
