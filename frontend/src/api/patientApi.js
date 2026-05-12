import { httpClient } from "./httpClient";

export const patientApi = {
  getAll: () => httpClient.get("/patients").then((res) => res.data),
  getById: (id) => httpClient.get(`/patients/${id}`).then((res) => res.data),
  create: (data) => httpClient.post("/patients", data).then((res) => res.data),
  update: (id, data) => httpClient.put(`/patients/${id}`, data).then((res) => res.data),
  delete: (id) => httpClient.delete(`/patients/${id}`),
};