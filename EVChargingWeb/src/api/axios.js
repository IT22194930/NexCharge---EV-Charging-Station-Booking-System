import axios from "axios";

// Base URL configuration for different environments
const BASE_URLS = {
  local: "http://localhost:5274/api",
  network: "http://192.168.1.63/EVChargingAPI/api",
};

const currentBaseURL = BASE_URLS.network; // Change to BASE_URLS.local for local development

const api = axios.create({
  baseURL: currentBaseURL,
});

// Auto-add JWT token if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const localApi = axios.create({
  baseURL: BASE_URLS.local,
});

export const networkApi = axios.create({
  baseURL: BASE_URLS.network,
});

// Add token interceptor to both instances
[localApi, networkApi].forEach((instance) => {
  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });
});

export default api;
