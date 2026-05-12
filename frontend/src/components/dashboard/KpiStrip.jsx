import { useState, useEffect } from "react";
import { Users, AlertCircle, AlertTriangle, Wifi } from "lucide-react";
import {
  AreaChart, Area, Tooltip, ResponsiveContainer,
} from "recharts";
import { alertApi } from "../../api/alertApi";

function AlertSparkline() {
  const [data, setData] = useState([]);

  useEffect(() => {
    const to = new Date();
    const from = new Date(Date.now() - 24 * 3600_000);
    alertApi.getSummary(from, to).then((pts) => {
      setData(pts.map((p) => ({
        hour: new Date(p.hour).getHours() + ":00",
        critical: p.critical,
        warning: p.warning,
      })));
    }).catch(() => {});
  }, []);

  if (data.length === 0) return null;

  return (
    <div className="kpi-sparkline">
      <ResponsiveContainer width="100%" height={36}>
        <AreaChart data={data} margin={{ top: 2, right: 2, left: 2, bottom: 2 }}>
          <defs>
            <linearGradient id="sparkCritical" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#dc2626" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#dc2626" stopOpacity={0}   />
            </linearGradient>
            <linearGradient id="sparkWarning" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#f59e0b" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#f59e0b" stopOpacity={0}   />
            </linearGradient>
          </defs>
          <Tooltip
            contentStyle={{ fontSize: 11, padding: "2px 8px", borderRadius: 5, border: "1px solid #e2e8f0" }}
            formatter={(v, name) => [v, name === "critical" ? "Critical" : "Warning"]}
            labelFormatter={(l) => `${l}`}
          />
          <Area type="monotone" dataKey="warning"  stroke="#f59e0b" fill="url(#sparkWarning)"  strokeWidth={1.5} dot={false} />
          <Area type="monotone" dataKey="critical" stroke="#dc2626" fill="url(#sparkCritical)" strokeWidth={1.5} dot={false} />
        </AreaChart>
      </ResponsiveContainer>
      <span className="kpi-sparkline__label">24 h alert trend</span>
    </div>
  );
}

export function KpiStrip({ patientCount, criticalCount, warningCount, devicesOnline }) {
  return (
    <div className="kpi-strip">
      <div className="kpi-card">
        <div className="kpi-card__icon kpi-card__icon--blue">
          <Users size={18} />
        </div>
        <div className="kpi-card__body">
          <div className="kpi-card__value">{patientCount}</div>
          <div className="kpi-card__label">Monitored Patients</div>
        </div>
      </div>

      <div className="kpi-card kpi-card--critical">
        <div className="kpi-card__icon kpi-card__icon--red">
          <AlertCircle size={18} />
        </div>
        <div className="kpi-card__body">
          <div className="kpi-card__value">{criticalCount}</div>
          <div className="kpi-card__label">Critical Alerts</div>
        </div>
      </div>

      <div className="kpi-card kpi-card--warning">
        <div className="kpi-card__icon kpi-card__icon--orange">
          <AlertTriangle size={18} />
        </div>
        <div className="kpi-card__body">
          <div className="kpi-card__value">{warningCount}</div>
          <div className="kpi-card__label">Active Warnings</div>
        </div>
      </div>

      <div className="kpi-card">
        <div className="kpi-card__icon kpi-card__icon--green">
          <Wifi size={18} />
        </div>
        <div className="kpi-card__body">
          <div className="kpi-card__value">{devicesOnline}</div>
          <div className="kpi-card__label">Devices Online</div>
        </div>
      </div>

      <AlertSparkline />
    </div>
  );
}
