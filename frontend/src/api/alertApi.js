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

  acknowledge: (alertId) =>
    httpClient.patch(`/alerts/${alertId}/acknowledge`).then((res) => res.data),

  dismiss: (alertId) =>
    httpClient.delete(`/alerts/${alertId}`),

  getSummary: (from, to) =>
    httpClient
      .get("/alerts/summary", { params: { from: from.toISOString(), to: to.toISOString() } })
      .then((res) => res.data),
};