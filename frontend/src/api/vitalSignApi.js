import { httpClient } from "./httpClient";

export const vitalSignApi = {
  getLatestByPatientId: (patientId) =>
    httpClient
      .get(`/vitals/patient/${patientId}/latest`)
      .then((res) => res.data),
};