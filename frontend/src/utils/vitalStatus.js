export function getHeartRateStatus(hr) {
  if (hr == null) return 'unknown';
  if (hr < 50 || hr > 120) return 'critical';
  if (hr >= 110) return 'warning';
  return 'normal';
}

export function getTemperatureStatus(temp) {
  if (temp == null) return 'unknown';
  const t = parseFloat(temp);
  if (t < 35.0 || t > 38.0) return 'critical';
  if (t >= 37.8) return 'warning';
  return 'normal';
}

export function getSpo2Status(spo2) {
  if (spo2 == null) return 'unknown';
  if (spo2 <= 92) return 'critical';
  if (spo2 <= 94) return 'warning';
  return 'normal';
}

export function getPatientStatus(vitals) {
  if (!vitals) return 'unknown';
  const statuses = [
    getHeartRateStatus(vitals.heartRate),
    getTemperatureStatus(vitals.temperature),
    getSpo2Status(vitals.spo2),
  ];
  if (statuses.includes('critical')) return 'critical';
  if (statuses.includes('warning')) return 'warning';
  return 'stable';
}

// Lower = higher priority (for sorting)
export const STATUS_ORDER = { critical: 0, warning: 1, stable: 2, unknown: 3 };
