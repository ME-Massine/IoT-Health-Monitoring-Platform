import { Link, Outlet } from "react-router-dom";

export function AppLayout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h1 className="logo">IoT Health</h1>

        <nav className="nav">
          <Link to="/">Dashboard</Link>
          <Link to="/alerts">Alerts</Link>
        </nav>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}