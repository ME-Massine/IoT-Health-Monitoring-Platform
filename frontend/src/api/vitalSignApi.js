import { httpClient } from "./httpClient";

export const vitalSignApi = {
  getLatestByPatientId: (patientId) =>
    httpClient
      .get(`/vitals/patient/${patientId}/latest`)
      .then((res) => res.data),

  getHistoryByPatientId: (patientId, { limit = 20, from, to } = {}) =>
    httpClient
      .get(`/vitals/patient/${patientId}/history`, {
        params: {
          limit,
          ...(from && { from: from instanceof Date ? from.toISOString() : from }),
          ...(to   && { to:   to   instanceof Date ? to.toISOString()   : to   }),
        },
      })
      .then((res) => res.data),
};