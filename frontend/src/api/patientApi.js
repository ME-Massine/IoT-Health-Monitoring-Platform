import { httpClient } from "./httpClient";

export const patientApi = {
  getAll: () => httpClient.get("/patients").then((res) => res.data),
  getById: (id) => httpClient.get(`/patients/${id}`).then((res) => res.data),
};