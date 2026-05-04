import { httpClient } from "./httpClient";

export const alertApi = {
  getByPatientId: (patientId) =>
    httpClient.get(`/alerts/patient/${patientId}`).then((res) => res.data),
  resolve: (alertId) =>
    httpClient.put(`/alerts/${alertId}/resolve`).then((res) => res.data),
};