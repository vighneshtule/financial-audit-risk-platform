/**
 * Sprint 2B.1 – Authentication Foundation
 * Strongly-typed auth domain types.
 */

export type UserRole = 'ADMIN' | 'AUDITOR' | 'VIEWER'

export interface AuthUser {
  id: number
  username: string
  role: UserRole
}

export interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  sessionExpired: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  clearSessionExpired: () => void
}

/** Shape stored in localStorage under AUREX_AUTH_STORAGE_KEY */
export interface PersistedAuthState {
  token: string
  user: AuthUser
}

/** POST /api/auth/login response */
export interface LoginResponse {
  token: string
  expiresIn: number
  user: AuthUser
}
