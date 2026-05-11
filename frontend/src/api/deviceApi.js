import { httpClient } from "./httpClient";

export const deviceApi = {
  getAll: () => httpClient.get("/devices").then((res) => res.data),
};
