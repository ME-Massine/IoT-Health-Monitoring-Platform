import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
  ReferenceArea,
  ResponsiveContainer,
} from "recharts";

function formatTime(dateStr) {
  return new Date(dateStr).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function isInMaintenance(recordedAt, maintenanceWindows) {
  const t = new Date(recordedAt).getTime();
  return maintenanceWindows.some((w) => {
    const start = new Date(w.startedAt).getTime();
    const end = w.endedAt ? new Date(w.endedAt).getTime() : Date.now();
    return t >= start && t <= end;
  });
}

function buildMaintenanceAreas(data) {
  const areas = [];
  let areaStart = null;

  data.forEach((d, i) => {
    if (d.inMaintenance && areaStart === null) {
      areaStart = d.time;
    }
    if (!d.inMaintenance && areaStart !== null) {
      areas.push({ x1: areaStart, x2: data[i - 1].time });
      areaStart = null;
    }
  });

  if (areaStart !== null) {
    areas.push({ x1: areaStart, x2: data[data.length - 1].time });
  }

  return areas;
}

export function VitalChart({ vitals, dataKey, color, refLines = [], yDomain, maintenanceWindows = [] }) {
  const data = [...vitals].reverse().map((v) => {
    const inMaintenance = maintenanceWindows.length > 0 && isInMaintenance(v.recordedAt, maintenanceWindows);
    return {
      time: formatTime(v.recordedAt),
      value: inMaintenance
        ? null
        : dataKey === "temperature"
        ? parseFloat(v[dataKey])
        : v[dataKey],
      inMaintenance,
    };
  });

  const areas = buildMaintenanceAreas(data);

  return (
    <ResponsiveContainer width="100%" height={150}>
      <LineChart data={data} margin={{ top: 4, right: 16, left: -16, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
        <XAxis
          dataKey="time"
          tick={{ fontSize: 10, fill: "#94a3b8" }}
          interval="preserveStartEnd"
          axisLine={false}
          tickLine={false}
        />
        <YAxis
          tick={{ fontSize: 10, fill: "#94a3b8" }}
          domain={yDomain}
          axisLine={false}
          tickLine={false}
        />
        <Tooltip
          contentStyle={{ fontSize: 12, padding: "4px 10px", borderRadius: 6, border: "1px solid #e2e8f0" }}
          formatter={(v) => v == null ? ["Maintenance", ""] : [v, ""]}
          labelStyle={{ color: "#64748b" }}
        />

        {areas.map((a, i) => (
          <ReferenceArea
            key={i}
            x1={a.x1}
            x2={a.x2}
            fill="#94a3b8"
            fillOpacity={0.15}
            stroke="#94a3b8"
            strokeOpacity={0.3}
            strokeDasharray="4 2"
            label={{ value: "Maintenance", position: "insideTop", fontSize: 9, fill: "#94a3b8" }}
          />
        ))}

        {refLines.map((ref) => (
          <ReferenceLine
            key={`${ref.value}-${ref.label}`}
            y={ref.value}
            stroke={ref.color}
            strokeDasharray="4 2"
            strokeOpacity={0.7}
          />
        ))}
        <Line
          type="monotone"
          dataKey="value"
          stroke={color}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, strokeWidth: 0 }}
          connectNulls={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
