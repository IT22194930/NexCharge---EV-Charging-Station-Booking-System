import axios from "axios";

// Base URL configuration from environment variables
const BASE_URLS = {
  local: import.meta.env.VITE_API_BASE_URL_LOCAL,
  network: import.meta.env.VITE_API_BASE_URL_NETWORK,
};

// Determine current base URL from environment variable
const currentEnvironment = import.meta.env.VITE_API_BASE_URL_CURRENT;
const currentBaseURL = BASE_URLS[currentEnvironment];

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
