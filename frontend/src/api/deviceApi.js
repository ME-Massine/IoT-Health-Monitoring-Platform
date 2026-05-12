import { httpClient } from "./httpClient";

export const deviceApi = {
  getAll: () => httpClient.get("/devices").then((res) => res.data),
  getByPatientId: (patientId) => httpClient.get(`/devices/patient/${patientId}`).then((res) => res.data),
  setStatus: (id, status) => httpClient.patch(`/devices/${id}/status`, { status }).then((res) => res.data),
  getMaintenanceWindows: (id) => httpClient.get(`/devices/${id}/maintenance-windows`).then((res) => res.data),
};
