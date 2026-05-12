import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    // Don't surface 404s as toasts — callers handle missing resources themselves
    if (status && status !== 404) {
      const message =
        error.response?.data?.message ||
        (status >= 500 ? "Server error. Please try again." : "Request failed.");
      window.dispatchEvent(new CustomEvent("api-error", { detail: message }));
    }
    return Promise.reject(error);
  }
);