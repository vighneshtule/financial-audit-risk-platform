import axios from 'axios'
import { getToken, notifyUnauthorized } from './tokenStore'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor ──────────────────────────────────────────────────────
// Automatically attach the Bearer token to every outgoing request.
// Reads from tokenStore (no React dependency → no circular imports).
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor ─────────────────────────────────────────────────────
// • 401 → notify AuthProvider to clear session (only 401; not 400/403/404/500).
// • All errors → standardize the error message shape.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Delegate to AuthProvider via the registered callback.
      // Routing/redirect is intentionally left to the React layer.
      notifyUnauthorized()
    }

    // Standardize error messaging across production API responses
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'An unexpected error occurred'
    return Promise.reject(new Error(message))
  }
)
