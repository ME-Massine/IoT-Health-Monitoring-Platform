import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
  ResponsiveContainer,
} from "recharts";

function formatTime(dateStr) {
  return new Date(dateStr).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

export function VitalChart({ vitals, dataKey, color, refLines = [], yDomain }) {
  const data = [...vitals].reverse().map((v) => ({
    time: formatTime(v.recordedAt),
    value: dataKey === "temperature" ? parseFloat(v[dataKey]) : v[dataKey],
  }));

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
          formatter={(v) => [v, ""]}
          labelStyle={{ color: "#64748b" }}
        />
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
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
