/**
 * Sprint 2B.1 – Authentication Foundation
 *
 * tokenStore: a minimal module-level token getter/setter.
 *
 * Purpose: Break the circular dependency chain:
 *   AuthContext → apiClient → AuthContext
 *
 * Instead:
 *   AuthProvider calls setToken() when auth state changes.
 *   apiClient reads getToken() in its request interceptor.
 *   apiClient calls onUnauthorized() for 401 responses.
 *
 * No React imports – this is plain TypeScript.
 */

let _token: string | null = null
let _onUnauthorized: (() => void) | null = null
let _isHandlingUnauthorized = false

/** Called by the apiClient request interceptor to retrieve the current token. */
export function getToken(): string | null {
  return _token
}

/** Called by AuthProvider to update the token whenever auth state changes. */
export function setToken(token: string | null): void {
  _token = token
  if (token !== null) {
    _isHandlingUnauthorized = false
  }
}

/**
 * Register a callback that the apiClient will invoke on a 401 response.
 * AuthProvider registers this on mount so it can clear auth state centrally.
 */
export function registerUnauthorizedHandler(handler: () => void): void {
  _onUnauthorized = handler
}

/**
 * Called by the apiClient response interceptor when a 401 is received.
 * Deduplicates concurrent 401 responses: if multiple requests fail with 401
 * simultaneously, only the first one triggers the unauthorized handler.
 */
export function notifyUnauthorized(): void {
  if (_isHandlingUnauthorized) {
    return
  }
  _isHandlingUnauthorized = true
  _onUnauthorized?.()
}

/**
 * Resets the unauthorized lock flag (e.g. on manual logout or after handling).
 */
export function resetUnauthorized(): void {
  _isHandlingUnauthorized = false
}
