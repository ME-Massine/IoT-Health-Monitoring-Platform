import { httpClient } from "./httpClient";

export const vitalSignApi = {
  getLatestByPatientId: (patientId) =>
    httpClient
      .get(`/vitals/patient/${patientId}/latest`)
      .then((res) => res.data),

  getHistoryByPatientId: (patientId, limit = 20) =>
    httpClient
      .get(`/vitals/patient/${patientId}/history`, { params: { limit } })
      .then((res) => res.data),
};