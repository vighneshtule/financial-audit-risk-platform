/**
 * Sprint 2B.1 – Authentication Foundation
 *
 * AuthContext + AuthProvider
 *
 * Architecture (no circular deps):
 *
 *   AuthProvider
 *       ↓  setToken()
 *   tokenStore           ← plain TS module, no React
 *       ↓  getToken()
 *   apiClient (Axios)
 *       ↓  request interceptor → Authorization: Bearer <token>
 *       ↓  response interceptor → 401 → notifyUnauthorized()
 *   tokenStore.notifyUnauthorized()
 *       ↓  registered handler
 *   AuthProvider.clearAuth()  ← state update, no direct Axios import needed
 */

import React, {
  createContext,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
import { authApi } from '../api/auth'
import { setToken, registerUnauthorizedHandler } from '../api/tokenStore'
import type { AuthContextValue, AuthUser, PersistedAuthState } from '../types/auth'

// ── localStorage key ─────────────────────────────────────────────────────────
const AUREX_AUTH_KEY = 'aurex_auth'

// ── Helpers ──────────────────────────────────────────────────────────────────
function readPersistedAuth(): PersistedAuthState | null {
  try {
    const raw = localStorage.getItem(AUREX_AUTH_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    if (
      parsed !== null &&
      typeof parsed === 'object' &&
      'token' in parsed &&
      'user' in parsed &&
      typeof (parsed as Record<string, unknown>).token === 'string'
    ) {
      return parsed as PersistedAuthState
    }
    return null
  } catch {
    return null
  }
}

function persistAuth(state: PersistedAuthState): void {
  localStorage.setItem(AUREX_AUTH_KEY, JSON.stringify(state))
}

function clearPersistedAuth(): void {
  localStorage.removeItem(AUREX_AUTH_KEY)
}

// ── Context ───────────────────────────────────────────────────────────────────
export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// ── Provider ──────────────────────────────────────────────────────────────────
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [token, setTokenState] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  /**
   * Synchronise the module-level tokenStore whenever our token state changes.
   * Must be kept in sync so the Axios interceptor always has the latest value.
   */
  const syncToken = useCallback((newToken: string | null) => {
    setTokenState(newToken)
    setToken(newToken)
  }, [])

  // ── clearAuth: called internally and by the 401 handler ──────────────────
  // Use a ref so the registered 401 callback always closes over the latest
  // version of clearAuth without needing to re-register on every render.
  const clearAuthRef = useRef<() => void>(() => {
    /* placeholder – replaced immediately below */
  })

  const clearAuth = useCallback(() => {
    setUser(null)
    syncToken(null)
    clearPersistedAuth()
  }, [syncToken])

  // Keep the ref current so the 401 callback stays up to date.
  useEffect(() => {
    clearAuthRef.current = clearAuth
  }, [clearAuth])

  // ── Session restoration on mount ─────────────────────────────────────────
  useEffect(() => {
    // Register the 401 handler once. Reads from ref so it's always current.
    registerUnauthorizedHandler(() => {
      clearAuthRef.current()
    })

    let cancelled = false

    async function restoreSession() {
      const persisted = readPersistedAuth()

      if (!persisted) {
        // Nothing stored → stay unauthenticated, finish loading.
        setIsLoading(false)
        return
      }

      // Temporarily inject the persisted token so /me gets the header.
      setToken(persisted.token)

      try {
        const verifiedUser = await authApi.me()
        if (!cancelled) {
          setUser(verifiedUser)
          syncToken(persisted.token)
          // Refresh persisted state with the validated user from /me.
          persistAuth({ token: persisted.token, user: verifiedUser })
        }
      } catch {
        // Token is invalid / expired → clear everything.
        if (!cancelled) {
          setToken(null) // clear tokenStore even if state hasn't set it
          clearPersistedAuth()
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    restoreSession()

    return () => {
      cancelled = true
    }
  }, [syncToken])

  // ── login() ───────────────────────────────────────────────────────────────
  const login = useCallback(
    async (username: string, password: string): Promise<void> => {
      const data = await authApi.login(username, password)
      const { token: newToken, user: newUser } = data

      syncToken(newToken)
      setUser(newUser)
      persistAuth({ token: newToken, user: newUser })
    },
    [syncToken]
  )

  // ── logout() ──────────────────────────────────────────────────────────────
  const logout = useCallback(() => {
    clearAuth()
  }, [clearAuth])

  const isAuthenticated = user !== null && token !== null

  const value: AuthContextValue = {
    user,
    token,
    isAuthenticated,
    isLoading,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
