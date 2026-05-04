import { httpClient } from "./httpClient";

export const alertApi = {
  getAll: () =>
    httpClient.get("/alerts").then((res) => res.data),

  getUnresolved: () =>
    httpClient.get("/alerts/unresolved").then((res) => res.data),

  getByPatientId: (patientId) =>
    httpClient.get(`/alerts/patient/${patientId}`).then((res) => res.data),

  resolve: (alertId) =>
    httpClient.put(`/alerts/${alertId}/resolve`).then((res) => res.data),
};