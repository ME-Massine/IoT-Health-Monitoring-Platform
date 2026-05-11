import { Users, AlertCircle, AlertTriangle, Wifi } from "lucide-react";

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
    </div>
  );
}
